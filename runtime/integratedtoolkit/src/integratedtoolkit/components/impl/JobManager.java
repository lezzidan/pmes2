
package integratedtoolkit.components.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Collections;

import gnu.trove.map.hash.*;

import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import org.gridlab.gat.GAT;
import org.gridlab.gat.GATContext;
import org.gridlab.gat.GATInvocationException;
import org.gridlab.gat.URI;
import org.gridlab.gat.io.File;
import org.gridlab.gat.monitoring.Metric;
import org.gridlab.gat.monitoring.MetricDefinition;
import org.gridlab.gat.monitoring.MetricEvent;
import org.gridlab.gat.monitoring.MetricListener;
import org.gridlab.gat.resources.HardwareResourceDescription;
import org.gridlab.gat.resources.Job;
import org.gridlab.gat.resources.Job.JobState;
import org.gridlab.gat.resources.JobDescription;
import org.gridlab.gat.resources.ResourceBroker;
import org.gridlab.gat.resources.ResourceDescription;
import org.gridlab.gat.resources.SoftwareDescription;

import org.objectweb.fractal.api.control.BindingController;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.RunActive;
import org.objectweb.proactive.Service;
import org.objectweb.proactive.api.PAFuture;
import org.objectweb.proactive.core.util.wrapper.StringWrapper;

import integratedtoolkit.ITConstants;
import integratedtoolkit.api.ITExecution.*;
import integratedtoolkit.components.FileInfoUpdate;
import integratedtoolkit.components.FileInformation;
import integratedtoolkit.components.FileTransfer;
import integratedtoolkit.components.JobCreation;
import integratedtoolkit.components.JobStatus;
import integratedtoolkit.components.Preparation;
import integratedtoolkit.components.Schedule;
import integratedtoolkit.components.TransferStatus;
import integratedtoolkit.components.JobStatus.JobEndStatus;
import integratedtoolkit.interfaces.ITError;
import integratedtoolkit.log.Loggers;
import integratedtoolkit.types.ExecutionParams;
import integratedtoolkit.types.JobInfo;
import integratedtoolkit.types.JobInfo.JobHistory;
import integratedtoolkit.types.Method;
import integratedtoolkit.types.Parameter;
import integratedtoolkit.types.Parameter.*;
import integratedtoolkit.types.file.Location;
import integratedtoolkit.types.file.FileAccessId;
import integratedtoolkit.types.file.FileAccessId.*;
import integratedtoolkit.types.file.FileInstanceId;
import integratedtoolkit.util.ProjectManager;
import integratedtoolkit.util.ResourceManager;
import integratedtoolkit.util.HistoricalManager;
import integratedtoolkit.util.RequestDispatcher;
import integratedtoolkit.util.RequestQueue;
import integratedtoolkit.util.ThreadPool;


