package integratedtoolkit.components.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Random;

import gnu.trove.map.hash.*;

import org.apache.log4j.Logger;

import org.objectweb.fractal.api.control.BindingController;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.RunActive;
import org.objectweb.proactive.Service;
import org.objectweb.proactive.core.util.wrapper.StringWrapper;

import org.gridlab.gat.GATContext;

import integratedtoolkit.ITConstants;
import integratedtoolkit.components.FileInformation;
import integratedtoolkit.components.JobCreation;
import integratedtoolkit.components.JobStatus;
import integratedtoolkit.components.Preparation;
import integratedtoolkit.components.Schedule;
import integratedtoolkit.components.SchedulerUpdate;
import integratedtoolkit.components.SchedFTMUpdate;
import integratedtoolkit.components.TaskStatus;
import integratedtoolkit.components.TaskStatus.*;
import integratedtoolkit.interfaces.ITError;
import integratedtoolkit.log.Loggers;
import integratedtoolkit.types.ExecutionParams;
import integratedtoolkit.types.Parameter;
import integratedtoolkit.types.Parameter.FileParameter;
import integratedtoolkit.types.Parameter.*;
import integratedtoolkit.types.Task;
import integratedtoolkit.types.file.FileInstanceId;
import integratedtoolkit.types.file.Location;
import integratedtoolkit.types.file.FileInfo;
import integratedtoolkit.types.file.FileAccessId.*;
import integratedtoolkit.util.ConstraintManager;
import integratedtoolkit.util.ProjectManager;
import integratedtoolkit.util.ResourceManager;
import integratedtoolkit.util.HistoricalManager;

