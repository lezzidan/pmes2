
package integratedtoolkit.api.impl;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import gnu.trove.map.hash.*;

import org.apache.log4j.Logger;
import org.objectweb.fractal.util.Fractal;
import org.objectweb.fractal.adl.Factory;
import org.objectweb.fractal.adl.ADLException;
import org.objectweb.fractal.api.Component;
import org.objectweb.fractal.api.NoSuchInterfaceException;
import org.objectweb.fractal.api.control.IllegalLifeCycleException;
import org.objectweb.fractal.api.control.LifeCycleController;
import org.objectweb.fractal.api.control.BindingController;
import org.objectweb.fractal.api.factory.GenericFactory;
import org.objectweb.fractal.api.type.ComponentType;
import org.objectweb.fractal.api.type.InterfaceType;
import org.objectweb.fractal.api.type.TypeFactory;
import org.objectweb.proactive.api.PADeployment;
import org.objectweb.proactive.api.PAFuture;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.component.adl.FactoryFactory;
import org.objectweb.proactive.core.component.Constants;
import org.objectweb.proactive.core.component.ContentDescription;
import org.objectweb.proactive.core.component.ControllerDescription;
import org.objectweb.proactive.core.component.controller.PriorityController;
import org.objectweb.proactive.core.component.controller.PriorityController.RequestPriority;
import org.objectweb.proactive.core.descriptor.data.ProActiveDescriptor;
import org.objectweb.proactive.core.process.JVMProcess;
import org.objectweb.proactive.core.util.wrapper.StringWrapper;

import integratedtoolkit.ITConstants;
import integratedtoolkit.api.APIEvents;
import integratedtoolkit.api.ITExecution;
import integratedtoolkit.api.IntegratedToolkit;
import integratedtoolkit.control.ITLifeCycleController;
import integratedtoolkit.interfaces.ITError;
import integratedtoolkit.interfaces.ITEvents;
import integratedtoolkit.interfaces.ITFileAccess;
import integratedtoolkit.interfaces.ITFileAccess.AccessMode;
import integratedtoolkit.interfaces.ITFileInformation;
import integratedtoolkit.interfaces.ITFileTransfer;
import integratedtoolkit.interfaces.ITPreparation;
import integratedtoolkit.interfaces.ITTaskCreation;
import integratedtoolkit.interfaces.ITTaskSubscription;
import integratedtoolkit.loader.LoaderAPI;
import integratedtoolkit.log.Loggers;
import integratedtoolkit.types.Parameter;
import integratedtoolkit.types.Parameter.*;
import integratedtoolkit.types.file.FileAccessId;
import integratedtoolkit.types.file.FileAccessId.*;
import integratedtoolkit.types.file.Location;


public class IntegratedToolkitImpl implements IntegratedToolkit, ITExecution, APIEvents, LoaderAPI {

	// Exception constants definition
	private static final String UNKNOWN_HOST_ERR	= 	"Cannot determine the IP address of the local host";
	private static final String DEPLOYMENT_ERR    	= 	"Error loading the deployment descriptor for the IT";
	private static final String IT_CREATION_ERR   	= 	"Error creating the IT";
	private static final String IT_INTERFACES_ERR 	= 	"Error getting IT interfaces";
	private static final String EVENT_HANDLER_ERR	= 	"Error creating event handler component";
	private static final String IT_START_ERR 		= 	"Error starting the IT";
	private static final String IT_INIT_ERR 		= 	"Error initialising the IT";
	private static final String IT_STOP_ERR 		= 	"Error stopping the IT";
	private static final String IT_KILLED_ERR 		= 	"Error: the IT component was killed and cannot be restarted";
	private static final String KILL_ERR			= 	"Error: cannot kill deployed components";
	private static final String SYNCH_ERR			= 	"Error: event synchronization failed";
	private static final String FILE_NAME_ERR		= 	"Error parsing file name";
	private static final String WRONG_DIRECTION_ERR	= 	"Error: invalid parameter direction: ";
	
	// URI beginning
	private static final String FILE_URI			= 	"file:";
	
	// Main component of the Integrated Toolkit
	private Component itComp;
	// Auxiliar component for event handling
	private Component eventComp;
	