public class JobManager implements JobCreation, TransferStatus, Preparation,
								   BindingController,
								   RunActive,
								   MetricListener {
	
	// Constants definition
	private static final int    POOL_SIZE 			  	  = 1;
	private static final String POOL_NAME 			  	  = "JM";
	private static final String WORKER_SCRIPT_JAVA        = "worker.sh";
	private static final String WORKER_BINARY_C           = "workerGS";
	private static final String ANY_PROT           		  = "any://";
	private static final String RES_ATTR           		  = "machine.node";
	private static final String JOB_STATUS		          = "job.status";
	private static final String JOB_OK	                  = "Job finished successfully";
	private static final String JOB_PREPARATION_ERR 	  = "Error preparing job";
	private static final String JOB_SUBMISSION_ERR 		  = "Error submitting job";
	private static final String JOB_EXECUTION_ERR 		  = "Error executing job";
	private static final String THREAD_POOL_ERR  	 	  = "Error starting pool of threads";
	private static final String RB_CREATION_ERR 	 	  = "Error creating resource broker";
	private static final String CALLBACK_PROCESSING_ERR   = "Error processing callback for job";
	private static final String STAGING_ERR				  = "Error staging in job files";
	private static final String TERM_ERR				  = "Error terminating";
	private static final String CLEAN_JOB_ERR 		 	  = "Error running clean job";
	private static final String PROJ_LOAD_ERR			  = "Error loading project information";
	private static final String RES_LOAD_ERR        		  = "Error loading resource information";
	private static final String HIST_LOAD_ERR       ="Error loading historical information";
	
	// Client interfaces
	private JobStatus jobStatus;
	private Schedule reschedule;
	private FileTransfer newTransfer;
	private FileInfoUpdate newVersion;
	private FileInformation newLocation;
	private ITError errorReport;
	
	// Map : job identifier -> job information
	private Map<Integer,JobInfo> jobToInfo;
	
	// Map : requested transfers identifier -> job information
	private Map<Integer,Integer> transferToJob;
	
	// GAT context
	private GATContext context;
	
	// GAT broker adaptor information
	private boolean usingGlobus;
	private boolean userNeeded;
	
	// Pool of worker threads and queue of requests
	private ThreadPool pool;
	private RequestQueue<Integer> queue;
	
	// Component logger - No need to configure, ProActive does
	private static final Logger logger = Logger.getLogger(Loggers.JM_COMP);
	private static final boolean debug = logger.isDebugEnabled();
	private static final String workerDebug = Boolean.toString(Logger.getLogger(Loggers.WORKER).isDebugEnabled());
	
	// SLA
	private static final boolean slaEnabled = System.getProperty(ITConstants.IT_SLA_ENABLED) != null
	  										  && System.getProperty(ITConstants.IT_SLA_ENABLED).equals("true")
	  										  ? true : false;
	//ProjectManager
	private ProjectManager projManager;

	//ResourceManager
	private ResourceManager resManager;

        //HistoricalManager
        private HistoricalManager histManager;

	// Preschedule
	private Map<String,Integer> hostToSlots; // host name -> Number of free processors
	private Map<String,List<Integer>> hostToPending; // host name -> List of pending prescheduled jobs

	private Map<Integer,Map<Integer,Long>> TaskIdTojobInSStart; //TaskId -> Timestamp of the instant when job Starts

	//Speed matrix structures
        private Map<String,Map<String,Float>> hostToSpeedMatrix;

	//Map: host -> allowed retries
	private Map<String,Integer> hostToRetry;  //Map: host -> reschedule attemps
	private Map<String,Boolean> hostToUsedInRun; //Map: host -> Used in Run?

        //Net speed in Mbps
	float netSpeed = 0;

	//Available locations?
	private boolean locationsDefined = false;

        //Performance debug
         private boolean  timeMeasuring = false;
         private double timeFileTrfInfo = 0;
         private double timeCheckPend = 0;
         private double timeCalcNetSpeed = 0;
         private double timeGetSize = 0;
	 private double orderTrfTime = 0;

	// Language
	private static final boolean isJava = System.getProperty(ITConstants.IT_LANG) != null
    									  && System.getProperty(ITConstants.IT_LANG).equals("java")
    									  ? true : false;

	// Worker script
	private static String workerScript;

		
	public JobManager() { }
	
	
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
			String brokerAdaptor = System.getProperty(ITConstants.GAT_BROKER_ADAPTOR),
			       fileAdaptor   = System.getProperty(ITConstants.GAT_FILE_ADAPTOR);
			context.addPreference("ResourceBroker.adaptor.name", brokerAdaptor);
			context.addPreference("File.adaptor.name", fileAdaptor + ", local");
			usingGlobus = brokerAdaptor.equalsIgnoreCase("globus");
			userNeeded = brokerAdaptor.regionMatches(true, 0, "ssh", 0, 3);
		}
			
		JobInfo.init();
		
		/* We need to synchronize this map, since it can be accessed by the
		 * job dispatcher thread or by the callback notifier thread from GAT
		 */
		if (jobToInfo == null)
			jobToInfo = Collections.synchronizedMap(new TreeMap<Integer,JobInfo>());
		else
			jobToInfo.clear();
		
		// No need to synchronize this map
		if (transferToJob == null)
			transferToJob = new TreeMap<Integer,Integer>();
		else
			transferToJob.clear();
		
		// Create thread that will handle job submission requests
		if (queue == null)
			queue = new RequestQueue<Integer>();
		else
			queue.clear();
		
		if (pool == null) {
			pool = new ThreadPool(POOL_SIZE, POOL_NAME, new JobDispatcher(queue, this));
			try {
				pool.startThreads();
			}
			catch (Exception e) {
				logger.error(THREAD_POOL_ERR, e);
				return new StringWrapper(ITConstants.JM + ": " + THREAD_POOL_ERR);
			}
		}

		if (projManager == null) {			
		  try {
		  	projManager = new ProjectManager();
                  }
		  catch (Exception e) {
			logger.error(PROJ_LOAD_ERR, e);
                        return new StringWrapper(ITConstants.JM + ": " + PROJ_LOAD_ERR);
		  }
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
			
			List<String> hosts = projManager.getWorkers();
			int numHosts = hosts.size();

			if(hostToSlots == null){			
			  hostToSlots = new THashMap<String,Integer>(numHosts + numHosts / 2 + 1);
			}
			else{
			  hostToSlots.clear();
			}

			if(hostToPending == null){	
			  hostToPending = new THashMap<String,List<Integer>>(numHosts + numHosts / 2 + 1);
			}
			else{
			  hostToPending.clear();
			}

			if( hostToRetry == null)
	                  hostToRetry = new THashMap<String,Integer>();
        	        else
                          hostToRetry.clear();

			if( hostToUsedInRun == null)
                          hostToUsedInRun = new THashMap<String,Boolean>();
                	else
                          hostToUsedInRun.clear();

			if(TaskIdTojobInSStart == null){
                           TaskIdTojobInSStart = new THashMap<Integer,Map<Integer,Long>>();
                        }
                        else{
                           TaskIdTojobInSStart.clear();
                        }

			if(hostToSpeedMatrix == null)
                         hostToSpeedMatrix = new THashMap<String,Map<String,Float>>();
                        else
                         hostToSpeedMatrix.clear();
			
			//Setting initial values of speed matrix
                        hostToSpeedMatrix = histManager.getSpeedMatrix();
                        
                        if(debug) {
                         logger.debug(" ");
                         logger.debug("hostToSpeedMatrix: ");
                         for (Map.Entry<String,Map<String,Float>> e : hostToSpeedMatrix.entrySet()){
                                logger.debug(" Link Src: " + e.getKey());
                                Map<String,Float> m = e.getValue();

                                 for (Map.Entry<String,Float> o : m.entrySet()){
                                logger.debug("  Destination Host: " + o.getKey() + " | Speed -> " + o.getValue() +" Mbps");
                                }
                         }
                        }

			//Setting locations availability flag
			locationsDefined = projManager.hasLocations();

                        //Setting hostToSlots and hostToPending maps
			for (String host : hosts) {
				int slots = Integer.parseInt(resManager.getResCPUCount(host));
				hostToSlots.put(host, slots);
				hostToPending.put(host, new ArrayList<Integer>(slots));
			}

			 // Setting resources
                        List<String> allResources = projManager.getWorkers();

			//Setting hostToRetry map
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

		workerScript = isJava ? WORKER_SCRIPT_JAVA : WORKER_BINARY_C;

		logger.info("Initialization finished");

		return new StringWrapper(ITConstants.INIT_OK);
	}
	
	public void cleanup() {
		// Make pool threads finish
		try {
			pool.stopThreads();
		}
		catch (Exception e) {
	           // Ignore, we are terminating
		}
		
		// Cancel all submitted jobs
		synchronized(jobToInfo) {
			for (JobInfo jobInfo : jobToInfo.values()) {
				Job gatJob;
				if ((gatJob = jobInfo.getGATJob()) != null) {
					try {
						MetricDefinition md = gatJob.getMetricDefinitionByName(JOB_STATUS);
						Metric m = md.createMetric();
						gatJob.removeMetricListener(this, m);
						gatJob.stop();
					}
					catch (GATInvocationException e) {
						logger.error(TERM_ERR, e);
					}
				}
			}
		}

		//Storing updated speed matrix to historical file
	      	calcMeanNetSpeed();
		GAT.end();
		logger.info("Cleanup done");
	}


	// JobCreation interface
	
	public int newJob(Method method, ExecutionParams execParams, List<FileInstanceId> filesToTransf) {
		// Store the information of the job
		JobInfo jobInfo = new JobInfo(method, execParams);
		int jobId = jobInfo.getJobId();
		jobToInfo.put(jobId, jobInfo);	

		String host = execParams.getHost();
		
		if (debug) {
			logger.debug("New Job (" + jobId + ")");
			logger.debug("  * Method name: " + method.getName());
			logger.debug("  * Target host: " + host);
		}
		
		orderTransfers(jobId,
					   method.getParameters(),
					   new Location(host, execParams.getWorkingDir()), filesToTransf);
	
		//Updating used in run map
                hostToUsedInRun.put(host,true);

		return jobId;
	}

	
	public int newCleanJob(URI[] cleanScripts, String[] cleanParams) {
		if(debug) logger.debug("New clean job, must run clean scripts:");
		
		RequestQueue<SoftwareDescription> sdQueue = new RequestQueue<SoftwareDescription>();
		RequestQueue<Job> jobQueue = new RequestQueue<Job>();
		ThreadPool pool = new ThreadPool(POOL_SIZE, POOL_NAME, new Cleaner(sdQueue, jobQueue));
		try {
			pool.startThreads();
		}
		catch (Exception e) {
			logger.error(THREAD_POOL_ERR, e);
			return 1;
		}
		
		for (int i = 0; i < cleanScripts.length; i++) {
			URI script = cleanScripts[i];
			String cleanParam = cleanParams[i];
			
			if(debug) logger.debug( "Clean call: " + script + " " + cleanParam);
			
			try {
				if (!userNeeded && script.getUserInfo() != null) // Remove user from the URI
					script.setUserInfo(null);
				String user = script.getUserInfo();
				if (user == null) user = "";
				else			  user += "@";
				
				SoftwareDescription sd = new SoftwareDescription();
				sd.addAttribute("uri", ANY_PROT + user + script.getHost());
		        sd.setExecutable(script.getPath());
		        sd.setArguments(new String[] { cleanParam });
		        
		        sdQueue.enqueue(sd);
			}
			catch (Exception e) {
				logger.error(CLEAN_JOB_ERR, e);
				errorReport.throwError(ITConstants.JM, CLEAN_JOB_ERR, e);
				return -1;
			}
		}
		
		// Poll for completion of the clean jobs
		int numJobs = cleanScripts.length;
		while (numJobs > 0) {
			Job job = jobQueue.dequeue();
			if (job.getState() == JobState.STOPPED) {
				if(debug) logger.debug("Job finished " + job.getJobDescription());
				numJobs--;
			}
			else if (job.getState() == JobState.SUBMISSION_ERROR) {
				logger.error(CLEAN_JOB_ERR + ": " + job);
				numJobs--;
			}
			else {
				jobQueue.enqueue(job);
				try { Thread.sleep(500); } catch (Exception e) { }
			}
		}
		
		try {
			pool.stopThreads();
		}
		catch (Exception e) { /* Ignore, we are terminating */ }
		
		// Value for synchronization
		return 0;
	}
	
	
	public int jobRescheduled(Method method, ExecutionParams newExecParams, List<FileInstanceId> filesToTransf) {

		// Store the information of the job
		JobInfo jobInfo = new JobInfo(method, newExecParams);
		int jobId = jobInfo.getJobId();
		jobToInfo.put(jobId, jobInfo);
		jobInfo.setHistory(JobHistory.RESCHEDULED);
		
		String host = newExecParams.getHost();
		
		if (debug) {
			logger.debug("Rescheduled Job (" + jobId + ")");
			logger.debug("  * Method name: " + method.getName());
			logger.debug("  * Target host: " + host);
		}
		
		orderTransfers(jobId,
					   method.getParameters(),
					   new Location(host, newExecParams.getWorkingDir()), filesToTransf);
		
		return jobId;
	}

	
	private void orderTransfers(Integer jobId, Parameter[] params, Location fileLocation, List<FileInstanceId> filesToTransf) {

	       double iTimeStamp =0;
               if(timeMeasuring) iTimeStamp = System.currentTimeMillis();

		List<FileAccessId> filesToTransfer = new LinkedList<FileAccessId>();
		JobInfo jobInfo = jobToInfo.get(jobId);
                ExecutionParams execParams = jobInfo.getExecutionParams();

		/* Request needed transfers to the File Transfer Manager, given the file accesses
		 * that the task performs and the execution location decided by the Task Scheduler.
		 * The working directory is the folder where files will be transferred to.
		 */
		  for (Parameter p : params) {
		       switch(p.getType()) {
                               case FILE_T:
                                  FileParameter fp = (FileParameter)p;
                                  if (debug)
                                    logger.debug("    * " + fp);

				  if (!fp.getDirection().equals("OUT")){
				    Integer fId = fp.getFileAccessId().getFileId();
				    
				    for(FileInstanceId f : filesToTransf){
                                      if(fId == f.getFileId()){
                                        filesToTransfer.add(fp.getFileAccessId());
					if(debug) logger.debug("File will be transfered to: " +"file://"+ fileLocation.getHost() + fileLocation.getPath()+fp.getName());
				      }
				    }
			          }
				break;
				
			        default: // Basic types (including String)
                                   if (debug) logger.debug("    * " + (BasicTypeParameter)p);
                                break;
                      }//End Switch
                }//End for

		int transferId;
		if (filesToTransfer.size() > 0){
		       transferId = newTransfer.transferFiles(filesToTransfer, fileLocation);
		}
		else
	                transferId = FileTransfer.FILES_READY;
		
		transferToJob.put(transferId, jobId);
		
		// If no transfers were necessary, we are ready to run the job
		if (transferId == FileTransfer.FILES_READY) {
			  fileTransferInfo(transferId, TransferState.DONE, null);  		
		}
		else if (slaEnabled) {
			// Set transfer id parameter for it to appear in the usage record
			BasicTypeParameter btp = (BasicTypeParameter)params[params.length - 1];
			btp.setValue(Integer.toString(transferId));
		}
             if(timeMeasuring) orderTrfTime+= System.currentTimeMillis() - iTimeStamp;
	}

	
	// TransferStatus interface
	
	public void fileTransferInfo(int transferId, TransferState status, String message) {
               double iTimeStamp =0;
               if(timeMeasuring) iTimeStamp = System.currentTimeMillis();

		Integer jobId = transferToJob.remove(transferId);
		String host = jobToInfo.get(jobId).getExecutionParams().getHost();
		JobInfo info = jobToInfo.get(jobId);
		int numSlots;

		if (debug)
			logger.debug("Received a notification for the transfers of job " + jobId + " with state " + status);

		switch (status) {
			case DONE:
				// Request the job submission
					host = jobToInfo.get(jobId).getExecutionParams().getHost();
					numSlots = hostToSlots.get(host);

			                if (hostToSlots.get(host) > 0) {
						// There is at least one free processor on the host, enqueue for submission
						queue.enqueue(jobId);
						if(debug) logger.debug("Now there are " + hostToSlots.get(host) + " empty slots on host " + host);
					}
					else {
						// All host processors are busy, put in pending
						if (debug) logger.debug("Prescheduling job " + jobId + " at host " + host + ", now pending");
						List<Integer> pending = hostToPending.get(host);
						pending.add(jobId);
						
						//Updating wait time in queue counter
                                                Integer methodId = info.getMethod().getId();
					        reschedule.updateWTCounters(host,methodId,1);
					}

                                        //Decreasing slots number
                                        hostToSlots.put(host, --numSlots);
				break;
			case FAILED:

				boolean noRetries = true;

				//Adding a submit failure on host
				reschedule.addFailedSubmit(host);
				
				JobInfo jobInfo = jobToInfo.get(jobId);

                                //Freeing host slot
				numSlots = hostToSlots.get(host);
                                hostToSlots.put(host, ++numSlots);

				int hostRetries = hostToRetry.get(host);
				hostToRetry.put(host,(--hostRetries));

				for (Map.Entry<String,Integer> r : hostToRetry.entrySet()){
			              if(r.getValue() > 0){
					 noRetries = false;
					 break;
                                      }
                  		}
					
				if(noRetries){
                                 // Already rescheduled job on all available resources, notify the failure to Task Scheduler
				  jobStatus.notifyJobEnd(jobId, JobEndStatus.TRANSFERS_FAILED, STAGING_ERR + ": " + message, null, null);
				  jobToInfo.remove(jobId);
                                }
				else{
				// Try to reschedule in available resources (with left retries)
                                 if(debug) logger.debug("Asking for reschedule of job " + jobId + " due to transfer failure: " + message);
                                 jobToInfo.remove(jobId);
                                 reschedule.rescheduleJob(jobId);
                               }
				break;
		}
                if(timeMeasuring)  timeFileTrfInfo+= System.currentTimeMillis() - iTimeStamp;
	}
	
	
	
	// Private method for job status notification
	
	private void jobStatusNotification(Integer jobId, JobEndStatus endStatus) {
		if (debug)
			logger.debug("Received a notification for job " + jobId + " with state " + endStatus);
		
		JobInfo jobInfo;

		switch (endStatus) {
			case OK:

				//Timestamping the job ending
                                jobInfo = jobToInfo.get(jobId);
                                Map<Integer,Long> jobInSubmStartTime =  TaskIdTojobInSStart.get(jobInfo.getMethod().getId());

				if(jobInSubmStartTime.get(jobId) != null){
                                  float jobInExecTime = (System.currentTimeMillis() - jobInSubmStartTime.get(jobId))/(float) 1000;
                                  String host = jobInfo.getExecutionParams().getHost();
			    
                                  //Adding the job execution time to job type mean execution time
                                  reschedule.addHostExTimeMeanValue(host,jobInExecTime,jobInfo.getMethod().getId());

			  	  if (debug)
                                     logger.debug("Job " + jobId +  " With Task Id "+jobInfo.getMethod().getId()+" executed in : "+jobInExecTime+ " s");
				}

				// Job finished, update info about the generated/updated files
				jobInfo = jobToInfo.remove(jobId);

				ExecutionParams execParams = jobInfo.getExecutionParams();
				Location loc = new Location(execParams.getHost(), execParams.getWorkingDir());
		
				//Adding an OK submit to the host
                                reschedule.addOkSubmit(execParams.getHost());

				Map<FileInstanceId,Location> fIdToLocations = new THashMap<FileInstanceId,Location>();
				Map<FileInstanceId,List<Long>> fIdToSizeAndMod = new THashMap<FileInstanceId,List<Long>>();

				for (Parameter p : jobInfo.getMethod().getParameters()) {
					switch(p.getType()) {
						case FILE_T:
							FileInstanceId fId = null;
							FileParameter fp = (FileParameter)p;
							switch (p.getDirection()) {
								case IN:
									// FIP and FTM already know about that file
									continue;
								case OUT:
									fId = ((WAccessId)fp.getFileAccessId()).getWrittenFileInstance();
									break;
								case INOUT:
									fId = ((RWAccessId)fp.getFileAccessId()).getWrittenFileInstance();
									break; 
							}
							// OUT or INOUT,: we must tell to FIP and FTM about the generated/updated file
							String name = fId.getRenaming();

							List<Location> location = new LinkedList<Location>();
				                        location.add(loc);

							newVersion.newFileVersion(fId, name, location);

							newLocation.addLocation(fId, loc.getHost(), loc.getPath());
							fIdToLocations.put(fId,loc);

							//Registering the file size as well
							List<Long> l = getFileSizeAndLastMod(name,loc.getHost(),loc.getPath());									    
						 	newLocation.addSize(fId,l);
							fIdToSizeAndMod.put(fId,l);
							
							break;
							
						default:
							break;
					}
				}
				jobStatus.notifyJobEnd(jobId, endStatus, JOB_OK,fIdToLocations,fIdToSizeAndMod);
				checkPending(execParams);
				break;
				
			default: // SUBMISSION_FAILED or EXECUTION_FAILED
				jobInfo = jobToInfo.get(jobId);
		
				if(TaskIdTojobInSStart.get(jobInfo.getMethod().getId()) != null){
				  jobInSubmStartTime = TaskIdTojobInSStart.get(jobInfo.getMethod().getId());
			 	  jobInSubmStartTime.remove(jobId);				
				}

				switch (jobInfo.getHistory()) {
					case NEW:
						// Try resubmission to the same host
						if(debug) logger.debug("Resubmitting job " + jobId + " to host " + jobInfo.getExecutionParams().getHost());
						jobInfo.setHistory(JobHistory.RESUBMITTED);
						jobInfo.setGATJob(null);
                                                queue.enqueue(jobId);						
						break;
					case RESUBMITTED:
						// Already resubmitted, ask the Task Scheduler for a reschedule on another host
						if(debug) logger.debug("Asking for reschedule of job " + jobId + " due to job failure: " + endStatus);
						//Adding a submit failure to the host.
		                                reschedule.addFailedSubmit(jobInfo.getExecutionParams().getHost());
						jobToInfo.remove(jobId);
						reschedule.rescheduleJob(jobId);
						checkPending(jobInfo.getExecutionParams());
						break;
					case RESCHEDULED:
						// Already rescheduled, notify the failure to the Task Scheduler
						if(debug) logger.debug("The rescheduled job " + jobId + " failed again, now in host "
								      + jobInfo.getExecutionParams().getHost() + ": " + endStatus);

                                                //Adding a submit failure to host.
		                                reschedule.addFailedSubmit(jobInfo.getExecutionParams().getHost());
						jobToInfo.remove(jobId);

						String errorMessage = endStatus == JobEndStatus.SUBMISSION_FAILED ?
											  JOB_SUBMISSION_ERR : JOB_EXECUTION_ERR;
						jobStatus.notifyJobEnd(jobId, endStatus, errorMessage + ": " + jobInfo, null, null);
						checkPending(jobInfo.getExecutionParams());
						break;
				}				
				break;
		}
	}
	
	
	// Private method for preschedule
	private void checkPending(ExecutionParams execParams) {

                double iTimeStamp =0;
                if(timeMeasuring) iTimeStamp = System.currentTimeMillis();

		String host = execParams.getHost();
		List<Integer> pending = hostToPending.get(host);
		int slots = 0;

		if(debug) logger.debug("Pending for host " + host + " is " + pending.size());
		if (pending.size() > 0) {
			int preschedJobId = pending.remove(0);
			if (debug) logger.debug("Putting in queue the prescheduled job " + preschedJobId + " for host " + host);
			queue.enqueue(preschedJobId);

                        JobInfo info = jobToInfo.get(preschedJobId);
			Integer methodId = info.getMethod().getId();

                        //Updating wait time in queue counter
		        reschedule.updateWTCounters(host,methodId,(-1));
		}//End of pending

		//Update slots status, freeing slots if it is needed
		  slots = hostToSlots.get(host);
		  hostToSlots.put(host, ++slots);
		  if(debug) logger.debug("Now there are " + hostToSlots.get(host) + " empty slots");
                 if(timeMeasuring)  timeCheckPend+= System.currentTimeMillis() - iTimeStamp;
	}


	// Private method for job preparation
	
	private JobDescription prepareJob(Integer jobId) throws Exception {
		// Get the information related to the job
		JobInfo jobInfo = jobToInfo.get(jobId);
		Method method = jobInfo.getMethod();
		String methodName = method.getName();
		ExecutionParams execParams = jobInfo.getExecutionParams();
		String targetPath = execParams.getInstallDir();
		String targetHost = execParams.getHost();
		String targetUser = execParams.getUser();
		if (userNeeded && targetUser != null)
			targetUser += "@";
		else
			targetUser = "";
		
		SoftwareDescription sd = new SoftwareDescription();
		sd.setExecutable(targetPath + workerScript);
		ArrayList<String> lArgs = new ArrayList<String>();
		// Prepare arguments: working_dir debug method_class method_name num_params par_type_1 par_1 ... par_type_n par_n
		lArgs.add(execParams.getWorkingDir());
		lArgs.add(workerDebug);
		lArgs.add(method.getDeclaringClass());
		lArgs.add(methodName);
		lArgs.add(Integer.toString(method.getParameters().length));
		for (Parameter param : method.getParameters()) {
			ParamType type = param.getType();
			lArgs.add(Integer.toString(type.ordinal()));
			switch (type) {
				case FILE_T:
					FileParameter fPar = (FileParameter)param;
					FileAccessId fAccId = (FileAccessId)PAFuture.getFutureValue(fPar.getFileAccessId());
					if (fAccId instanceof RAccessId) {
						FileInstanceId fId = ((RAccessId)fAccId).getReadFileInstance();
						
						//If 1st Version, submit the job using original name
						if (fId.getVersionId() == 1) 
							lArgs.add(newLocation.getOriginalName(fId));
						else
							lArgs.add(fId.getRenaming());
					}
					else if (fAccId instanceof WAccessId)
						lArgs.add(((WAccessId)fAccId).getWrittenFileInstance().getRenaming());
					else  // fAccId instanceof RWAccessId
						lArgs.add(((RWAccessId)fAccId).getWrittenFileInstance().getRenaming());
					break;
					
				case STRING_T:
					BasicTypeParameter btParS = (BasicTypeParameter)param;
					// Check spaces
					String value = btParS.getValue().toString();
					int numSubStrings = value.split(" ").length;
					lArgs.add(Integer.toString(numSubStrings));
					lArgs.add(value);
					break;
					
				default: // Basic types
					BasicTypeParameter btParB = (BasicTypeParameter)param;
					lArgs.add(btParB.getValue().toString());
			}
		}
		// Conversion vector -> array
		String[] arguments = new String[lArgs.size()];
		arguments = lArgs.toArray(arguments);
        sd.setArguments(arguments);

        sd.addAttribute("jobId", jobId);
        
        if (debug) {
        	// Set standard output file for job
        	File outFile = GAT.createFile(context, "any:///job" + jobId + ".out");
        	sd.setStdout(outFile);
        }
        
        if (debug || usingGlobus) {
        	// Set standard error file for job
        	File errFile = GAT.createFile(context, "any:///job" + jobId + ".err");
        	sd.setStderr(errFile);
        }
        
        Map<String,Object> attributes = new THashMap<String,Object>();
        attributes.put(RES_ATTR, ANY_PROT + targetUser + targetHost);
        ResourceDescription rd = new HardwareResourceDescription(attributes);
        
        if (debug) {
        	logger.debug("Ready to submit job " + jobId + ":");
        	logger.debug("  * Host: " + targetHost);
        	logger.debug("  * Executable: " + sd.getExecutable());
        	
        	StringBuilder sb = new StringBuilder("  - Arguments:");
        	for (String arg : sd.getArguments()) {
        		sb.append(" ").append(arg);
        	}
        	logger.debug(sb.toString());
        }
        
        return new JobDescription(sd, rd);
    }
	
	
	
	// MetricListener interface implementation
	
	public void processMetricEvent(MetricEvent value) {
		Job job = (Job)value.getSource();
		JobState newJobState = (JobState)value.getValue();
		
		Integer jobId = (Integer)((JobDescription)job.getJobDescription()).getSoftwareDescription().getAttributes().get("jobId");

       		 /* Check if either the job has finished or there has been a submission error.
		 * We don't care about other state transitions
		 */
		if (newJobState == JobState.STOPPED) {
			/* We must check whether the chosen adaptor is globus
			 * In that case, since globus doesn't provide the exit status of a job,
			 * we must examine the standard error file
			 */
			try {
				if (usingGlobus) {
					File errFile = ((JobDescription)job.getJobDescription()).getSoftwareDescription().getStderr();
					// Error file should always be in the same host as the IT
					File localFile = GAT.createFile(context, errFile.toGATURI());
					if (localFile.length() > 0)
						jobStatusNotification(jobId, JobEndStatus.EXECUTION_FAILED);
					else {
						if (!debug) localFile.delete();
						jobStatusNotification(jobId, JobEndStatus.OK);
					}
				}
				else {
					if (job.getExitStatus() == 0)
						jobStatusNotification(jobId, JobEndStatus.OK);
					else 
						jobStatusNotification(jobId, JobEndStatus.EXECUTION_FAILED);
				}
			}
			catch (Exception e) {
				logger.error(CALLBACK_PROCESSING_ERR + ": " + jobToInfo.get(jobId)+" JobId -> "+jobId, e);
				errorReport.throwError(ITConstants.JM, CALLBACK_PROCESSING_ERR + ": " + jobToInfo.get(jobId), e);
			}
		}
		else if (newJobState == JobState.SUBMISSION_ERROR) {
			try {
				if (debug)
					logger.debug("Job info for job " + jobId +": " + job.getInfo() + "\n" + jobToInfo.get(jobId));
			
				if (usingGlobus && job.getInfo().get("resManError").equals("NO_ERROR"))
					jobStatusNotification(jobId, JobEndStatus.OK);
				else
					jobStatusNotification(jobId, JobEndStatus.SUBMISSION_FAILED);
			}
			catch (GATInvocationException e) {
				logger.error(CALLBACK_PROCESSING_ERR + ": " + jobToInfo.get(jobId), e);
				errorReport.throwError(ITConstants.JM, CALLBACK_PROCESSING_ERR + ": " + jobToInfo.get(jobId), e);
			}
		}
    }


     private List<Long> getFileSizeAndLastMod(String fileName,String host,String path){

           double iTimeStamp =0;
           if(timeMeasuring) iTimeStamp = System.currentTimeMillis();

           String target = null;
           String targetUser = null;
           URI sourceURI = null;
           File f = null;
           long size = 0;
           long lastmod = 0;
           List<Long> l = new ArrayList<Long>();

           //Asking for source file size
           //We will check if the source of file is a worker or a datanode
           if(projManager.getProperty(host, ITConstants.USER) != null){
              targetUser = projManager.getProperty(host, ITConstants.USER);
           }
           else{
              targetUser = projManager.getDataNodeProperty(host, ITConstants.USER);
           }

           if (userNeeded && targetUser != null)
               targetUser += "@";
           else
               targetUser = "";

           try{
                sourceURI = new URI("any://"
                                    + targetUser
                                    + host + "/"
                                    + path
                                    + fileName);
              }
              catch (URISyntaxException e) {
                logger.error("Syntax error creating Source URI in size file checker ", e);
              }

              try{
                   f = GAT.createFile(context,sourceURI);
                 }
                 catch (Exception e) {
                   logger.error("Error checking file existance ", e);
                 }

                 size = f.length();
                 lastmod = f.lastModified();
                 l.add(size);
                 l.add(lastmod);

          if(timeMeasuring)  timeGetSize+= System.currentTimeMillis() - iTimeStamp;
          return l;
        }

     private void calcMeanNetSpeed(){

             double iTimeStamp =0;
             if(timeMeasuring) iTimeStamp = System.currentTimeMillis();

	   //Reloading historical file
              try {
                 histManager = new HistoricalManager();
              }
              catch (Exception e) {
                 logger.error(HIST_LOAD_ERR, e);
              }

	  //Updating speed matrix on historical file
	  histManager.setSpeedMatrix(hostToSpeedMatrix);

          if(timeMeasuring) timeCalcNetSpeed+= System.currentTimeMillis() - iTimeStamp;

          logger.info("Updating net speed matrix on historical file");

          if(timeMeasuring){
            logger.info(" ");
            logger.info("Debug Run Time of functions (in seconds) : ");
	    logger.info("Total time on orderTransfers -> "+orderTrfTime/1000);
            logger.info("Total time on fileTransferInfo -> "+timeFileTrfInfo/1000);
            logger.info("Total time on checkPending -> "+timeCheckPend/1000);
            logger.info("Total time on calcNetSpeed -> "+timeCalcNetSpeed/1000);
            logger.info("Total time on TS getFileSizeAndLastMod  -> "+timeGetSize/1000);
            logger.info(" ");
          }
        }


	public Map<String,Map<String,Float>> getSpeedMatrix(){
	  return hostToSpeedMatrix;
	}

	public void setSpeedMatrix(Map<String,Map<String,Float>> speedMatrix){	
  	  hostToSpeedMatrix = speedMatrix;
	}

	public void speedMatrixUpdate(String source, String target, float netSpeed){
	  float oldNetSpeed = hostToSpeedMatrix.get(source).get(target);
	  hostToSpeedMatrix.get(source).put(target,((oldNetSpeed+netSpeed)/2));
	}

	public Map<String,Boolean> getHostToUsedInRun(){
	 return hostToUsedInRun;
	}

	public Map<String,Integer> getHostToRetry(){
         return hostToRetry;
        }

	// Controller interfaces implementation
	
	// Binding controller interface
	
	public String[] listFc() {
	    return new String[] { "JobStatus" , "Reschedule" , "NewTransfer" , "NewVersion" , "NewLocation" , "ErrorReport" };
	}

	public Object lookupFc(final String cItf) {
		if (cItf.equals("JobStatus")) {
			return jobStatus;
	    }
		else if (cItf.equals("Reschedule")) {
			return reschedule;
	    }
		else if (cItf.equals("NewTransfer")) {
			return newTransfer;
	    }
		else if (cItf.equals("NewVersion")) {
			return newVersion;
	    }
		else if (cItf.equals("NewLocation")) {
			return newLocation;
	    }
		else if (cItf.equals("ErrorReport")) {
			return errorReport;
	    }
	    return null;
	}

	public void bindFc(final String cItf, final Object sItf) {
		if (cItf.equals("JobStatus")) {
	    	jobStatus = (JobStatus)sItf;
	    }
		else if (cItf.equals("Reschedule")) {
			reschedule = (Schedule)sItf;
	    }
		else if (cItf.equals("NewTransfer")) {
			newTransfer = (FileTransfer)sItf;
	    }
		else if (cItf.equals("NewVersion")) {
			newVersion = (FileInfoUpdate)sItf;
	    }
		else if (cItf.equals("NewLocation")) {
			newLocation = (FileInformation)sItf;
	    }
		else if (cItf.equals("ErrorReport")) {
			errorReport = (ITError)sItf;
	    }
	}
	
	public void unbindFc(final String cItf) {
	    if (cItf.equals("JobStatus")) {
	    	jobStatus = null;
		}
	    else if (cItf.equals("Reschedule")) {
	    	reschedule = null;
		}
	    else if (cItf.equals("NewTransfer")) {
	    	newTransfer = null;
		}
	    else if (cItf.equals("NewVersion")) {
	    	newVersion = null;
		}
	    else if (cItf.equals("NewLocation")) {
	    	newLocation = null;
		}
	    else if (cItf.equals("ErrorReport")) {
	    	errorReport = null;
		}
	}
	
	
	
	// Thread that handles job submission requests
	private class JobDispatcher extends RequestDispatcher<Integer> {
		
		// Object to register callbacks on
		private JobManager associatedJM;
		
		// Brokers - TODO: Problem if many resources used
		private Map<String,ResourceBroker> brokers;

		private Integer lastJobId = null;
		private JobInfo lastJobInfo = null;		

		public JobDispatcher(RequestQueue<Integer> queue,
						     JobManager associatedJM) {
			super(queue);
			this.associatedJM = associatedJM;
			this.brokers = new TreeMap<String,ResourceBroker>();
		}
		
		public void processRequests() {
			while (true) {
				Integer jobId = queue.dequeue();
				if (jobId == null) break;

				JobInfo info = jobToInfo.get(jobId);	
				//logger.debug("lastJobId -> "+lastJobId+" JobId -> "+jobId);
                            	
			        if((lastJobId == jobId) && (lastJobInfo.getHistory() == info.getHistory())){
                                  logger.debug(" Breaking Loop! Repeated Job Submission");
				  break;
				 }

				 lastJobInfo = info;
                                 lastJobId = jobId;

				// Prepare the job
				JobDescription jobDescr = null;
				try {
					jobDescr = prepareJob(jobId);
				}
				catch (Exception e) {
					logger.error(JOB_PREPARATION_ERR + ": " + info, e);
					errorReport.throwError(ITConstants.JM, JOB_PREPARATION_ERR + ": " + info, e);
					continue;
				}
				
				// Get a broker for the host
				ResourceBroker broker = null;
				try {
					String dest = (String)jobDescr.getResourceDescription().getResourceAttribute(RES_ATTR);
					if ((broker = brokers.get(dest)) == null) {
						broker = GAT.createResourceBroker(context, new URI(dest));
						brokers.put(dest, broker);
					}
				}
				catch (Exception e) {
					logger.error(RB_CREATION_ERR, e);
					errorReport.throwError(ITConstants.JM, RB_CREATION_ERR, e);
					return;
				}
				
				// Submit the job, registering for notifications of job state transitions (associatedJM is the metric listener)
				 Job job = null;

				 try{
                                 //Timestamping the job submission
                                  info = jobToInfo.get(jobId);

				  if(TaskIdTojobInSStart.get(info.getMethod().getId()) != null){
                                    Map<Integer,Long> jobInSubmStartTime = TaskIdTojobInSStart.get(info.getMethod().getId());
                                    jobInSubmStartTime.put(jobId,System.currentTimeMillis());
                                  }
                                  else{
                                    Map<Integer,Long> jobInSubmStartTime = new THashMap<Integer,Long>();
                                    jobInSubmStartTime.put(jobId,System.currentTimeMillis());
                                    TaskIdTojobInSStart.put(info.getMethod().getId(),jobInSubmStartTime);
                                  }
                                 }catch (Exception e){
                                  logger.error(JOB_SUBMISSION_ERR + ": " + jobToInfo.get(jobId)+" JobId -> "+jobId, e);
                                 }

				try {
			         job = broker.submitJob(jobDescr, associatedJM, JOB_STATUS);
				}
				catch (Exception e) {
					logger.error(JOB_SUBMISSION_ERR + ": " + jobToInfo.get(jobId)+" JobId -> "+jobId, e);
					jobStatusNotification(jobId, JobEndStatus.SUBMISSION_FAILED);
					continue;
				}

				// Update mapping
				info.setGATJob(job);

				if (debug) logger.debug("Job " + jobId + " submitted");
			}
		}
	}
	
	private class Cleaner extends RequestDispatcher<SoftwareDescription> {
		
		private RequestQueue<Job> jobQueue;
		
		public Cleaner(RequestQueue<SoftwareDescription> sdQueue,
					   RequestQueue<Job> jobQueue) {
			super(sdQueue);
			this.jobQueue = jobQueue; 
		}
		
		public void processRequests() {
			while (true) {
				SoftwareDescription sd = queue.dequeue();
				if (sd == null) break;
				
				try {
					URI brokerURI = new URI((String)sd.getObjectAttribute("uri"));
					ResourceBroker broker = GAT.createResourceBroker(context, brokerURI);
		
					Job job = broker.submitJob(new JobDescription(sd));
					jobQueue.enqueue(job);
				}
				catch (Exception e) {
					logger.error(JOB_SUBMISSION_ERR + ": clean job");
				}
			}
		}	
	}
	
}
