package integratedtoolkit.components.impl;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import gnu.trove.map.hash.*;

import integratedtoolkit.components.FileTransfer;
import integratedtoolkit.components.FileInfoUpdate;
import integratedtoolkit.components.JobCreation;
import integratedtoolkit.components.Preparation;
import integratedtoolkit.components.SchedFTMUpdate;

import integratedtoolkit.components.TransferStatus;
import integratedtoolkit.components.TransferStatus.*;
import integratedtoolkit.components.FileInformation;
import integratedtoolkit.components.AppFileEvents;
import integratedtoolkit.interfaces.ITError;
import integratedtoolkit.log.Loggers;

import integratedtoolkit.ITConstants;
import integratedtoolkit.types.file.*;
import integratedtoolkit.types.file.FileOperation.*;
import integratedtoolkit.types.file.FileAccessId.*;
import integratedtoolkit.util.ElementNotFoundException;
import integratedtoolkit.util.ProjectManager;
import integratedtoolkit.util.RequestDispatcher;
import integratedtoolkit.util.RequestQueue;
import integratedtoolkit.util.ThreadPool;
import integratedtoolkit.util.GroupManager;
import integratedtoolkit.util.ur.FileTransferUsageRecord;

import org.apache.log4j.Logger;
import org.gridlab.gat.GAT;
import org.gridlab.gat.GATContext;
import org.gridlab.gat.URI;
import org.gridlab.gat.io.File;
import org.gridlab.gat.io.LogicalFile;

import org.objectweb.fractal.api.control.BindingController;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.RunActive;
import org.objectweb.proactive.api.PAFuture;
import org.objectweb.proactive.core.util.wrapper.StringWrapper;
import org.objectweb.proactive.Service;