	// Deployment
	private ProActiveDescriptor deploymentDescriptor;
	
	// Controllers for main IT component
	private ITLifeCycleController itLC;
	private BindingController itBC;
	private PriorityController itPC;
	
	// Controllers for event handler component
	private LifeCycleController eventLC;
	
	// IT Server Interfaces
	private ITTaskCreation taskCreationItf;
	private ITTaskSubscription taskSubscriptionItf;
	private ITFileAccess fileAccessItf;
	private ITFileInformation fileInformationItf;
	private ITFileTransfer fileTransferItf;
	private ITPreparation preparationItf;
	
	// Application attributes
	private String appHost;
	private String appWorkingDir;
	
	// Semaphore to wait for events
	private Semaphore sem;
	
	// True if the IT is started an can accept tasks
	private static boolean acceptingTasks = false;
	
	// True if the IT components have already been killed
	private boolean compKilled = false;
	
	// Logger
	private static final Logger logger = Logger.getLogger(Loggers.API);
	private static final boolean debug = logger.isDebugEnabled();
	
	// SLA
	private static final boolean slaEnabled = System.getProperty(ITConstants.IT_SLA_ENABLED) != null
	  										  && System.getProperty(ITConstants.IT_SLA_ENABLED).equals("true")
	  										  ? true : false;
	
	
	public IntegratedToolkitImpl() {
		// Initialization of application attributes
		try {
			InetAddress localHost = InetAddress.getLocalHost();
			appHost = localHost.getCanonicalHostName();
		}
		catch (UnknownHostException e) {
			logger.fatal("Error: "+ UNKNOWN_HOST_ERR, e);
			System.exit(1);
		}
		appWorkingDir = System.getProperty("user.dir") + "/";
		
		// Synchronization
		this.sem = new Semaphore(0);
		ITEventHandler.init((APIEvents)this);

		logger.info("Deploying the Integrated Toolkit");
		deployIT();
	}
	