public class TaskScheduler implements Schedule, SchedulerUpdate, SchedFTMUpdate, JobStatus, Preparation,
									  BindingController,
									  RunActive {
	
	// Constants definition
	private static final String RES_LOAD_ERR 	= "Error loading resource information";
	private static final String CONSTR_LOAD_ERR = "Error loading application constraints";
	private static final String PROJ_LOAD_ERR	= "Error loading project information";
	private static final String HIST_LOAD_ERR 	="Error loading historical information";	

	// Client interfaces
	private JobCreation newJob;
	private TaskStatus taskStatus;
	private FileInformation fileLocation;
	private ITError errorReport;

	// GAT context
        private GATContext context;

        // GAT broker adaptor information
        private boolean userNeeded;
	private boolean usingGlobus;
	
	//Net speed in Mbps
	float netSpeed = 0;

	//Locations availability flag
	private boolean locationsDefined = false;
	
	// Object that stores the information about all available resources
	private ResourceManager resManager;
	
	// Object that stores the constraints for each task
	private ConstraintManager constrManager;
	
	// Object that stores the information about the current project
	private ProjectManager projManager;

	// Object that stores the information about the historical
	private HistoricalManager histManager;
	
	// Pending tasks
	private LinkedList<Task> pendingTasks;
	
	// Tasks to reschedule
	private LinkedList<Task> tasksToReschedule;
	
	// Map: jobId -> taskId
	private Map<Integer,Integer> jobToTask;
	
	// Map: taskId -> jobId (for pending tasks to reschedule)
	private Map<Integer,Integer> taskToJob;
	
	// Map: jobId -> host of execution
	private Map<Integer,String> jobToHost;

	//Mean wait time in queue

	private Map<String,Float> hostToWTQueue; //Map of Host -> Time to wait in queue in order to have slot.
	private Map<String,Map<Integer,Integer>> hostToPendingMethodId; //host name -> Number of method id tasks in pending queue.

	//Mean execution time in queue
	private Map<Integer,Map<String,List>> TaskIdToHostsExecTimeValues; //Map of TaskId -> (Map Host -> Execution Time Values)
        private Map<Integer,Map<String,Float>> TaskIdToHostsMeanExecTime; //Map of TaskId -> (Map Host -> Mean Execution Time)
        private Map<Integer,Map<String,Float>> hTaskIdToHostsMeanExecTime; //Historical map of TaskId -> (Map Host -> Mean Execution Time).
	
	//Availability
	private Map<String,Float> hostToAvailability; //Map Host -> Availability Score
	private Map<String,Float> hHostToAvailability; //Historical of Map Host -> Availability Score

	private Map<String,Integer> hostToSubmits; // Map Host -> Total amount of submitted jobs on host
	private Map<String,Integer> hostToOkSubmits; //Map Host -> Number ok jobs submitted ok

	private Map<String,Integer> hostToRetry; //Map Host -> Reschedule tries
        private Map<String,Boolean> hostToUsedInRun; //Map Host -> Used in Run?	
	private List<String> excludedResources; //List of excluded resources by in run errors

	//Speed matrix structure
	private Map<String,Map<String,Float>> hostToSpeedMatrix; 

	//File information structures
	private Map<Integer,FileInfo> fIdToFileInfo;

	// Component logger - No need to configure, ProActive does
	private static final Logger logger = Logger.getLogger(Loggers.TS_COMP);
	private static final boolean debug = logger.isDebugEnabled();
	
	// Language
	private static final boolean isJava = System.getProperty(ITConstants.IT_LANG) != null
    									  && System.getProperty(ITConstants.IT_LANG).equals("java")
    									  ? true : false;

	//Performance debug
         private boolean  timeMeasuring = false;
	 private double assignTStamp = 0;
	 private double timePredTStamp = 0;
	 private double addQueueTStamp = 0;
         private double addExecTStamp = 0;
	 private double addUpdateWTQTStamp = 0;
	 private double getExecTime = 0;	
	
	public TaskScheduler(){}

	// RunActive interface
	
	public void runActivity(Body body) {
		body.setImmediateService("terminate", new Class[] {});
		
		Service service = new Service(body);
		service.fifoServing();
	}
	
	
	// Server interfaces implementation
	
	// Preparation interface
	
	public StringWrapper initialize() {
		if (pendingTasks == null)
			pendingTasks = new LinkedList<Task>();
		else
			pendingTasks.clear();
		
		if (tasksToReschedule == null)
			tasksToReschedule = new LinkedList<Task>();
		else
			tasksToReschedule.clear();
		
		if (jobToTask == null)
			jobToTask = new TreeMap<Integer,Integer>();
		else
			jobToTask.clear();
		
		if (taskToJob == null)
			taskToJob = new THashMap<Integer,Integer>();
		else
			taskToJob.clear();
		
		if (jobToHost == null)
			jobToHost = new TreeMap<Integer,String>();
		else
			jobToHost.clear();

		if(hostToPendingMethodId == null)
                        hostToPendingMethodId = new THashMap<String,Map<Integer,Integer>>();
                else
                        hostToPendingMethodId.clear();
                        
		if (hostToWTQueue == null)
                        hostToWTQueue = new THashMap<String,Float>();
                else
                        hostToWTQueue.clear();
		 
		if( TaskIdToHostsExecTimeValues == null)
			  TaskIdToHostsExecTimeValues = new THashMap<Integer,Map<String,List>>();
                else
                         TaskIdToHostsExecTimeValues.clear();

                if( TaskIdToHostsMeanExecTime == null)
                         TaskIdToHostsMeanExecTime = new THashMap<Integer,Map<String,Float>>();
                else
                         TaskIdToHostsMeanExecTime.clear();

		if( hTaskIdToHostsMeanExecTime == null)
                         hTaskIdToHostsMeanExecTime = new THashMap<Integer,Map<String,Float>>();
                else
                         hTaskIdToHostsMeanExecTime.clear();

	        if( hostToAvailability == null)
                          hostToAvailability = new THashMap<String,Float>();
                else
                          hostToAvailability.clear();

	        if( hHostToAvailability == null)
                         hHostToAvailability = new THashMap<String,Float>();
                else
                         hHostToAvailability.clear();

		if( hostToSubmits == null)
                         hostToSubmits = new THashMap<String,Integer>();
                else
                         hostToSubmits.clear();

		if( hostToOkSubmits == null)
                         hostToOkSubmits = new THashMap<String,Integer>();
                else
                         hostToOkSubmits.clear();
	
		if( hostToRetry == null)
                         hostToRetry = new THashMap<String,Integer>();
                else
                         hostToRetry.clear();

		if( hostToUsedInRun == null)
                         hostToUsedInRun = new THashMap<String,Boolean>();
                else
                         hostToUsedInRun.clear();
		
	        if (excludedResources == null)
                        excludedResources = new LinkedList<String>();
                else
                        excludedResources.clear();	

		if( hostToSpeedMatrix == null)
                         hostToSpeedMatrix = new THashMap<String,Map<String,Float>>();
                else
                         hostToSpeedMatrix.clear();

		if( fIdToFileInfo == null)
                         fIdToFileInfo = new THashMap<Integer,FileInfo>();
                else
                         fIdToFileInfo.clear();


		// GAT adaptor path
                System.setProperty(ITConstants.GAT_ADAPTOR,
                                                   System.getenv("GAT_LOCATION") + ITConstants.GAT_ADAPTOR_LOC);

                if (context == null) {
                        context = new GATContext();
                        String brokerAdaptor = System.getProperty(ITConstants.GAT_BROKER_ADAPTOR),
                               fileAdaptor   = System.getProperty(ITConstants.GAT_FILE_ADAPTOR);
                        context.addPreference("ResourceBroker.adaptor.name", brokerAdaptor);
                        context.addPreference("File.adaptor.name", fileAdaptor + ", local");
                        usingGlobus = brokerAdaptor.equalsIgnoreCase("globus");
                        userNeeded = brokerAdaptor.regionMatches(true, 0, "ssh", 0, 3);
                }


		if (resManager == null)
			try {
				resManager = new ResourceManager();
			}
			catch (Exception e) {
				logger.error(RES_LOAD_ERR, e);
				return new StringWrapper(ITConstants.TS + ": " + RES_LOAD_ERR);
			}
		else
			resManager.freeAllResources();
	

		if (histManager == null) {
                        try {
                                histManager = new HistoricalManager();
                        }
                        catch (Exception e) {
                                logger.error(HIST_LOAD_ERR, e);
                                return new StringWrapper(ITConstants.TS + ": " + HIST_LOAD_ERR);
                        }
                }

	
		if (projManager == null) {
			try {
				projManager = new ProjectManager();
			}
			catch (Exception e) {
				logger.error(PROJ_LOAD_ERR, e);
				return new StringWrapper(ITConstants.TS + ": " + PROJ_LOAD_ERR);
			}
		
			// Seting the available resources
			List<String> allResources = projManager.getWorkers();
 
  			for (String res : allResources){
				int limitOfTasks = 0;

				if(projManager.getProperty(res, ITConstants.LIMIT_OF_TASKS) != null){
				  limitOfTasks = Integer.parseInt(projManager.getProperty(res, ITConstants.LIMIT_OF_TASKS));
				}
				else{
				  limitOfTasks = Integer.parseInt(resManager.getResCPUCount(res));	 
				}
				resManager.setMaxTaskCount(res,Integer.toString(limitOfTasks));			     
			}

			//Setting the availability in previous runs
			hHostToAvailability = histManager.getAvailability();

                        //If there isn't previous historical data, initializing structure
			if(hHostToAvailability.size() == 0){
			  for (String res : allResources){
                           hHostToAvailability.put(res,(float) 1);
			  }
			}

			//Setting max retries map
			if(hostToRetry.size() == 0){
                          for (String res : allResources){
			   if(projManager.getMaxRetries() == null){
 		             hostToRetry.put(res,Integer.parseInt(ITConstants.DEFAULT_MAX_RETRIES));
			   }
			   else{
			     hostToRetry.put(res,Integer.parseInt(projManager.getMaxRetries()));
			   }
                          }
                        }

                        //Setting used in run map
                        if(hostToUsedInRun.size() == 0){
                          for (String res : allResources){
                           hostToUsedInRun.put(res,false);
                          }
                        }

                        //Setting locations availability flag
                        locationsDefined = projManager.hasLocations();
	
			//Setting historical execution time of previous runs
                        hTaskIdToHostsMeanExecTime = histManager.getMeanTimeStructure("MeanExecTime");
		}

		if (constrManager == null) {
			if (isJava) {
				try {
					String appName = System.getProperty(ITConstants.IT_APP_NAME);
					constrManager = new ConstraintManager(Class.forName(appName + "Itf"));
				}
				catch (ClassNotFoundException e) {
					logger.error(CONSTR_LOAD_ERR, e);
					return new StringWrapper(ITConstants.TS + ": " + CONSTR_LOAD_ERR);
				}
			}
			else { // C binding
				try {
					constrManager = new ConstraintManager(System.getProperty(ITConstants.IT_CONSTR_FILE));
					logger.error(ITConstants.IT_CONSTR_FILE);
				}
				catch (Exception e) {
					logger.error(CONSTR_LOAD_ERR, e);
					return new StringWrapper(ITConstants.TS + ": " + CONSTR_LOAD_ERR);
				}
			}
		}

		logger.info("Initialization finished");
		
		return new StringWrapper(ITConstants.INIT_OK);
		// TODO: Load shared disks information
	}
	
	
	public void cleanup() {

                //Storing new historical data to historical file
                calcNewHist();
		logger.info("Cleanup done");
	}
	
	
	// Schedule interface
	
	public void scheduleTasks(List<Task> tasks) {
		if (debug) {
			StringBuilder sb = new StringBuilder("Schedule tasks: ");
			for (Task t : tasks)
				sb.append(t.getMethod().getName()).append("(").append(t.getId()).append(") ");
			
			logger.debug(sb);
		}
		
		// Try to assign tasks to available resources
		List<String> resources = null;
		List<String> workers = projManager.getWorkers();

		for (Task currentTask : tasks) {
			// Find available resources that match user constraints for this task
			try {
				resources = resManager.findResources(constrManager.getConstraints(currentTask));
			
				//Filtering the resources that are in resources file but not in project
                                for(int i=0;i< resources.size();i++){
                                    if(!workers.contains(resources.get(i))){
                                      resources.remove(resources.get(i));
                                    }
                                }

				//Excluding resources that there are run out of attempts
				for(String excl : excludedResources){
		                  resources.remove(excl);
                                }

                                if(debug)
				logger.debug("Constraints: " + constrManager.getConstraints(currentTask));

				if(resources.isEmpty()){
					if (debug) logger.debug("Trying to preschedule task " + currentTask.getId());
					resources = resManager.findResources(constrManager.getConstraintsPresched(currentTask));

					//Filtering the resources that are in resources file but not in project
                                        for(int i=0;i< resources.size();i++){
                                         if(!workers.contains(resources.get(i))){
                                           resources.remove(resources.get(i));
                                         }
                                        }
				}
			}
			catch (NoSuchMethodException e) {
				logger.error(CONSTR_LOAD_ERR, e);
				errorReport.throwError(ITConstants.TS, CONSTR_LOAD_ERR, e);
				return;
			}
			
			if (debug) {
				StringBuilder sb = new StringBuilder("Available suitable resources for task ");
				sb.append(currentTask.getId()).append(", ").append(currentTask.getMethod().getName()).append(":");
				for (String s: resources)
					sb.append(" ").append(s);
				
				logger.debug(sb);
			}
			
			// Schedule task
			if (!resources.isEmpty()) {

				List assignSet = assignTaskToBestResource(currentTask, resources);				

				String chosenResource = (String) assignSet.get(0);
                                List<FileInstanceId> filesToTransfer = (List<FileInstanceId>) assignSet.get(1);
		
				if (debug)
					logger.debug("Match: Task(" + currentTask.getId() + ", "
							     + currentTask.getMethod().getName() + ") "
							     + "Resource(" + chosenResource + ")");
				
				// Reserve the resource
				resManager.reserveResource(chosenResource);

				// Request the creation of a job form task
				sendJob(currentTask, chosenResource, filesToTransfer);
			}
			else
				pendingTasks.add(currentTask);
		}
		
		if (debug)
			if (!pendingTasks.isEmpty()) {
				StringBuilder sb = new StringBuilder("Pending tasks:");
				for (Task t : pendingTasks)
					sb.append(" ").append(t.getId());
				
				logger.debug(sb);
			}
	

	}

	
	public void rescheduleJob(int oldJobId) {

		// Get the corresponding task to resche a shallow copy of this HashMap instance: the keys and values themselves are not cloned.ule
		int taskId = jobToTask.remove(oldJobId);
		Task task = taskStatus.getTaskInfo(taskId);
		List<String> workers = projManager.getWorkers();

		// Find available resources that match user constraints for this task
		List<String> resources = null;
		try {
			resources = resManager.findResources(constrManager.getConstraints(task));

			//Filtering the resources that are in resources file but not in project
                         for(int i=0;i< resources.size();i++){
                          if(!workers.contains(resources.get(i))){
                            resources.remove(resources.get(i));
                          }
                         }
				
			  if(resources.isEmpty()){
				if (debug) logger.debug("Trying to preschedule task " + task.getId());
				resources = resManager.findResources(constrManager.getConstraintsPresched(task));

				//Filtering the resources that are in resources file but not in project
                                for(int i=0;i< resources.size();i++){
                                  if(!workers.contains(resources.get(i))){
                                   resources.remove(resources.get(i));
                                  }
                                }
			}

			 //Excluding resources that there are run out of tries
                         for(String excl : excludedResources){
                                  resources.remove(excl);
                         }
		}
		catch (NoSuchMethodException e) {
			logger.error(CONSTR_LOAD_ERR, e);
			errorReport.throwError(ITConstants.TS, CONSTR_LOAD_ERR, e);
			return;
		}
		
		/* Get the host where the task failed and remove it from the list
		 * so that it will not be chosen again
		 */ 
		String failedResource = jobToHost.get(oldJobId);
		
		//Removing worker from resource list in order to reschedule to other resources
		resources.remove(failedResource);
                if(debug){
		  logger.debug("Removing Failed Resource: -> "+failedResource);
		  logger.debug("Reschedule: Job " + oldJobId + " failed to run in " + failedResource);
                }
		// Reschedule task
		if (!resources.isEmpty()) {
			 List assignSet = assignTaskToBestResource(task, resources);

			 String newResource = (String) assignSet.get(0);
                         List<FileInstanceId> filesToTransfer = (List<FileInstanceId>) assignSet.get(1);

			if (debug)
				logger.debug("Re-Match: Task(" + task.getId() + ", "
						     + task.getMethod().getName() + ") "
						     + "Resource(" + newResource + ")");
			
			// Reserve the resource
			resManager.reserveResource(newResource);
			
			// Request the creation of a job for the task
			sendJobRescheduled(task, newResource, filesToTransfer);
		}
		else {
			taskToJob.put(taskId, oldJobId);
			jobToHost.put(oldJobId, failedResource);
			tasksToReschedule.add(task);
		}

		int hostRetries = hostToRetry.get(failedResource);
		
		if(hostRetries > 0){
		   if(debug) logger.debug("Adding resource again: -> "+failedResource+" | Lefting attempts -> "+hostRetries);

		   if(hostRetries == 1){
		     if(debug) logger.debug("Adding resource: -> "+failedResource+" to exclusion list ");
		     excludedResources.add(failedResource);
		   }
		  hostToRetry.put(failedResource,(--hostRetries));

		  //Adding again worker to resource list
		  resources.add(failedResource);
		}
		
		// Notify the job end to free the resource, in case another task can be scheduled in it
		// TODO: Maybe it would be better to remove the failed resource from the list?
		notifyJobEnd(oldJobId, JobEndStatus.TO_RESCHEDULE, null, null, null);
	}
	
	
	private void sendJob(Task task, String resource, List<FileInstanceId> filesToTransf) {
		String installDir = projManager.getProperty(resource, ITConstants.INSTALL_DIR);
		String workingDir = projManager.getProperty(resource, ITConstants.WORKING_DIR);
		String user = projManager.getProperty(resource, ITConstants.USER);
		
		// Prepare the execution parameters
		// TODO: Cost
		ExecutionParams execParams = new ExecutionParams(user,
				 										 resource,
				 										 installDir,
				 										 workingDir);

		// Request the creation of the job
		int jobId = newJob.newJob(task.getMethod(), execParams, filesToTransf);
	
		// Update mappings
		jobToTask.put(jobId, task.getId());
		jobToHost.put(jobId, resource);
	
		//Updating used in run map
		hostToUsedInRun.put(resource,true);
	}
	
	
	private void sendJobRescheduled(Task task, String resource, List<FileInstanceId> filesToTransf) {
		String installDir = projManager.getProperty(resource, ITConstants.INSTALL_DIR);
		String workingDir = projManager.getProperty(resource, ITConstants.WORKING_DIR);
		String user = projManager.getProperty(resource, ITConstants.USER);
		
		// Prepare the execution parameters
		// TODO: Cost
		ExecutionParams execParams = new ExecutionParams(user,
				 										 resource,
				 										 installDir,
				 										 workingDir);

		// Request the creation of the job
		int jobId = newJob.jobRescheduled(task.getMethod(), execParams, filesToTransf);
		
		// Update mappings
		jobToTask.put(jobId, task.getId());
		jobToHost.put(jobId, resource);

		//Updating used in run map
                hostToUsedInRun.put(resource,true);
       }

		
	// JobStatus interface
	
	public void notifyJobEnd(int jobId, JobEndStatus status, String message, Map<FileInstanceId,Location> locs, Map<FileInstanceId,List<Long>> sizeAndMod) {
		if (debug)
			logger.debug("Notification received for job " + jobId + " with end status "
					     + status + " and message \"" + message +"\"");
	
		//Updating file information structure	
		if(locs != null && sizeAndMod != null){
		  for (Map.Entry<FileInstanceId,Location> l :  locs.entrySet()){
		     addNewVersion(l.getKey());
		     addLocation(l.getKey(),l.getValue().getHost(),l.getValue().getPath());
		     addSize(l.getKey(),sizeAndMod.get(l.getKey()));
		  }
		}

		// JobEndStatus -> TaskEndStatus, propagate notification
		Integer taskId = jobToTask.get(jobId);
		jobToTask.remove(jobId);

		switch (status) {
			case OK:
				taskStatus.notifyTaskEnd(taskId, TaskEndStatus.OK, message);
				break;
			case TO_RESCHEDULE:
				// Do nothing, we just want the failed resource to be assigned to some other task
				break;
			case TRANSFERS_FAILED:
				taskStatus.notifyTaskEnd(taskId, TaskEndStatus.FAILED, message);
				break;
			case SUBMISSION_FAILED:
				taskStatus.notifyTaskEnd(taskId, TaskEndStatus.FAILED, message);
				break;
			case EXECUTION_FAILED:
				taskStatus.notifyTaskEnd(taskId, TaskEndStatus.FAILED, message);
				break;
		}
		
		// Obtain freed resource
		String hostName = jobToHost.remove(jobId);
		
		// Tell the resource manager that the resource is free
		  resManager.freeResource(hostName);
		
		// First check if there is some task to reschedule
		if (!tasksToReschedule.isEmpty()) {
			Task chosenTask = assignRescheduledTask(hostName);

			if (chosenTask != null) {
				// Task rescheduled
				if (debug)
					logger.debug("Freed Re-Match: Task(" + chosenTask.getId() + ", "
								 + chosenTask.getMethod().getName() + ") "
								 + "Resource(" + hostName + ")");
			
			        List trfSet = trfTimePredictor(chosenTask,hostName);
                                List<FileInstanceId> filesToTransfer = (List<FileInstanceId>) trfSet.get(1);
	
				tasksToReschedule.remove(chosenTask);
				resManager.reserveResource(hostName);
				sendJobRescheduled(chosenTask, hostName, filesToTransfer);
				return;
			}
		}
	
		// Now assign, if possible, one of the pending tasks to the resource
		if (pendingTasks.isEmpty()) {
			if (debug)
				logger.debug("Resource " + hostName + " FREE");
		}
		else {
		  Task chosenTask = assignResourceToBestTask(hostName);
			if (chosenTask != null) {
				if (debug)
					logger.debug("Freed Match: Task(" + chosenTask.getId() + ", "
								 + chosenTask.getMethod().getName() + ") "
								 + "Resource(" + hostName + ")");
				
				List trfSet = trfTimePredictor(chosenTask,hostName);
 	                        List<FileInstanceId> filesToTransfer = (List<FileInstanceId>) trfSet.get(1);

				pendingTasks.remove(chosenTask);
				resManager.reserveResource(hostName);
				sendJob(chosenTask, hostName, filesToTransfer);
			}
			else {
				if (debug)
					logger.debug("Resource " + hostName + " FREE");
			}
		}
	}

	
	// Schedule decision - FIFO
	/*private String assignTaskToBestResource(Task t, List<String> resources) {
		return resources.get(0);
	}*/

	public void addOkSubmit(String host){

           int submits = 0;
	   int ok_submits = 0;

           if(hostToSubmits.get(host) != null){	
              submits = hostToSubmits.get(host);
	      submits++;
	   }
	   else{
		submits = 1;
	   }

	   hostToSubmits.put(host,submits);

           if(hostToOkSubmits.get(host) != null){
              ok_submits = hostToOkSubmits.get(host);
	      ok_submits++;
           }
           else{
		ok_submits = 1;
           }

	   hostToOkSubmits.put(host,ok_submits);

	   float ratio = (float) ok_submits/(float) submits;
           hostToAvailability.put(host,ratio);
	   //if(debug) logger.debug("Adding Submit Ok: "+host+" | Submits OK => "+ok_submits+" Total Submits => "+submits+" Avail Ratio => "+ratio);
	}


	public void addFailedSubmit(String host){

	   int submits = 0;
	   int ok_submits = 0;

           if(hostToSubmits.get(host) != null){
              submits = hostToSubmits.get(host);
              submits++;
           }
           else{
                submits = 1;
           }

	    hostToSubmits.put(host,submits);

           if(hostToOkSubmits.get(host) != null){
              ok_submits = hostToOkSubmits.get(host);
     	   }

	   float ratio = (float) ok_submits/(float) submits;
           hostToAvailability.put(host,ratio);
	    //if(debug) logger.debug("Adding Submit Failure: "+host+" | Submits OK => "+ok_submits+" Total Submits => "+submits+" Avail Ratio => "+ratio);

	}

	public void addHostExTimeMeanValue(String host, float enlapsedTime, int methodId){

        double iTimeStamp = 0;

        if(timeMeasuring) iTimeStamp = System.currentTimeMillis();

        float hostExecMeanTime = 0;
        List hostExecTimes;
        Map<String,List> hostsToExecValues;
        Map<String,Float> hostsToMeanExec;

        if(TaskIdToHostsExecTimeValues.get(methodId) != null){
           hostsToExecValues = TaskIdToHostsExecTimeValues.get(methodId);
        }
        else{
           hostsToExecValues = new THashMap<String,List>();
        }

        if(hostsToExecValues.get(host) != null)
        {
          hostExecTimes = hostsToExecValues.get(host);
        }
        else{
          hostExecTimes = new ArrayList(2);
        }

	if(hostExecTimes.isEmpty()){
          hostExecTimes.add(0,1);
          hostExecTimes.add(1,enlapsedTime);
          hostExecMeanTime = enlapsedTime;
        }
        else{
          Integer nSamples = (Integer) (hostExecTimes.get(0))+1;
          Float accum = (Float) (hostExecTimes.get(1))+enlapsedTime;
          hostExecMeanTime = accum/nSamples;

          hostExecTimes.add(0,nSamples);
          hostExecTimes.add(1,accum);
        }

        hostsToExecValues.put(host,hostExecTimes);
        TaskIdToHostsExecTimeValues.put(methodId,hostsToExecValues);


        if(TaskIdToHostsMeanExecTime.get(methodId) != null){
           hostsToMeanExec = TaskIdToHostsMeanExecTime.get(methodId);
        }
        else{
           hostsToMeanExec = new THashMap<String,Float>();
        }

        hostsToMeanExec.put(host,hostExecMeanTime);
	TaskIdToHostsMeanExecTime.put(methodId,hostsToMeanExec);

        if(timeMeasuring) addExecTStamp+= System.currentTimeMillis() - iTimeStamp;

        if(debug)
          logger.debug("Mean Execution Time of host: " + host + " and Task Id: "+methodId+" is "+hostExecMeanTime+" s");
        }


	public void updateWTCounters(String host, Integer methodId, Integer count){
	   float wTQueue = 0;
	   double iTimeStamp = 0;

	   if(timeMeasuring) iTimeStamp = System.currentTimeMillis();

	   //Calculating wait in queue time
           if(hostToPendingMethodId.get(host) != null){
              Map<Integer,Integer> methodIdRecount = hostToPendingMethodId.get(host);

              Integer methodIdnum = methodIdRecount.get(methodId);
	      methodIdnum+=count;
              methodIdRecount.put(methodId, methodIdnum);
           }
           else{
              Map<Integer,Integer> methodIdRecount = new THashMap<Integer,Integer>();
              methodIdRecount.put(methodId, count);
              hostToPendingMethodId.put(host,methodIdRecount);
           }	

	   //Updating hostToWTQueue
           Map<Integer,Integer> methodIdRecount = hostToPendingMethodId.get(host);
           if(hTaskIdToHostsMeanExecTime.size() != 0 || TaskIdToHostsMeanExecTime.size() != 0){  
             for (Map.Entry<Integer,Integer> e : methodIdRecount.entrySet()){
                   wTQueue+=(e.getValue()*getMeanExecTime(host,e.getKey()));
             }
           } 

	   hostToWTQueue.put(host,wTQueue);	
           if(timeMeasuring) addUpdateWTQTStamp+= System.currentTimeMillis() - iTimeStamp;
	}

	private float getMeanExecTime(String host, Integer methodId){

           float execTime = 0;
	   double iTimeStamp = 0;

	   if(timeMeasuring) iTimeStamp = System.currentTimeMillis();

          //Trying to get real time mean execution time values
             if(TaskIdToHostsMeanExecTime.get(methodId) != null ){
                 if((TaskIdToHostsMeanExecTime.get(methodId)).get(host) != null ){
                     execTime = (TaskIdToHostsMeanExecTime.get(methodId)).get(host);
                 }
                 else{
                  if((hTaskIdToHostsMeanExecTime.get(methodId)) != null){
                   // The chosen resource, have had tasks in last run?
                   if((hTaskIdToHostsMeanExecTime.get(methodId)).get(host) != null ){
                     execTime = (hTaskIdToHostsMeanExecTime.get(methodId)).get(host);
                   }
                  }
                }
             }
             else{
              //Have historical of this method type?
              if((hTaskIdToHostsMeanExecTime.get(methodId)) != null){
                // The chosen resource, have had tasks in last run?
                if((hTaskIdToHostsMeanExecTime.get(methodId)).get(host) != null){
                   execTime = (hTaskIdToHostsMeanExecTime.get(methodId)).get(host);
                }
              }
             }
	   if(timeMeasuring) getExecTime+= System.currentTimeMillis() - iTimeStamp;

          return execTime;
        }

	
        private void calcNewHist(){
             
             logger.info(" ");	
             logger.info("Execution report: ");
             logger.info(" ");
             //logger.info("TaskId to hosts mean wait time in queue: ");

	     //for (Map.Entry<String,Float> e : hostToWTQueue.entrySet()){
             //      logger.info("  Host: " + e.getKey() + " has mean wait in queue time of: " + e.getValue() +" s");
             //}

             //logger.info(" ");
             logger.info("Historical host to availability: ");
             for (Map.Entry<String,Float> e :  hHostToAvailability.entrySet()){
                   logger.info("Host: " + e.getKey()+"| Availability -> "+e.getValue() );
             }

             logger.info(" ");
             logger.info("Total submits: ");
             for (Map.Entry<String,Integer> e :  hostToSubmits.entrySet()){
                   logger.info("Host: " + e.getKey()+" | Number of submits -> "+e.getValue() );
             }

             logger.info(" ");
             logger.info("Total successful submits: ");
             for (Map.Entry<String,Integer> e :  hostToOkSubmits.entrySet()){
                   logger.info("Host: " + e.getKey()+" | Number of ok submits -> "+e.getValue() );
             }


             logger.info(" ");
             logger.info("Run availability ratios: ");
             for (Map.Entry<String,Float> e :  hostToAvailability.entrySet()){
                   logger.info("Host: " + e.getKey()+" | Availability -> "+e.getValue() );
             }


             logger.info(" ");
             logger.info("Hosts used in run: ");
             for (Map.Entry<String,Boolean> e :  hostToUsedInRun.entrySet()){
                   logger.info("Host: " + e.getKey()+" | Used in run? -> "+e.getValue() );
             }
	     logger.info(" ");

	     logger.info(" ");
             logger.info("Excluded resources by errors: ");

	     for(String r: excludedResources){
               logger.info("Excluded resource -> "+r);
            }

            logger.info(" ");

          //Setting availability weights
	  float oldavailWeight = Float.parseFloat(ITConstants.OLDAVAIL_WEIGHT);
          float newavailWeight = Float.parseFloat(ITConstants.NEWAVAIL_WEIGHT);

	  // Adding confidence to non-used and penalted workers
           addConfidence();

	  //Storing availability to historical
	   for (Map.Entry<String,Float> e : hHostToAvailability.entrySet()){
	     if(hostToAvailability.get(e.getKey()) != null){
		 float runValue = hostToAvailability.get(e.getKey());
		 float oldValue = hHostToAvailability.get(e.getKey());
		 float newValue = (newavailWeight*runValue) + (oldavailWeight*oldValue); //Weighted mean
		 hHostToAvailability.put(e.getKey(),newValue);
	     }
	   }

	   // Writting new historical availability values
            logger.info("Storing availability statistics to historical file");

            histManager.setAvailability(hHostToAvailability);

	     //Reloading historical file
              try {
                 histManager = new HistoricalManager();
              }
              catch (Exception e) {
                 logger.error(HIST_LOAD_ERR, e);
              }
	     
         //Storing MeanExecTime historical
         //If we have not historical..
         if(hTaskIdToHostsMeanExecTime.size() == 0 ){
             hTaskIdToHostsMeanExecTime = TaskIdToHostsMeanExecTime;
         }
         else{

            for (Map.Entry<Integer,Map<String,Float>> e : hTaskIdToHostsMeanExecTime.entrySet()){
                     Map<String,Float> hHostsMeanExecTime = e.getValue();

                     if(TaskIdToHostsMeanExecTime.get(e.getKey()) != null){
                        Map<String,Float> HostsMeanExecTime = TaskIdToHostsMeanExecTime.get(e.getKey());

                        //Updating values of hosts that exist in historical
                        for (Map.Entry<String,Float> o : hHostsMeanExecTime.entrySet()){

                                if(HostsMeanExecTime.get(o.getKey()) != null){
                                   float newValue = HostsMeanExecTime.get(o.getKey());

                                   hHostsMeanExecTime.put(o.getKey(),newValue);
                                   HostsMeanExecTime.remove(o.getKey());
                                }
                        }
                        //Adding the new ones
                        for (Map.Entry<String,Float> s : HostsMeanExecTime.entrySet()){
                            hHostsMeanExecTime.put(s.getKey(),s.getValue());
                        }

                        hTaskIdToHostsMeanExecTime.put(e.getKey(),hHostsMeanExecTime);
                     }
             }//End for

        //Adding some new task id queues that aren't in historical.
             for (Map.Entry<Integer,Map<String,Float>> e : TaskIdToHostsMeanExecTime.entrySet()){
               if(hTaskIdToHostsMeanExecTime.get(e.getKey()) == null){
                  Map<String,Float> HostsMeanExecTime = e.getValue();
                  hTaskIdToHostsMeanExecTime.put(e.getKey(),HostsMeanExecTime);
               }
             }
         } //End else

	 // Writting new execution time values on historical file
         if(hTaskIdToHostsMeanExecTime.size() != 0){
               logger.info("Storing execution time to historical file");
               histManager.setMeanExecTime(hTaskIdToHostsMeanExecTime);
         }
	    
	 //Debug
         if(timeMeasuring){
	   logger.info(" ");
           logger.info("Debug Run Time of functions (in seconds) :");
	   logger.info("Total time on assignTasktoBestResource -> "+assignTStamp/1000);
	   logger.info("Total time on trfTimePredictor -> "+timePredTStamp/1000);
	   logger.info("Total time on addHostWQueueValue -> "+addQueueTStamp/1000);
           logger.info("Total time on addHostExecValue -> "+addExecTStamp/1000);
           logger.info("Total time on addUpdateWTQTStamp -> "+addUpdateWTQTStamp/1000);
           logger.info("Total time on getExecTime -> "+getExecTime/1000);
           logger.info(" ");
         }

	 logger.info("Has locations defined -> "+locationsDefined);
         logger.info(" ");


      }

	// Comparator sort pattern
        static final Comparator<Map.Entry<String,Float>> MINMAJOR_PATTERN = new Comparator<Map.Entry<String,Float>>()
        {
           public int compare(Map.Entry<String,Float> e1, Map.Entry<String,Float> e2) {
              return e1.getValue().compareTo(e2.getValue());
           }
        };

	// Comparator sort pattern
        static final Comparator<Map.Entry<String,Float>> MAJORMIN_PATTERN = new Comparator<Map.Entry<String,Float>>()
        {
           public int compare(Map.Entry<String,Float> e1, Map.Entry<String,Float> e2){
              return e2.getValue().compareTo(e1.getValue());
           }
        };

       // Scheduler evaluation function
       private List assignTaskToBestResource(Task t, List<String> resources) {
 
        double iTimeStamp = 0;
        
        if(timeMeasuring) iTimeStamp = System.currentTimeMillis();

	String bestResource = null;
	float bestRank = 0;
	int slotsNum = 0;
	
	Map<String,Float> hostToFinalRank = new THashMap<String,Float>(resources.size());
	Map<String,Float> hostToWaitTime = new THashMap<String,Float>(resources.size());

	List<Map.Entry<String,Float>> hostToWait = null;

        Map<Float,Integer>  valueToRank = new THashMap<Float,Integer>();
	Map<String,Integer> hostToWRank = new THashMap<String,Integer>();
	
        List<Map.Entry<String,Float>> hostToSortedRanks = new ArrayList<Map.Entry<String,Float>>();

	//Files that will be transfered in case of choose this resource as the best
	Map<String,List<FileInstanceId>> resToTrfFiles = new THashMap<String,List<FileInstanceId>>();

	//The fuction return object
        List returnSet = new ArrayList(2);

	//Random generator in case of draw in result
	Random randGen = new Random(System.currentTimeMillis());	
        if(debug){
	 logger.debug(" ");
	 logger.debug(" ");
        }

	for(String res : resources){
	    float wTQueueScore = 0;
		
	   //Calculating wTQueueRankScore
	    if(hostToWTQueue.get(res) != null){
	      wTQueueScore = hostToWTQueue.get(res);
	    }

	  //Calculating timePrediction
	  //Predicting transfer time in case of have to transfer files to the resource
	   List trfSet = trfTimePredictor(t,res); 

           float timePrediction = (Float) trfSet.get(0);
           List<FileInstanceId> filesToTransfer = (List<FileInstanceId>) trfSet.get(1);

	   resToTrfFiles.put(res,filesToTransfer);

	 //Calculating PerformanceScore value
	     //Calculating execTime
	      float execTime = 0;
	
             //Trying to get real time mean execution time values
             if(TaskIdToHostsMeanExecTime.get(t.getMethod().getId()) != null ){
                 if((TaskIdToHostsMeanExecTime.get(t.getMethod().getId())).get(res) != null ){
                     execTime = (TaskIdToHostsMeanExecTime.get(t.getMethod().getId())).get(res);
                    //if(debug) logger.debug("Using Real Execution Time Values");
                 }
                 else{
                  if((hTaskIdToHostsMeanExecTime.get(t.getMethod().getId())) != null){
                   // The chosen resource, have had tasks in last run?
                   if((hTaskIdToHostsMeanExecTime.get(t.getMethod().getId())).get(res) != null ){
                     execTime = (hTaskIdToHostsMeanExecTime.get(t.getMethod().getId())).get(res);
                     //if(debug) logger.debug("Using Historical Execution Time Values");
                   }
                  }
                }
             }
             else{
              //Have historical of this task type?
              if((hTaskIdToHostsMeanExecTime.get(t.getMethod().getId())) != null){
                // The chosen resource, have had tasks in last run?
                if((hTaskIdToHostsMeanExecTime.get(t.getMethod().getId())).get(res) != null ){
                   execTime = (hTaskIdToHostsMeanExecTime.get(t.getMethod().getId())).get(res);
                   //if(debug)logger.debug("Using Historical Execution Time Values");
                }
              }
             }

	 if(debug) logger.debug("Resource: "+res+" WTQueue -> "+wTQueueScore+" TransferTime -> "+timePrediction+" PerfScore -> "+execTime);

	 hostToWaitTime.put(res,(wTQueueScore+timePrediction+execTime));

	}//End for

         if(debug) logger.debug("");

	//Copying to Arraylist structure
	hostToWait = new ArrayList<Map.Entry<String,Float>>(hostToWaitTime.size());
	hostToWait.addAll(hostToWaitTime.entrySet());

        //Sorting the scores
        Collections.sort(hostToWait, MAJORMIN_PATTERN);

	int wRank = 1;

	//Assigning rank to each score
        for (int i = 0; i < hostToWait.size(); i++ ) { 
         if(valueToRank.get(hostToWait.get(i).getValue()) == null){
           valueToRank.put(hostToWait.get(i).getValue(),wRank);
           wRank++;
         }
        }

        for (int i = 0; i < hostToWait.size(); i++ ) {
          int rank = valueToRank.get(hostToWait.get(i).getValue());
          hostToWRank.put(hostToWait.get(i).getKey(),rank);
        }
	
	//Cleaning data structures
	valueToRank.clear();
	hostToWaitTime.clear();
	hostToWait.clear();
	
	//Calculating rank
	for(String res : resources){

	 int hostWRankPos = 0;
	 float hostAvailRate = hHostToAvailability.get(res);

	 if(hostToWRank.get(res) != null){
	   hostWRankPos = hostToWRank.get(res);
	 }

	    //Final rank evaluation formula
            float rank = ((float) hostWRankPos*hostAvailRate);
	    hostToFinalRank.put(res,rank);

	   if(debug) logger.debug("Host " + res + " | TaskId -> "+t.getMethod().getId()+" | Rank -> "+hostWRankPos+" | Avail Rate "+hostAvailRate);
        }

	hostToSortedRanks.addAll(hostToFinalRank.entrySet());

        //Sorting final ranks
	Collections.sort(hostToSortedRanks, MAJORMIN_PATTERN);

	hostToFinalRank.clear();

	//If resources in draw, use random choose to select the best resource.
	if(hostToSortedRanks.size() > 1){
	  List<String> drawResources = new LinkedList<String>();
	  float v = hostToSortedRanks.get(0).getValue();
	  drawResources.add(hostToSortedRanks.get(0).getKey());

          for(int i = 1; i < hostToSortedRanks.size();i++){
	   if(v == hostToSortedRanks.get(i).getValue()){
	     drawResources.add(hostToSortedRanks.get(i).getKey());
	     if(debug) logger.debug("Drawn resources, using random choose");
	   }
	 }

	 if(drawResources.size() > 1){
           int randIndex = randGen.nextInt(drawResources.size());
           bestResource = drawResources.get(randIndex);
           bestRank = hostToSortedRanks.get(0).getValue();
         }
         else{
          bestResource = hostToSortedRanks.get(0).getKey();
          bestRank = hostToSortedRanks.get(0).getValue();
         }
       } 
       else{
          bestResource = hostToSortedRanks.get(0).getKey();
          bestRank = hostToSortedRanks.get(0).getValue();
       } 


	if(debug){
	    logger.debug("");
            logger.debug("TaskId "+t.getMethod().getId()+" | BestResource " +bestResource + " | Best rank -> " + bestRank +" ");
      
            logger.debug(" ");
            logger.debug(" ");
        }

	returnSet.add(0,bestResource);
	returnSet.add(1,resToTrfFiles.get(bestResource));

        if(timeMeasuring) assignTStamp += System.currentTimeMillis() - iTimeStamp;
  
        return returnSet;
   }


	// Schedule decision - number of files
       //TODO: Reduce iterations number, trying, for instance, the 5 first tasks
	private Task assignResourceToBestTask(String resourceName) {
		Task bestTask = null;
		int bestScore = 0;
		
		// Find best scoring task whose constraints are fulfilled by the resource
		for (Task t : pendingTasks) {
			String constraints;
			try {
				constraints = constrManager.getConstraintsPresched(t);
			}
			catch (NoSuchMethodException e) {
				logger.error(CONSTR_LOAD_ERR, e);
				errorReport.throwError(ITConstants.TS, CONSTR_LOAD_ERR, e);
				return null;
			}
			boolean matches = resManager.matches(resourceName, constraints);
			if (matches) {
				int score = 0;
				Parameter[] params = t.getMethod().getParameters();
				if (bestTask == null) bestTask = t;
				for (Parameter p : params) {
					if (p instanceof FileParameter) {
						FileParameter fp = (FileParameter)p;
						FileInstanceId fId = null;
						switch (fp.getDirection()) {
							case IN:
								RAccessId raId = (RAccessId)fp.getFileAccessId();
								fId = raId.getReadFileInstance();
								break;
							case INOUT:
								RWAccessId rwaId = (RWAccessId)fp.getFileAccessId();
								fId = rwaId.getReadFileInstance();
								break;
							case OUT:
								break;
						}
						
						if (fId != null) {;
							List<Location> locs = getLocations(fId);

							 for(int i=0; i < locs.size();i++){
								String host = locs.get(i).getHost();
								if (host.equals(resourceName)) {
									score++;
									break;
								}
							 }
						}
					}
				}
				if (score > bestScore) {
					bestScore = score;
					bestTask = t;
				}
			}
		}
		
		if (debug) logger.debug("Best scoring task is #" + bestTask.getId() + " with score " + bestScore);
		return bestTask;
	}
	

	private Task assignRescheduledTask(String hostName) {
		for (Task t : tasksToReschedule) {
			String constraints;
			try {				
				constraints = constrManager.getConstraintsPresched(t);
			}
			catch (NoSuchMethodException e) {
				logger.error(CONSTR_LOAD_ERR, e);
				errorReport.throwError(ITConstants.TS, CONSTR_LOAD_ERR, e);
				continue;
			}
			boolean matches = resManager.matches(hostName, constraints);
			if (matches) {
				// Now we must ensure that the freed host is not the one where the task failed to run
				int taskId = t.getId();
				int oldJobId = taskToJob.get(taskId);
				String failedHost = jobToHost.get(oldJobId);
				if (failedHost.equals(hostName))
					continue;
				else {
					taskToJob.remove(taskId);
					jobToHost.remove(oldJobId);
					return t;
				}
			}
		}
		return null;
	}


       private void addConfidence(){

	  float confidence_step = Float.parseFloat(ITConstants.CONFIDENCE_INC_STEP);

	   logger.info("Adding confidence to non-used workers:");
	   logger.info(" ");

          for (Map.Entry<String,Boolean> e :  hostToUsedInRun.entrySet()){
              String host = e.getKey();
	      Boolean used = e.getValue();           
	      if(!used){	      
                float avail_rate = hHostToAvailability.get(host);
	        if(avail_rate < 1.0){
	          if((avail_rate+confidence_step) <= 1.0){
	           avail_rate = avail_rate+confidence_step;
                  }
	        else{
	          avail_rate = (float) 1.0;	
                }
                 //Updating host to availability map
	         hHostToAvailability.put(host,avail_rate);
	       }
	      }	
           }
       }
      
       public Map<String,Integer> getHostToRetry(){
	 return hostToRetry;
       }

       public void addLocation(FileInstanceId fId,
                                                        String newHost,
                                                        String newPath) {

                FileInfo info = fIdToFileInfo.get(fId.getFileId());
                info.addLocationForVersion(fId.getVersionId(), newHost, newPath);
       }

       private void removeLocation(FileInstanceId fId,
                                                           String oldHost,
                                                           String oldPath) {

                FileInfo info = fIdToFileInfo.get(fId.getFileId());
                info.removeLocationForVersion(fId.getVersionId(), oldHost, oldPath);
       }
	

       public List<Location> getLocations(FileInstanceId fId) {

	    List<Location> locs = null;

            if(fIdToFileInfo.get(fId.getFileId()) == null){
                locs = fileLocation.getLocations(fId);
            }
            else{
              FileInfo info = fIdToFileInfo.get(fId.getFileId());
              locs = info.getLocationsForVersion(fId.getVersionId());
	    }

	    return locs;
       }


       public void addSize(FileInstanceId fId,List<Long> sizeModDate){

                FileInfo info = fIdToFileInfo.get(fId.getFileId());       
		long size = sizeModDate.get(0);
                long lastMod = sizeModDate.get(1);
                info.addSizeForVersion(fId.getVersionId(),size);
                info.addLastModForVersion(fId.getVersionId(),lastMod);
       }


       private long getSize(FileInstanceId fId){

  	    long size = 0;	

	    if(fIdToFileInfo.get(fId.getFileId()) == null){
                size = fileLocation.getSize(fId);
	    }
	    else{
              FileInfo info = fIdToFileInfo.get(fId.getFileId());
              size =info.getSizeForVersion(fId.getVersionId());
	    } 
   
           return size;
       }


       public void addNewVersion(FileInstanceId fId){
         FileInfo info = fIdToFileInfo.get(fId.getFileId());
	 info.addVersion();
       }
	
       public void setFileInformation(FileInfo fileInfo){
        fIdToFileInfo.put(fileInfo.getFileId(),fileInfo);
       }

       public void resetExecTimeHistorical(){
           if(debug) logger.debug("Cleaning execution historical time due change in some files.");
           hTaskIdToHostsMeanExecTime.clear();
       }
	
       private List trfTimePredictor(Task t, String trfChosenResource){

           float tTimePrediction = 0;
           boolean fileExist = false;
	   boolean in = false;
           FileInstanceId fId = null;
           List<Location> locs = null;
	   List returnSet = new ArrayList(2);
	   List<FileInstanceId> filesToTransfer = new ArrayList<FileInstanceId>();
           double iTimeStamp = 0;
           double locTStamp = 0;
           double i2TimeStamp = 0;

           //Setting target of file transference
           String target = trfChosenResource;

	   //Getting speed matrix
           hostToSpeedMatrix = newJob.getSpeedMatrix();
	   
           String workingDir = projManager.getProperty(trfChosenResource, ITConstants.WORKING_DIR);
	   List<String> workers = projManager.getWorkers();
           String user = projManager.getProperty(trfChosenResource, ITConstants.USER);
           Parameter[] params = t.getMethod().getParameters();

           if(timeMeasuring)  iTimeStamp = System.currentTimeMillis();

	   //Predicting transference time
            for (Parameter p : params) {

                     //Resetting fileExist flag
                     fileExist = false;
		     in = false;

                        switch(p.getType()) {
                                case FILE_T:
                                        FileParameter fp = (FileParameter)p;
                                       // if (debug)
                                       //         logger.debug("    * " + fp);

                                        //Checking if the transfer could be avoided
                                          fId = null;

                                        switch (fp.getDirection()) {
                                          case IN:
                                                RAccessId raId = (RAccessId)fp.getFileAccessId();
                                                fId = raId.getReadFileInstance();
						in = true;
						break;
                                          case INOUT:
                                                RWAccessId rwaId = (RWAccessId)fp.getFileAccessId();
					        fId = rwaId.getReadFileInstance();
			                        break;
                                          case OUT:
                                                break;
                                       }
					
					//Asking for file locations
					  if(timeMeasuring) locTStamp = System.currentTimeMillis();

					 if(fId != null){ 
 					   locs = getLocations(fId);
				   }
			                //If we have locations defined will use FIP information
					if(locationsDefined && in){                        
					  for(int i=0; i < locs.size();i++){   					
						String host = locs.get(i).getHost();
						String path = locs.get(i).getPath();
	
					        //if(debug) logger.debug("Fitxer: "+fp.getName()+" Location -> host: "+host+" path: "+path);
					 
                                                if(host.equals(trfChosenResource) && path.equals(workingDir)) {
                                                  fileExist = true;
                                                  break;
                                                }					    
                                          }
					} //End if	

                                         //If the file doesn't exist...
                                         if(!fileExist && (fId != null)){				    
					  filesToTransfer.add(fId);					   

                                           //Transfer time estimation per each transfer
			                   long fileLength = getSize(fId);

                                                 if(fileLength != 0) {
				       	           //Setting source of file transference
						    String source = null;

						    if(locationsDefined){
						     source = locs.get(0).getHost();
						    }
						    else{
						     source = fp.getHost();
						    }

					            //Getting link speed from speed matrix
						    netSpeed = (hostToSpeedMatrix.get(source)).get(target);
					                       
                                                    if(debug){
                                                     //logger.debug("Source File Name is: " +fp.getName()+ " Source File Size: "+ fileLength+" Time prediction: "+tTimePrediction);
						     //logger.debug("TimePrediction -> Source: "+source+" Target: "+target+" | NetSpeed: "+netSpeed);
                                                     //logger.debug(" ");
						     }					    
                                                    tTimePrediction = tTimePrediction + ((float)(fileLength*8)/(float)(netSpeed*1000000)); //Prediction in seconds
                                                 }
                                        }//End file doesn't exist

                                        break;

                                default: // Basic types (including String)
                                        //if (debug) logger.debug("    * " + (BasicTypeParameter)p);
                                        break;
                        }//End switch
            }//End for parameters

        returnSet.add(0,tTimePrediction);
	returnSet.add(1,filesToTransfer);

        if(timeMeasuring)  timePredTStamp+= System.currentTimeMillis() - iTimeStamp;        

     return returnSet;   
} //End trfTimePredictor


	// Controller interfaces implementation
	
	// Binding controller interface
	
	public String[] listFc() {
	    return new String[] { "NewJob", "TaskStatus" , "FileLocation", "ErrorReport" };
	}

	public Object lookupFc(final String cItf) {
		if (cItf.equals("NewJob")) {
			return newJob;
	    }
		else if (cItf.equals("TaskStatus")) {
			return taskStatus;
	    }
		else if (cItf.equals("FileLocation")) {
			return fileLocation;
	    }
		else if (cItf.equals("ErrorReport")) {
			return errorReport;
	    }
	    return null;
	}

	public void bindFc(final String cItf, final Object sItf) {
		if (cItf.equals("NewJob")) {
			newJob = (JobCreation)sItf;
	    }
		else if (cItf.equals("TaskStatus")) {
			taskStatus = (TaskStatus)sItf;
	    }
		else if (cItf.equals("FileLocation")) {
			fileLocation = (FileInformation)sItf;
	    }
		else if (cItf.equals("ErrorReport")) {
			errorReport = (ITError)sItf;
	    }
	}

	public void unbindFc(final String cItf) {
	    if (cItf.equals("NewJob")) {
	    	newJob = null;
	    }
	    else if (cItf.equals("TaskStatus")) {
	    	taskStatus = null;
		}
	    else if (cItf.equals("FileLocation")) {
	    	fileLocation = null;
		}
	    else if (cItf.equals("ErrorReport")) {
	    	errorReport = null;
		}
	}
	
}