public class FileTransferManager implements FileTransfer, FileInfoUpdate, Preparation,
											BindingController,
											RunActive {
	
	// Constants definition
	private static final int 	POOL_SIZE 		 	 = 5;
	private static final String POOL_NAME 		   	 = "FTM";
	private static final String CLEAN_SCRIPT         = "clean.sh";
	private static final String ANY_PROT             = "any://";
	private static final String THREAD_POOL_ERR  	 = "Error starting pool of threads";
	private static final String REFLECTION_ERR   	 = "Error getting post processing method";
	private static final String DELETE_ERR   	 = "Error deleting intermediate files";
	private static final String LF_PREPARATION_ERR   = "Error preparing logical file to replicate";
	private static final String VERSION_CREATION_ERR = "Error creating logical file";
	private static final String TRANSFER_ERR         = "Error transferring file";
	private static final String TRANSFERS_LEFT_ERR 	 = "Error calculating the number of transfers left in a group";
	private static final String POST_PROCESS_ERR 	 = "Error post processing operation";
	private static final String PROJ_LOAD_ERR	 = "Error loading project information";
	private static final String POOL_ERR	         = "Error stopping threads of pool";
	
	// Client interfaces
	private FileInformation fileInformation;
	private TransferStatus transferStatus;
	private AppFileEvents appTransferEvents;
	private JobCreation newJob;
	private SchedFTMUpdate schedFTMUpdate;
	private ITError errorReport;
	
	/* Map : logical file name -> logical file
	 * It acts as a logical file repository, where logical files can be
	 * retrieved giving their logical name
	 */
	private Map<String,LogicalFile> nameToLogicalFile;

	// Actual transfer  time calculation
        private Map<String,Map<String,List<Long>>> hostToSimTrfFiles; //Host -> Simultaneous transfer files
	
	// Pool of worker threads and queue of requests
	private ThreadPool pool;
	private RequestQueue<FileOperation> queue;
	
	// Manager for groups of operations
	private GroupManager opGroups;
	
	// Post processing method for transfers related to R file accesses
	private Method readPostProcess;
	
	// GAT context
	private GATContext context;
	
	// GAT file adaptor
	String adaptor;
	private boolean userNeeded;
	
	// Object that stores the information about the current project
	private ProjectManager projManager;

         //Performance debug
         private boolean  timeMeasuring = false;
	 private double timedoCopy = 0;

	// Component logger - No need to configure, ProActive does
	private static final Logger logger = Logger.getLogger(Loggers.FTM_COMP);
	private static final boolean debug = logger.isDebugEnabled();
	
	// SLA
	private static final boolean slaEnabled = System.getProperty(ITConstants.IT_SLA_ENABLED) != null
	  										  && System.getProperty(ITConstants.IT_SLA_ENABLED).equals("true")
	  										  ? true : false;
	
	public FileTransferManager() { }
	
	
	// RunActive interface
	
	public void runActivity(Body body) {
		body.setImmediateService("terminate", new Class[] {});
		
		Service service = new Service(body);
		service.fifoServing();
	}
	
		
	// Server interfaces implementation
	
	// Preparation interface
	
	public StringWrapper initialize() {
		
		// GAT adaptor path
		System.setProperty(ITConstants.GAT_ADAPTOR,
						   System.getenv("GAT_LOCATION") + ITConstants.GAT_ADAPTOR_LOC);
		
		if (context == null) {
			context = new GATContext();
			adaptor = System.getProperty(ITConstants.GAT_FILE_ADAPTOR);
			/* We need to try the local adaptor when both source and target hosts
			 * are local, because ssh file adaptor cannot perform local operations
			 */
			context.addPreference("File.adaptor.name", adaptor + ", srcToLocalToDestCopy, local");
			userNeeded = adaptor.regionMatches(true, 0, "ssh", 0, 3);
		}
		
		if (nameToLogicalFile == null)
			nameToLogicalFile = new TreeMap<String,LogicalFile>();
		else
			nameToLogicalFile.clear();
		
		if (opGroups == null)
			opGroups = new GroupManager();
		else
			opGroups.clear();
		
		// Use reflection to get post processing method (DeclaredMethod since it is private)
		if (readPostProcess == null) {
			try {
				Class ftm = Class.forName(this.getClass().getName());
				readPostProcess = ftm.getDeclaredMethod("readPostProcess", Copy.class);
			}
			catch (Exception e) {
				logger.error(REFLECTION_ERR, e);
				return new StringWrapper(ITConstants.FTM + ": " + REFLECTION_ERR);
			}
		}
		
		// Create threads that will handle (blocking) file transfer requests
		if (queue == null)
			queue = new RequestQueue<FileOperation>();
		else
			queue.clear();
		
		if (pool == null) {
			pool = new ThreadPool(POOL_SIZE, POOL_NAME, new TransferDispatcher(queue, this));
			try {
				pool.startThreads();
			}
			catch (Exception e) {
				logger.error(THREAD_POOL_ERR, e);
				return new StringWrapper(ITConstants.FTM + ": " + THREAD_POOL_ERR);
			}
		}
		
		if (projManager == null)
			try {
				projManager = new ProjectManager();
			}
			catch (Exception e) {
				logger.error(PROJ_LOAD_ERR, e);
				return new StringWrapper(ITConstants.FTM + ": " + PROJ_LOAD_ERR);
			}

		if(hostToSimTrfFiles == null){
                          hostToSimTrfFiles = new THashMap<String,Map<String,List<Long>>>();
                        }
                        else{
                          hostToSimTrfFiles.clear();
                        }

		logger.info("Initialization finished");
		
		return new StringWrapper(ITConstants.INIT_OK);
	}
	
	
	public void cleanup() {
		// Make pool threads finish
		try {
			pool.stopThreads();
		}
		catch (Exception e) {
			logger.error(POOL_ERR, e);
		}

		GAT.end();
                
		if(timeMeasuring){
                  logger.info(" ");
                  logger.info("Debug Run Time of functions (in seconds) : ");
                  logger.info("Total spent time calculating and updating speed matrix -> "+timedoCopy/1000);
                  logger.info(" ");
                }
		logger.info("Cleanup done");
	}
	
	
	
	// FileTransfer interface
	
	public void transferBackResultFiles(List<Integer> fileIds) {
		Set<ResultFile> resultFiles = fileInformation.getResultFiles(fileIds);
		
		if (debug)
			logger.debug("Result files");
		
		// Create the group of operations for result files transfer
		int opGId = opGroups.addGroup(resultFiles.size());
		
		Copy c;
		FileInstanceId fId;
		for (ResultFile rf : resultFiles) {
			if (debug)
				logger.debug("  * " + rf.getOriginalLocation() + rf.getOriginalName());
			
			fId = rf.getFileInstanceId();
			
			c = new Copy(fId,
						 opGId,
						 FileRole.RESULT_FILE,
						 nameToLogicalFile.get(fId.getRenaming()),
						 rf.getOriginalName(),
						 rf.getOriginalLocation(),
						 null);
			queue.enqueue(c);
		}
	}
	
	
	public void checkResultFilesTransferred() {
		while (queue.getWaiting() < POOL_SIZE) {
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
				// Ignore
			}
		}
		appTransferEvents.resultFilesTransferred();
	}
	
	
	public void transferFileForOpen(FileAccessId faId, Location targetLocation) {
		if (debug)
			logger.debug("File for Open");
		
		/* If the application requests to open a file in W mode, no transfer is actually
		 * performed, we only update the information about the location of the new version.
		 * And then we inform the application that it can continue
		 */
		if (faId instanceof WAccessId) {
			WAccessId waId = (WAccessId)faId;
			FileInstanceId targetFile = waId.getWrittenFileInstance();
			String targetName = targetFile.getRenaming();
			
			if (debug)
				logger.debug(targetFile + " to be opened as " + targetLocation + targetName);
			
			List<Location> location = new LinkedList<Location>();
			location.add(targetLocation);

			newFileVersion(targetFile,
                                                 targetName,
                                                 location);

                        //Updating file information store
			fileInformation.addLocation(targetFile,
										targetLocation.getHost(), targetLocation.getPath());

                        //Updating replicated file information store on Task Scheduler
			schedFTMUpdate.addLocation(targetFile,
                                                                                targetLocation.getHost(), targetLocation.getPath());		
	
			appTransferEvents.fileForOpenTransferred();
		}
		else {
			List<FileAccessId> list = new LinkedList<FileAccessId>();
			list.add(faId);
			transferFiles(list, targetLocation, FileRole.OPEN_FILE);
		}
	}
	
	
	public void transferFileRaw(FileAccessId faId, Location targetLocation) {
		if (debug)
			logger.debug("Raw file");
		
		RAccessId raId = (RAccessId)faId;
		FileInstanceId sourceFile = raId.getReadFileInstance();
		int groupId = opGroups.addGroup(1); // Actually we don't care about the group
		String targetName = sourceFile.getRenaming();
		
		// Make a copy of the original logical file, we don't want to leave track
		LogicalFile origLogicalFile = nameToLogicalFile.get(targetName);
		LogicalFile logicalFile = null; 
		try {
			logicalFile = GAT.createLogicalFile(context, targetName, LogicalFile.TRUNCATE);
			for (URI u : origLogicalFile.getURIs())
				logicalFile.addURI(u);
		}
		catch (Exception e) {
			String errMessage = LF_PREPARATION_ERR + ": " +  raId +
			   					", target location is " + targetLocation;
			logger.error(errMessage, e);
			errorReport.throwError(ITConstants.FTM, errMessage, e);
		}
		
		Copy c = new Copy(sourceFile,
						  groupId,
				  		  FileRole.RAW_FILE,
				  		  logicalFile,
				  		  targetName,
				  		  targetLocation,
				  		  null);
		queue.enqueue(c);
	}
	
	
	public int transferFiles(List<FileAccessId> fileAccesses, Location targetLocation) {
		if (debug)
			logger.debug("Job files");
		
		return transferFiles(fileAccesses, targetLocation, FileRole.JOB_FILE);
	}
	
	
	public void deleteIntermediateFiles() {
		/*Set<String> hosts = new TreeSet<String>();
		
		// Search for intermediate files to delete
		for (LogicalFile logicalFile : nameToLogicalFile.values()) {
			try {
				for (File f : logicalFile.getFiles())
					if (f.getName().matches(IT_FILE_REG_EXPR))
						hosts.add(f.toGATURI().getHost());
	        }
	        catch (Exception e) {
	        	logger.error(DELETE_ERR, e);
	        	errorReport.throwError(ITConstants.FTM, DELETE_ERR, e);
	        	return;
	        }
		}*/
		
		List<String> hosts = projManager.getWorkers();
	
		Map<String,Integer> hostToRetry = newJob.getHostToRetry();
		Map<String,Boolean> hostToUsedInRun = newJob.getHostToUsedInRun();

                //Avoid to launch cleanScripts in non used hosts
		for(Map.Entry<String,Boolean> u : hostToUsedInRun.entrySet()){
                  if(u.getValue()== false){
                    hosts.remove(u.getKey());
                  }
                }

                //Avoid to launch cleanScripts in excluded hosts
		for(Map.Entry<String,Integer> r : hostToRetry.entrySet()){
                  if(r.getValue()<= 0){
		   hosts.remove(r.getKey());
		  }                          
                }

		if (hosts.isEmpty()) {
			appTransferEvents.intermediateFilesDeleted();
			return;
		}
		
		URI[] cleanScripts = new URI[hosts.size()];
		String[] cleanParams = new String[hosts.size()];
		int i = 0;
		for (String host : hosts) {
			String installDir = projManager.getProperty(host, ITConstants.INSTALL_DIR);
			String workingDir = projManager.getProperty(host, ITConstants.WORKING_DIR);
			
			String user = projManager.getProperty(host, ITConstants.USER);
			if (user == null) user = "";
			else 			  user += "@";
			
			try {
				cleanScripts[i] = new URI(ANY_PROT + user + host + "/" + installDir + CLEAN_SCRIPT);
			}
			catch (URISyntaxException e) {
				logger.error(DELETE_ERR, e);
	        	errorReport.throwError(ITConstants.FTM, DELETE_ERR, e);
	        	return;
			}
			cleanParams[i++] = workingDir;
		}
		
		// Return value used to make the call synchronous
		i = newJob.newCleanJob(cleanScripts, cleanParams);
		
		appTransferEvents.intermediateFilesDeleted();
	}
	
	
	// Private method that performs file transfers
	@SuppressWarnings("unchecked")
	private int transferFiles(List<FileAccessId> fileAccesses,
							  Location targetLocation,
							  FileRole role) {
		
		if (fileAccesses.size() == 0) return FileTransfer.FILES_READY;
		
		RAccessId raId;
		WAccessId waId;
		RWAccessId rwaId;
		LogicalFile logicalFile = null, origLogicalFile = null;
		FileInstanceId sourceFile = null, targetFile;
		String targetName = null;
		Method postProcessMethod = null;
		int writeOps = 0;
		
		// Create the group of operations for files
		int groupId = opGroups.addGroup(fileAccesses.size());
		
		for (FileAccessId faId : fileAccesses) {
			// May be necessary to the value of the FileAccessId object (get rid of the stub)
			faId = (FileAccessId)PAFuture.getFutureValue(faId);
			
			if (faId instanceof RAccessId) {
				raId = (RAccessId)faId;
				sourceFile = raId.getReadFileInstance();

				//If 1st Version, transfer with original name
				if (sourceFile.getVersionId() == 1)
				  targetName = fileInformation.getOriginalName(sourceFile);
				else
				  targetName = sourceFile.getRenaming();				

				logicalFile = nameToLogicalFile.get(sourceFile.getRenaming());
				postProcessMethod = readPostProcess;
			}
			else {
				if (faId instanceof WAccessId) { // Not possible for an open file role
					// "False" transfer, no real transfer is needed
					waId = (WAccessId)faId;
					targetFile = waId.getWrittenFileInstance();
					writeOps++;
				}
				else { // instance of RWAccessId
					rwaId = (RWAccessId)faId;
					sourceFile = rwaId.getReadFileInstance();
					targetFile = rwaId.getWrittenFileInstance();
					
					origLogicalFile = nameToLogicalFile.get(sourceFile.getRenaming());
					try {
						logicalFile = GAT.createLogicalFile(context, sourceFile.getRenaming(), LogicalFile.TRUNCATE);
						// Make a copy of the logical file, to avoid modifying the original when replicating
						for (URI u : origLogicalFile.getURIs())
							logicalFile.addURI(u);
					}
					catch (Exception e) {
						String errMessage = LF_PREPARATION_ERR + ": " +  rwaId + ", target location is "
											+ targetLocation + ", role is " + role;
						logger.error(errMessage, e);
						errorReport.throwError(ITConstants.FTM, errMessage, e);
						
						/* Return the identifier of the group, anyway this group will never have all its
						 * transfers done because this one hasn't been requested
						 */
						return groupId;
					}
				}
				targetName = targetFile.getRenaming();
			}
			
			if (!(faId instanceof WAccessId)) {
				if (debug)
					logger.debug("File: " + faId.getClass().getSimpleName()
							   	 + "(GID = " + groupId  + "): "
							   	 + sourceFile
							   	 + " to " + targetLocation + targetName);
				
				Copy c = new Copy(sourceFile,
								  groupId,
								  role,
								  logicalFile,
								  targetName,
								  targetLocation,
								  postProcessMethod);

				queue.enqueue(c);

			}
			else {
				if (debug)
					logger.debug("File: " + faId.getClass().getSimpleName()
								 + "(GID = " + groupId  + "): "
								 + "will be generated in "
								 + targetLocation + targetName);
			}
		}
		
		// Check if all requested transfers are ready
		int response;
		if (writeOps > 0) {
			int transfersLeft = 0;
			try {
				transfersLeft = opGroups.removeMembers(groupId, writeOps);
			}
			catch (ElementNotFoundException e) {
				String errMessage = TRANSFERS_LEFT_ERR + ": group is " +  groupId
									+ ", target location is " + targetLocation + ", role is " + role;
				logger.error(errMessage, e);
				errorReport.throwError(ITConstants.FTM, errMessage, e);
				
				return groupId;
			}
			if (transfersLeft == 0)
				response = FileTransfer.FILES_READY;
			else
				response = groupId;
		}
		else
			response = groupId;
		
		return response;
	}
	
	
	public int copyFile(FileInstanceId fId,
						FileRole role,
						String targetName,
						Location targetLocation) {
		
		int groupId = opGroups.addGroup(1);
		LogicalFile logicalFile = nameToLogicalFile.get(fId.getRenaming());
		
		Copy c = new Copy(fId, groupId, role, logicalFile,
						  targetName, targetLocation,
						  null);

		queue.enqueue(c);
		
		return groupId;
	}
	
	
	// Post processing method for file copy
	
	@SuppressWarnings("unused")
	protected void readPostProcess(Copy c) {
		// File transferred for reading
		FileInstanceId faId = c.getFileInstanceId();
		// Location where the source file has been transferred to
		Location loc = c.getTargetLocation();
		
		// Inform the File Information Provider about the new location of the file
		fileInformation.addLocation(faId, loc.getHost(), loc.getPath());

		schedFTMUpdate.addLocation(faId, loc.getHost(), loc.getPath());
	}
	
	
	// FileInfoUpdate interface
	
	 public void newFileVersion(FileInstanceId newFileId,
                                                           String name,
                                                           List<Location> locations) {
                // Obtain the renaming for this file version
                String renaming = newFileId.getRenaming();

                if (debug) {
                        logger.debug("Create new file version:");
                        logger.debug("  * File Id: " + newFileId);
			for (Location l : locations) {
                           logger.debug("  * Location: " + l + name);
                        }
                }

		 LogicalFile logicalFile = null;

		 try{
		  logicalFile = GAT.createLogicalFile(context, renaming, LogicalFile.TRUNCATE);
	   	 }
		 catch (Exception e) {
                        String errMessage = VERSION_CREATION_ERR + ": Name is " + renaming
                                                                + ", Initial location is " + locations.get(0);
                        logger.error(errMessage, e);
                        errorReport.throwError(ITConstants.FTM, errMessage, e);
                        return;
                  }


		for (Location l : locations) {

		  String user = null;
     
		  if(projManager.getProperty(l.getHost(), ITConstants.USER) != null) {
                    user = projManager.getProperty(l.getHost(), ITConstants.USER);
                  }
                  else{
                    user = projManager.getDataNodeProperty(l.getHost(), ITConstants.USER);
                  }

                  if (userNeeded && user != null)
                        user += "@";
                  else
                        user = "";

                  try {
                        URI u = new URI("any://" + user + l.getHost() + "/" + l.getPath() + name);
                        logicalFile.addURI(u);
                  }
                  catch (Exception e) {
                        String errMessage = VERSION_CREATION_ERR + ": Name is " + renaming
                                                                + ", Error Creating URI. ";
                        logger.error(errMessage, e);
                        errorReport.throwError(ITConstants.FTM, errMessage, e);
                        return;
                  }
		}
                 nameToLogicalFile.put(renaming, logicalFile);
        }

	
	// Controller interfaces implementation
	
	// Binding controller interface
	
	public String[] listFc() {
	    return new String[] { "TransferStatus" , "FileInformation", "AppTransferEvents" , "NewJob", "ErrorReport", "SchedFTMUpdate"};
	}

	public Object lookupFc(final String cItf) {
		if (cItf.equals("TransferStatus")) {
			return transferStatus;
	    }
		else if (cItf.equals("FileInformation")) {
			return fileInformation;
	    }
		else if (cItf.equals("AppTransferEvents")) {
			return appTransferEvents;
	    }
		else if (cItf.equals("NewJob")) {
			return newJob;
	    }
		else if (cItf.equals("ErrorReport")) {
			return errorReport;
	    }
		else if (cItf.equals("SchedFTMUpdate")) {
                        return schedFTMUpdate;
	    }

	    return null;
	}

	public void bindFc(final String cItf, final Object sItf) {
		if (cItf.equals("TransferStatus")) {
			transferStatus = (TransferStatus)sItf;
	    }
		else if (cItf.equals("FileInformation")) {
			fileInformation = (FileInformation)sItf;
	    }
		else if (cItf.equals("AppTransferEvents")) {
			appTransferEvents = (AppFileEvents)sItf;
	    }
		else if (cItf.equals("NewJob")) {
			newJob = (JobCreation)sItf;
	    }
		else if (cItf.equals("ErrorReport")) {
			errorReport = (ITError)sItf;
	    }
	        else if (cItf.equals("SchedFTMUpdate")) {
                        schedFTMUpdate = (SchedFTMUpdate)sItf;
            }

	}

	public void unbindFc(final String cItf) {
	    if (cItf.equals("TransferStatus")) {
	    	transferStatus = null;
		}
	    else if (cItf.equals("FileInformation")) {
	    	fileInformation = null;
		}
	    else if (cItf.equals("AppTransferEvents")) {
	    	appTransferEvents = null;
		}
	    else if (cItf.equals("NewJob")) {
	        newJob = null;
	    }
	    else if (cItf.equals("ErrorReport")) {
	    	errorReport = null;
		}

	    else if (cItf.equals("SchedFTMUpdate")) {
                schedFTMUpdate = null;
            }
	}
	
	
	
	// Threads that handle file transfer requests
	private class TransferDispatcher extends RequestDispatcher<FileOperation> {
		
		// Object to invoke postProcess methods on
		private FileTransferManager associatedFTM;
		
		// Transfers in progress
        private Map<URI, Copy> inProgress;
		
        
		public TransferDispatcher(RequestQueue<FileOperation> queue,
								  FileTransferManager associatedFTM) {
			super(queue);
			this.associatedFTM = associatedFTM;
			this.inProgress = new THashMap<URI, Copy>(POOL_SIZE + POOL_SIZE / 2);
		}
		
		public void processRequests() {
			FileOperation fOp;
			Copy c;
			Delete d;

			while (true) {
				fOp = queue.dequeue();
				if (fOp == null) break;
				
				// What kind of operation is requested?
				if (fOp instanceof Copy) { 		// File transfer (copy)
					c = (Copy)fOp;
					doCopy(c);
				}
				else { // fOp instanceof Delete
					d = (Delete)fOp;
					doDelete(d);
				}
				
				// Check end state of the operation
				switch (fOp.getEndState()) {
					case OP_OK:
						postProcess(fOp);
						checkNotifications(fOp);
						break;
					case OP_IN_PROGRESS:

                		        break;

					default: // OP_FAILED or OP_PREPARATION_FAILED
						notifyFailure(fOp);
						break;
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		private void doCopy(Copy c) {
                        double iTimeStamp = 0;

			String targetUser = projManager.getProperty(c.getTargetLocation().getHost(),
                                                        ITConstants.USER);
			if (userNeeded && targetUser != null)
				targetUser += "@";
			else
				targetUser = "";

			LogicalFile logicalFile = c.getLogicalFile();
			URI targetURI = null;
			try {
				targetURI = new URI("any://"
									+ targetUser
									+ c.getTargetLocation().getHost() + "/"
									+ c.getTargetLocation().getPath()
									+ c.getTargetName());
			}
			catch (URISyntaxException e) {
				c.setEndState(OpEndState.OP_PREPARATION_FAILED);
				c.setException(e);
				return;
			}

			synchronized(inProgress) {
				Copy copyInProgress = inProgress.get(targetURI);
				if (copyInProgress == null)
					inProgress.put(targetURI, c);
				else {
					// The same operation is already in progress - no need to repeat it
					c.setEndState(OpEndState.OP_IN_PROGRESS);

					// This group must be notified as well when the operation finishes
					copyInProgress.addGroupId(c.getGroupIds().get(0));

					if (debug)
						logger.debug("THREAD " + Thread.currentThread().getName() +
									 " - A copy to " + targetURI + " is already in progress, skipping replication");
					return;
				}

                                if(timeMeasuring) iTimeStamp = System.currentTimeMillis();
                                String source = null;

                                try{
                                 source = logicalFile.getURIs().get(0).getHost();
                                }catch (Exception e) {
                                        c.setEndState(OpEndState.OP_FAILED);
                                        c.setException(e);
                                        return;
                                }

                                String target = targetURI.getHost();
                                //if(debug) logger.debug("Tranfer Between: "+source+" -> "+target);

                                //Updating Host to Simultaneous files transfers
                                long fileSize = fileInformation.getSize(c.getFileInstanceId());

                                if(hostToSimTrfFiles.get(source) != null){
                                  Map<String,List<Long>> hostToList = hostToSimTrfFiles.get(source);
                                  List<Long> l = null;

                                  if(hostToList.get(target) != null){
                                   l = hostToList.get(target);

                                   //Updating number of simultaneous file transfers
                                   long nSimFiles = l.get(0);
                                   nSimFiles++;
                                   l.set(0,nSimFiles);

                                   //Updating the amount of data to transfer
                                   long trfSize = l.get(2);
                                   trfSize += fileSize;
                                   l.set(2,trfSize);
                                 }
                                 else{
                                   l = new ArrayList<Long>(3);
                                   l.add((long) 1);
                                   l.add(System.currentTimeMillis());
                                   l.add(fileSize);

				   hostToList.put(target,l);
                                 }
                                }
                                else{
                                  Map<String,List<Long>> hostToList = new THashMap<String,List<Long>>();
                                  List<Long> l = new ArrayList<Long>(3);

                                  l.add((long) 1);
                                  l.add(System.currentTimeMillis());
                                  l.add(fileSize);
                                  
                                  hostToList.put(target,l);
                                  hostToSimTrfFiles.put(source,hostToList);
                                }

                           if(timeMeasuring) timedoCopy+= System.currentTimeMillis() - iTimeStamp;
			} //End inProgress

			if (debug)
				logger.debug("THREAD " + Thread.currentThread().getName() +
							 " - Copy file " + c.getFileInstanceId() + " to " + c.getTargetLocation() + c.getTargetName());
			
			FileTransferUsageRecord ftur = null;
			try {
				// Perform the copy
				if (slaEnabled) {
					/* We ignore which source file will be chosen by GAT in the replication.
					 * Therefore, we create the usage record with the logical file name as source name.
					 * Nevertheless, we also pass a source file to create a UR with information about
					 * the physical file (such as the size)
					 */
					String logicName = logicalFile.getName();
					ftur = getUsageRecord(logicalFile.getFiles().get(0),
										  logicName,
										  targetURI.toString());
				}

			//Replicating file
			logicalFile.replicate(targetURI);

			}
			catch (Exception e) {
				c.setEndState(OpEndState.OP_FAILED);
				c.setException(e);
				return;
			}
			finally {
				synchronized(inProgress) {

                                                 if(timeMeasuring) iTimeStamp = System.currentTimeMillis();
					         String source = null;
                                            
                                                 try{
                                                    source = logicalFile.getURIs().get(0).getHost();
                                                  }catch (Exception e) {
                                                   c.setException(e);
                                                   return;
                                                  }

                                                 String target = c.getTargetLocation().getHost();

                                                 //Updating host to simultaneous transfers structure
                                                 Map<String,List<Long>> hostToList = hostToSimTrfFiles.get(source);
                                                 List<Long> l = hostToList.get(target);

                                                 //Updating number of simultaneous files in transfer
                                                 long nSimFiles = l.get(0);
                                                 nSimFiles--;

                                                 //Calculating transfer speed
                                                 if(nSimFiles == 0){
                                                   float trfTime = ((System.currentTimeMillis()) - l.get(1))/(float) 1000;
                                                   long size = l.get(2); //In bytes

                                                    //If amount of data is greater than a minimum threshold specified, a new net speed will be updated
                                                   if((size > ((ITConstants.NET_SPEED_THRESHOLD)*1000000)) && (trfTime > 0)){
                                                     float newNetSpeed = (((size*8)/trfTime)/1000000); //Mbps

                                                    //Changing speed of matrix link
                                                     newJob.speedMatrixUpdate(source,target,newNetSpeed);
                                                    
						   // if(debug) logger.debug("End of transfer between: "+source+" -> "+target+" | Size -> "+size+" Time -> "+trfTime+" NetSpeed -> "+newNetSpeed);
                                                   }

                                                   //Removing from transfer structure
                                                   hostToList.remove(target);
                                                   //if(debug) logger.debug("Removing target -> "+target+" from source row: "+source);                                  
                                                 }
                                                 else{
                                                   l.set(0,nSimFiles);
                                                 }
                                        if(timeMeasuring) timedoCopy+= System.currentTimeMillis() - iTimeStamp;

					Copy finishedCopy = inProgress.remove(targetURI);
					if (slaEnabled) ftur.stop(finishedCopy.getGroupIds());
				} //End inProgress
			}

			c.setEndState(OpEndState.OP_OK);
		}
		
		private FileTransferUsageRecord getUsageRecord(File oneSource,
													   String sourceLogicalFile,
													   String targetFile) {
			String appName = System.getProperty(ITConstants.IT_APP_NAME),
				   slaId = System.getProperty(ITConstants.IT_SLA_ID),
				   toFile = Long.toString(System.currentTimeMillis())
				            + "-fileCopy-" + sourceLogicalFile + ".ur.xml",
				   fileDestination = targetFile;
			
			if (debug)
				logger.debug("Writing usage record to " + toFile);
			
			return new FileTransferUsageRecord(appName,
											   slaId,
											   toFile,
											   "true",
											   oneSource,
											   sourceLogicalFile,
											   fileDestination);
        }
		
		
		private void doDelete(Delete d) {
			if (debug)
				logger.debug("THREAD Delete " + d.getFile());
			
			try {
				d.getFile().delete();
			}
			catch (Exception e) {
				d.setEndState(OpEndState.OP_FAILED);
				d.setException(e);
				return;
			}
			
			d.setEndState(OpEndState.OP_OK);			
		}
		
		
		private void checkNotifications(FileOperation fOp) {
			List<Integer> groupIds = fOp.getGroupIds();
            for (int groupId : groupIds) {
            	int numOps = 0;
				try {
					numOps = opGroups.removeMember(groupId);
				}
				catch (ElementNotFoundException e) {
					/* An operation belonging to the same group as the current one
					 * has failed and the group has been removed, don't do anything
					 */
					logger.error("Already removed group", e);
					return;
				}
				
				// Are there any operations of this group left?
				if (numOps == 0) {
					opGroups.removeGroup(groupId);
					// Notify the end of the group of operations
					switch (fOp.getRole()) {
						case JOB_FILE:
							   transferStatus.fileTransferInfo(groupId, TransferState.DONE, null);
							break;
						case OPEN_FILE:
							appTransferEvents.fileForOpenTransferred();
							break;
						case RESULT_FILE:
							break;
						case DELETE_FILE:
							appTransferEvents.intermediateFilesDeleted();
							break;
						case RAW_FILE:
							appTransferEvents.rawFileTransferred();
							break;
					}
				}
            }
		}
		
		
		private void notifyFailure(FileOperation fOp) {
			if (debug)
				logger.error("THREAD File Operation failed on " + fOp.getFileInstanceId()
							 + ", file role is " + fOp.getRole()
							 + ", operation end state is " + fOp.getEndState(),
							 fOp.getException());
			
			for (int groupId : fOp.getGroupIds()) {
				if (!opGroups.exists(groupId)) {
					// A previous failure in the same group has already been notified, do nothing
					continue;
				}
				
				opGroups.removeGroup(groupId);
			
				// Notify the end of the group of operations
				switch (fOp.getRole()) {
					case JOB_FILE:
						Copy c = (Copy)fOp;
						transferStatus.fileTransferInfo(groupId,
                                                                                          TransferState.FAILED,
											  "Transfer of " + c.getTargetName() +
										          " to " + c.getTargetLocation() + " failed." +
										          " Operation end state is " + fOp.getEndState() + "\n" +
											  "Exception message is:\n" + fOp.getException().getMessage());
						break;
					case OPEN_FILE:
						errorReport.throwError(ITConstants.FTM,
		   			   			   			   TRANSFER_ERR + ": File " + fOp.getFileInstanceId() +
		   			   			   			   ", Role " + fOp.getRole() + "." +
		   			   			   			   " Operation end state is " + fOp.getEndState(),
		   			   			   			   fOp.getException());
						break;
					case RESULT_FILE:
						errorReport.throwError(ITConstants.FTM,
		   			   			   			   TRANSFER_ERR + ": File " + fOp.getFileInstanceId() +
		   			   			   			   ", Role " + fOp.getRole() + "." +
		   			   			   			   " Operation end state is " + fOp.getEndState(),
		   			   			   			   fOp.getException());
						break;
					case DELETE_FILE:
						Delete d = (Delete)fOp;
						String fileName = d.getFile().getName();
						
						errorReport.throwError(ITConstants.FTM,
		   			   			   			   DELETE_ERR + ": File " + fileName +
		   			   			   			   ", Role " + d.getRole() + "." +
		   			   			   			   " Operation end state is " + fOp.getEndState(),
		   			   			   			   fOp.getException());
						break;
					case RAW_FILE:
						errorReport.throwError(ITConstants.FTM,
		   			   			   			   TRANSFER_ERR + ": File " + fOp.getFileInstanceId() +
		   			   			   			   ", Role " + fOp.getRole() + "." +
		   			   			   			   " Operation end state is " + fOp.getEndState(),
		   			   			   			   fOp.getException());
						break;
				}
			}
		}
		
		private void postProcess(FileOperation fOp) {
			// Invoke the post processing method after the operation has been performed
			Method postProcess = fOp.getPostProcessMethod();
			try {
				if (postProcess != null)
					postProcess.invoke(associatedFTM, fOp);
			}
			catch (Exception e) {
				String errMessage = POST_PROCESS_ERR + ": Method " + postProcess.getName();
				logger.error(errMessage, e);
				errorReport.throwError(ITConstants.FTM, errMessage, e);
				return;
			}
		}
		
	}
	
}