	private void deployIT() {
		// Component structure definition to load
		String adl = ITConstants.IT;
        
		// Location of the deployment descriptor
		String descriptor = System.getProperty(ITConstants.IT_DEPLOYMENT);
		
		THashMap<String,Object> context = null;
		
      	try {       	
        	// Deployment descriptor definition
        	context = new THashMap<String,Object>();
        	deploymentDescriptor = PADeployment.getProactiveDescriptor(descriptor);
            context.put("deployment-descriptor", deploymentDescriptor);
        }
      	catch (ProActiveException pae) {
            logger.fatal(DEPLOYMENT_ERR, pae);
            System.exit(1);
        }

      	// Set dinamic JVM options
      	StringBuilder sb = new StringBuilder();
	sb.append(" -Xms128m -Xmx256m");
      	sb.append(" -D" + ITConstants.IT_PROJ_FILE + "=" + System.getProperty(ITConstants.IT_PROJ_FILE));
      	sb.append(" -D" + ITConstants.IT_RES_FILE + "=" + System.getProperty(ITConstants.IT_RES_FILE));
	sb.append(" -D" + ITConstants.IT_HIST_FILE + "=" + System.getProperty(ITConstants.IT_HIST_FILE));
      	sb.append(" -D" + ITConstants.IT_APP_NAME + "=" + System.getProperty(ITConstants.IT_APP_NAME));
      	sb.append(" -D" + ITConstants.GAT_BROKER_ADAPTOR + "=" + System.getProperty(ITConstants.GAT_BROKER_ADAPTOR));
      	sb.append(" -D" + ITConstants.GAT_FILE_ADAPTOR + "=" + System.getProperty(ITConstants.GAT_FILE_ADAPTOR));
      	sb.append(" -D" + ITConstants.LOG4J + "=" + System.getProperty(ITConstants.LOG4J));
      	sb.append(" -D" + ITConstants.GAT_DEBUG + "=" + System.getProperty(ITConstants.GAT_DEBUG));
      	sb.append(" -D" + ITConstants.IT_LOCATIONS + "=" + System.getProperty(ITConstants.IT_LOCATIONS));     
 	sb.append(" -D" + ITConstants.IT_CONSTR_FILE + "=" + System.getProperty(ITConstants.IT_CONSTR_FILE));
      	sb.append(" -D" + ITConstants.IT_LANG + "=" + System.getProperty(ITConstants.IT_LANG));
	//sb.append(" -Xshare:off");
        
        if (slaEnabled) {
        	sb.append(" -D" + ITConstants.IT_SLA_ENABLED + "=" + System.getProperty(ITConstants.IT_SLA_ENABLED));
            sb.append(" -D" + ITConstants.IT_SLA_ID + "=" + System.getProperty(ITConstants.IT_SLA_ID));
        }
        
        JVMProcess jvmProcess = ((JVMProcess)deploymentDescriptor.getProActiveDescriptorInternal().getVirtualMachine(ITConstants.IT_JVM).getProcess()); 
        jvmProcess.setJvmOptions(sb.toString());

        /* For remote deployment: take care with log4j file, don't pass the local url to the remote process, the remote process will take its url from the descriptor
        JVMProcess jvmProcessLocal = ((JVMProcess)deploymentDescriptor.getVirtualMachine("jvmTJ").getProcess());
        JVMProcess jvmProcessRemote = (JVMProcess)(((org.objectweb.proactive.core.process.ssh.SSHProcess)deploymentDescriptor.getVirtualMachine("jvmF").getProcess()).getFinalProcess());
        jvmProcessLocal.setJvmOptions(dinamicJVMOptions);
        jvmProcessRemote.setJvmOptions(dinamicJVMOptions);
        deploymentDescriptor.activateMappings(); */

        try {
            // Factory creation
            Factory f = FactoryFactory.getFactory();

            // Integrated Toolkit component creation
            itComp = (Component) f.newComponent(adl, context);

            logger.debug("Integrated Toolkit component created");
        }
        catch (ADLException ae) {
            logger.fatal(IT_CREATION_ERR, ae);
            System.exit(1);
        }

        try {
            // Controller to start/stop IT component
            itLC = (ITLifeCycleController) itComp.getFcInterface(Constants.LIFECYCLE_CONTROLLER);

            // Controller to bind IT main component with event handler component
            itBC = ((BindingController)itComp.getFcInterface(Constants.BINDING_CONTROLLER));
       
            // Controller to increase the priority of non-functional requests
            itPC = (PriorityController)itComp.getFcInterface(Constants.REQUEST_PRIORITY_CONTROLLER);
            itPC.setPriority(Constants.LIFECYCLE_CONTROLLER, "stopFc", new Class[] {}, RequestPriority.NF2);

            // Initialize IT server interfaces
            taskCreationItf = ((ITTaskCreation) itComp.getFcInterface("TaskCreation"));
            taskSubscriptionItf = ((ITTaskSubscription) itComp.getFcInterface("TaskSubscription"));
            fileAccessItf = ((ITFileAccess) itComp.getFcInterface("FileAccess"));
            fileInformationItf = ((ITFileInformation) itComp.getFcInterface("FileInformation"));
            fileTransferItf = ((ITFileTransfer) itComp.getFcInterface("FileTransfer"));
            preparationItf = ((ITPreparation) itComp.getFcInterface("Preparation"));
        }
        catch (NoSuchInterfaceException nsie) {
            logger.fatal(IT_INTERFACES_ERR, nsie);
            killIT();
            System.exit(1);
        }

        try {
            // Creation of event handler component
			Component boot = org.objectweb.fractal.api.Fractal.getBootstrapComponent();
			TypeFactory typeFactory = Fractal.getTypeFactory(boot);
			ComponentType eventCompType = typeFactory.createFcType(
											new InterfaceType[] {
	                    				  	  typeFactory.createFcItfType("ITEvents", ITEvents.class.getName(),
	                    				  			  					  false, false, false),
	                    				  	  typeFactory.createFcItfType("ITError", ITError.class.getName(),
	  	                    				  		  					  false, false, false),
											}
										  );
				
			GenericFactory cf = Fractal.getGenericFactory(boot);
			eventComp = cf.newFcInstance(eventCompType,
                        new ControllerDescription("EventHandler", Constants.PRIMITIVE),
                        new ContentDescription(ITEventHandler.class.getName()));
			
			// Controller to start/stop event handler component
			eventLC = ((LifeCycleController)eventComp.getFcInterface(Constants.LIFECYCLE_CONTROLLER));
				
			// Bind the event and error client interfaces of IT component with the event handler component
			itBC.bindFc("ApplicationEvents", eventComp.getFcInterface("ITEvents"));
			itBC.bindFc("ErrorManagement", eventComp.getFcInterface("ITError"));
			
			logger.debug("Event handler component created and bound to IT");
        }
        catch (Exception e) {
        	logger.fatal(EVENT_HANDLER_ERR, e);
        	killIT();
			System.exit(1);
		}
	}
	
	
	// Integrated Toolkit user interface implementation
	
	public void startIT() {
		logger.info("Starting the Integrated Toolkit");
		
		// Check if the IT component was killed
		if (compKilled) {
			logger.fatal(IT_KILLED_ERR);
			System.exit(1);
		}
		
		try {
			// Start IT component
			itLC.startFc();
		}
		catch (IllegalLifeCycleException ilce) {
			logger.fatal(IT_START_ERR, ilce);
			killIT();
			System.exit(1);
		}
		
		try {
			// Start event handler component
			eventLC.startFc();
		}
		catch (IllegalLifeCycleException ilce) {
			try { itLC.stopFc(); }
			catch (IllegalLifeCycleException ignore) { /* Ignore */ }
			logger.fatal(IT_START_ERR, ilce);
			killIT();
			System.exit(1);
		}
		
		logger.info("Initializing components");
		
		// Invoke the initialization of all IT subcomponents
        StringWrapper initMessage = preparationItf.initialize();
        if (!initMessage.stringValue().equals(ITConstants.INIT_OK)) {
        	try { itLC.stopFc(); }
			catch (IllegalLifeCycleException ignore) { /* Ignore */ }
			logger.fatal(IT_INIT_ERR + ": " + initMessage);
			killIT();
			System.exit(1);
        }
        
        acceptingTasks = true;
        
        logger.info("Ready to process tasks");
	}
	
	
	public void stopIT(boolean terminate) {
		// Block until all tasks have finished
		taskCreationItf.noMoreTasks();
		waitForFunctionalEvent(EventType.ALL_TASKS_FINISHED);
		
        // Block until all result files are transferred back to the original host
        fileTransferItf.checkResultFilesTransferred();
        waitForFunctionalEvent(EventType.RESULT_FILES_TRANSFERRED);
		
		// Block until all intermediate files generated by IT are deleted
		fileTransferItf.deleteIntermediateFiles();
		waitForFunctionalEvent(EventType.INTERMEDIATE_FILES_DELETED);
		
		// Now the runtime can be stopped
		stopRuntime(terminate);
		
		// Stop the event handler component
		try {
			eventLC.stopFc();
		}
		catch (IllegalLifeCycleException ilce) {
			if (terminate)
				logger.error(IT_STOP_ERR, ilce);
			else {
				logger.fatal(IT_STOP_ERR, ilce);
				killIT();
				System.exit(1);
			}
		}
		
		// Destroy the runtime if it will be no longer used 
		if (terminate)
			killIT();
	}
	
	private void stopRuntime(boolean cleanup) {
		/* We could have this here, in case we want to remove the intermediate files
		 * also in case of error. We should remove it from stopIT
		 * fileTransferItf.deleteIntermediateFiles();
		 * waitForFunctionalEvent(EventType.INTERMEDIATE_FILES_DELETED);
		 */
		try {
			// Stop components
			itLC.stopFc(cleanup);
		}
		catch (IllegalLifeCycleException ilce) {
			logger.fatal(IT_STOP_ERR, ilce);
			killIT();
			System.exit(1);
		}
		
		acceptingTasks = false;
		
		logger.info("Integrated Toolkit stopped");
	}
	
	
	private void killIT() {
		try {
			deploymentDescriptor.killall(false);
		}
		catch (ProActiveException pae) {
			// Simply log the error
			logger.error(KILL_ERR);
		}
		
		compKilled = true;
		
		logger.info("Integrated Toolkit killed");
	}
	
	
	public String openFile(String fileName, OpenMode m) {
		logger.info("Opening file " + fileName + " in mode " + m);
		
		// Parse the file name and translate the access mode
		String name = null, path = null, host = null;
		try {
			String[] hostPathName = extractHostPathName(fileName);
			host = hostPathName[0];
			path = hostPathName[1];
			name = hostPathName[2];
		}
		catch (Exception e) {
			newErrorEvent(true);
			logger.fatal(FILE_NAME_ERR, e);
			System.exit(1);
		}
		
		logger.debug("Synchronizing with previous tasks");
		
		// Let all previous tasks be created
		@SuppressWarnings("unused")
		int i = taskCreationItf.synchronizeWithPreviousCreations();
		
		// If the file is local and no task has accessed it before, just work with its original source
		if (host.equals(appHost) && !fileAccessItf.alreadyAccessed(name, path, host))
			return path + name;
		
		AccessMode am = null;
		switch (m) {
			case READ:		am = AccessMode.R; 		break;
			case WRITE: 	am = AccessMode.W; 		break;
			case APPEND: 	am = AccessMode.RW;		break;
		}
		
		/* Tell the FM that the application wants to access a file.
		 * Call getFutureValue to get rid of the stub
		 */
		FileAccessId faId = (FileAccessId)PAFuture.getFutureValue(
								fileAccessItf.registerFileAccess(name,
						  						 	 	   	 	 path,
						  						 	 	   	 	 host,
						  						 	 	   	 	 am)
							);
		
		if (am != AccessMode.W) {
			// Block until the last writer task for the file has finished
			taskSubscriptionItf.subscribeToTaskEnd(faId.getFileId());
			waitForFunctionalEvent(EventType.LAST_WRITER_TASK_FINISHED);
		}
		
		/* Transfer the file to the application host (the IT will know if this is neccesary
		 * or not, depending on the access mode).
		 */
		fileTransferItf.transferFileForOpen(faId, new Location(appHost, appWorkingDir));
		waitForFunctionalEvent(EventType.FILE_FOR_OPEN_TRANSFERRED);
		
		// Obtain the renaming for the file
		String rename = null;
		switch (am) {
			case R:
				RAccessId ra = (RAccessId)faId; 
				rename = fileInformationItf.getName(ra.getReadFileInstance());
				break;
			case W:
				WAccessId wa = (WAccessId)faId; 
				rename = fileInformationItf.getName(wa.getWrittenFileInstance());
				break;
			case RW:
				RWAccessId rwa = (RWAccessId)faId; 
				/* Get the renaming for the written version
				 * The file has already been transferred with this renaming 
				 */
				rename = fileInformationItf.getName(rwa.getWrittenFileInstance());
				break;
		}
		
		/* Return the path that the application must use to access the (renamed) file
		 * The file won't recover its original name until IT_Off is called
		 */
		return appWorkingDir + rename;
	}
	
	
	
	// IT_Execution interface implementation
	
	public void executeTask(String methodClass,
							String methodName,
							int parameterCount,
							Object... parameters) {
		
		if (debug) {
			logger.debug("Creating task from method " + methodName + " in " + methodClass);
			logger.debug("There " + (parameterCount > 1 ? "are " : "is ") + parameterCount + " parameter" + (parameterCount > 1 ? "s" : ""));
		}
	
		Parameter[] pars = new Parameter[parameterCount];
		
		// Parameter parsing needed, object is not serializable
		for (int npar = 0; npar < parameterCount; npar++) {
			int i = npar * 3;
			
			ParamType type = (ParamType)parameters[i+1];
			ParamDirection direction = (ParamDirection)parameters[i+2];
			
			if (debug) logger.debug("  Parameter " + (npar + 1) + " has type " + type.name());
			
			switch (type) {
				case FILE_T:
					String name = null, path = null, host = null;
					try {
						String[] hostPathName = extractHostPathName((String)parameters[i]);
						host = hostPathName[0];
						path = hostPathName[1];
						name = hostPathName[2];
					}
					catch (Exception e) {
						logger.fatal(FILE_NAME_ERR, e);
						newErrorEvent(true);
						System.exit(1);
					}
					
					pars[npar] = new FileParameter(type, direction, name, path, host);
					break;
					
				default:
					/* Basic types (including String).
					 * The only possible direction is IN, warn otherwise
					 */
					if (direction != ParamDirection.IN) {
						logger.warn(WRONG_DIRECTION_ERR
						   		    + "Method " + methodName
						   		    + ", parameter " + npar
						   		    + " has a basic type, therefore it must have INPUT direction");
					}
					pars[npar] = new BasicTypeParameter(type, ParamDirection.IN, parameters[i]);
					break;
			}
		}
		
		taskCreationItf.newTask(methodClass, methodName, pars);
	}
	
	
	public static boolean acceptingTasks() {
		return acceptingTasks;
	}
	
	
	
	// APIEvents interface implementation
	
	public void newFunctionalEvent(EventType eType) {
		logger.debug("Received event of type " + eType);
		
		sem.release();
	}
	
	
	public void waitForFunctionalEvent(EventType eType) {
		logger.debug("Waiting for event of type " + eType);
		
		try {
			sem.acquire();
		}
		catch (InterruptedException ie) {
			logger.fatal("Error: " + SYNCH_ERR, ie);
			killIT();
			System.exit(1);
		}
	}
	
	
	public void newErrorEvent(boolean mustClean) {
		logger.debug("Received error with mustClean: " + mustClean);
		
		stopRuntime(mustClean);
		
		killIT();
	}
	
	
	
	// LoaderAPI interface implementation
	
	public String getFile(String fileName, String destDir) {
		// Parse the file name
		String name = null, path = null, host = null;
		try {
			String[] hostPathName = extractHostPathName(fileName);
			host = hostPathName[0];
			path = hostPathName[1];
			name = hostPathName[2];
		}
		catch (Exception e) {
			logger.fatal(FILE_NAME_ERR, e);
			newErrorEvent(true);
			System.exit(1);
		}
		
		logger.debug("Synchronizing with previous tasks");
		
		// Let all previous tasks be created
		@SuppressWarnings("unused")
		int i = taskCreationItf.synchronizeWithPreviousCreations();
		
		// The file is local; if no task has accessed it before, just work with its original source
		if (!fileAccessItf.alreadyAccessed(name, path, host))
			return path + name;
		
		/* Tell the FM that the application wants to access a file.
		 * Call getFutureValue to get rid of the stub
		 */
		FileAccessId faId = (FileAccessId)PAFuture.getFutureValue(
								fileAccessItf.registerFileAccess(name,
						  						 	 	   	 	 path,
						  						 	 	   	 	 host,
						  						 	 	   	 	 AccessMode.R)
							);
		
		// Block until the last writer task for the file has finished
		taskSubscriptionItf.subscribeToTaskEnd(faId.getFileId());
		waitForFunctionalEvent(EventType.LAST_WRITER_TASK_FINISHED);
		
		// Transfer the file to the application host
		fileTransferItf.transferFileRaw(faId, new Location(appHost, destDir));
		waitForFunctionalEvent(EventType.RAW_FILE_TRANSFERRED);
		
		// Obtain the renaming for the file
		RAccessId ra = (RAccessId)faId; 
		String rename = fileInformationItf.getName(ra.getReadFileInstance());
		
		// Return the name of the file (a renaming) on which the stream will be opened
		return destDir + rename;
	}
	
	
	public void loaderError(boolean mustClean) {
		newErrorEvent(mustClean);
	}
	
	
	// Private method for file name parsing. TODO: Logical file names?
	
	private String[] extractHostPathName(String fullName) throws Exception {
		String name, path, host;
		
		if (fullName.startsWith(FILE_URI)) {
			/* URI syntax with host name and absolute path, e.g. "file://bscgrid01.bsc.es/home/etejedor/file.txt"
			 * Only used in grid-aware applications, using IT API and partial loader,
			 * since total loader targets sequential applications that use local files.
			 */
			URI u = new URI(fullName);
			host = u.getHost();
			String fullPath = u.getPath();
			int pathEnd = fullPath.lastIndexOf("/");
			path = fullPath.substring(0, pathEnd + 1);
			name = fullPath.substring(pathEnd + 1);
		}
		else {
			// Local file, format will depend on OS
			File f = new File(fullName);
			String canonicalPath = f.getCanonicalPath();
			name = f.getName();
			path = canonicalPath.substring(0, canonicalPath.length() - name.length());
			host = appHost;
		}
		
		return new String[] { host, path, name };
	}
	
}
