
package integratedtoolkit.components.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import org.objectweb.fractal.api.control.BindingController;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.RunActive;
import org.objectweb.proactive.Service;
import org.objectweb.proactive.core.util.wrapper.StringWrapper;

import integratedtoolkit.ITConstants;
import integratedtoolkit.components.AppTaskEvents;
import integratedtoolkit.components.FileAccess;
import integratedtoolkit.components.FileTransfer;
import integratedtoolkit.components.Preparation;
import integratedtoolkit.components.Schedule;
import integratedtoolkit.components.TaskCreation;
import integratedtoolkit.components.TaskStatus;
import integratedtoolkit.interfaces.ITError;
import integratedtoolkit.interfaces.ITFileAccess.AccessMode;
import integratedtoolkit.log.Loggers;
import integratedtoolkit.types.Task;
import integratedtoolkit.types.Task.TaskState;
import integratedtoolkit.types.file.AccessParams;
import integratedtoolkit.types.file.FileAccessId;
import integratedtoolkit.types.Parameter;
import integratedtoolkit.types.Parameter.*;
import integratedtoolkit.util.ElementNotFoundException;
import integratedtoolkit.util.Graph;


public class TaskAnalyser implements TaskCreation, TaskStatus, Preparation,
									 BindingController,
									 RunActive {
	
	// Constants definition
	private static final int 	NO_TASK 	= -1;
	private static final String TASK_FAILED = "Task failed: ";
	
	// Client interfaces
	private Schedule newTask;
	private FileAccess fileAccess;
	private FileTransfer newTransfer;
	private AppTaskEvents appTaskEvents;
	private ITError errorReport;
	
	// Dependency graph
	private Graph<Integer,Task> depGraph;
	
	// <File id, Last writer task> table
	private Map<Integer,Integer> writers;
	
	// Successfully finished tasks
	private Set<Task> finishedTasks;
	
	// Application demands
	private boolean endRequested;
	private int taskWaited;
	
	// Component logger - No need to configure, ProActive does
	private static final Logger logger = Logger.getLogger(Loggers.TA_COMP);
	private static final boolean debug = logger.isDebugEnabled();
	private static final String lineSep = System.getProperty("line.separator");
	
	// SLA
	private static final boolean slaEnabled = System.getProperty(ITConstants.IT_SLA_ENABLED) != null
	  										  && System.getProperty(ITConstants.IT_SLA_ENABLED).equals("true")
	  										  ? true : false;
	
	// Language
	private static final boolean isJava = System.getProperty(ITConstants.IT_LANG) != null
    									  && System.getProperty(ITConstants.IT_LANG).equals("java")
    									  ? true : false;
	
	
	public TaskAnalyser() {	}
	
	
	// RunActive interface
	
	public void runActivity(Body body) {
		body.setImmediateService("terminate", new Class[] {});

		Service service = new Service(body);
		service.fifoServing();
	}
	
	
	
	// Server interfaces implementation
	
	// Preparation interface
	
	public StringWrapper initialize() {
		endRequested = false;
		taskWaited = NO_TASK;
		
		if (depGraph == null)
			depGraph = new Graph<Integer,Task>();
		else
			depGraph.clear();
		
		if (writers == null)
			writers = new TreeMap<Integer,Integer>();
		else
			writers.clear();
		
		if (finishedTasks == null)
			finishedTasks = new TreeSet<Task>();
		else
			finishedTasks.clear();
		
		Task.init();
		
		logger.info("Initialization finished");
		
		return new StringWrapper(ITConstants.INIT_OK);
	}
	
	
	public void cleanup() {
		// Nothing to do
		logger.info("Cleanup done");
	}
	
	
	
	// TaskCreation interface
	
	public void newTask(String methodClass, String methodName, Parameter[] parameters) {		
		// Create task and add it to the graph
		Task currentTask;
		
		if (isJava) currentTask = new Task(methodClass, methodName, parameters);
		else		currentTask = new Task(Integer.parseInt(methodName), parameters);
		
		int currentTaskId = currentTask.getId();
		depGraph.addNode(currentTask.getId(), currentTask);
		
		if (debug)
			logger.debug("New task(" + methodName + "), ID = " + currentTask.getId());
		
		if (slaEnabled) {
        	// Assign the file name for the usage record
        	FileParameter fp = (FileParameter)parameters[parameters.length - 3];
        	/* if (methodName.endsWith("WithUR")
			&& fp.getName().equals("dummy")) { */
        	String name = methodName.substring(0, (methodName.length() - 6));              
        	fp.setName(System.currentTimeMillis() + "-" + name + ".ur.xml");
        }
		
		List<AccessParams> accesses = new ArrayList<AccessParams>(parameters.length);
		for (Parameter p : parameters) {
			switch (p.getType()) {
				case FILE_T:
					AccessMode am = null;
					FileParameter fp = (FileParameter)p;
					
					if (debug)
						logger.debug("* Parameter : " + fp);
					
					// Conversion: direction -> file access mode
					switch (fp.getDirection()) {
						case IN: 	am = AccessMode.R;	break;
						case OUT: 	am = AccessMode.W; 	break;
						case INOUT: am = AccessMode.RW; break;
					}
					
					accesses.add(new AccessParams(fp.getName(),
									              fp.getPath(),
												  fp.getHost(),
												  am));
					
					break;
					
				default:
					/* Basic types (including String).
					 * The only possible access mode is R (already checked by the API)
					 */
					if (debug)
						logger.debug("* Parameter : " + (BasicTypeParameter)p);
				
					break;
			}
		}
		
		// Inform the File Manager about the new file accesses		
		List<FileAccessId> faIds = fileAccess.registerFileAccesses(accesses);	
		

		ListIterator<FileAccessId> lifaIds = faIds.listIterator();
		ListIterator<AccessParams> liacc = accesses.listIterator();
		for (Parameter p : parameters) {
			switch (p.getType()) {
				case FILE_T:
					FileParameter fp = (FileParameter)p;
					FileAccessId faId = lifaIds.next();
					AccessMode am = liacc.next().getMode();
					
					fp.setFileAccessId(faId);
					int fileId = faId.getFileId();
					if (am != AccessMode.W) { 	// R or RW
						Integer lastWriterId = writers.get(fileId);
						if (lastWriterId != null
							&& depGraph.get(lastWriterId) != null
							&& lastWriterId != currentTaskId) { // avoid self-dependencies
							
							if (debug) {
								logger.debug("Last writer for " + fp.getName() +" is task " + lastWriterId);
								logger.debug("Adding dependency between task " + lastWriterId + " and task " + currentTaskId);
							}
							
							try {
								depGraph.addEdge(lastWriterId, currentTaskId);
							}
							catch (ElementNotFoundException e) {
								logger.error("Error when adding a dependency between tasks "
										     + lastWriterId + " and " + currentTaskId, e);
								errorReport.throwError(ITConstants.TA, "", e);
								return;
							}
							
						}
					}
					
					if (am != AccessMode.R) {	// W or RW
						writers.put(fileId, currentTaskId); // update last writer
						
						if (debug)
							logger.debug("New writer for " + fp.getName() +" is task " + currentTaskId);
					}
					
					break;
					
				default:
					break;
			}
		}
		
		try {
			if (!depGraph.hasPredecessors(currentTaskId)) {
				// No dependencies for this task, schedule
				if (debug)
					logger.debug("Task " + currentTaskId + " has NO dependencies, send for schedule");
				
				List<Task> s = new LinkedList<Task>();
				s.add(currentTask);
				newTask.scheduleTasks(s);
			}
		}
		catch (ElementNotFoundException e) {
			logger.error("Error checking dependencies for task " + currentTaskId, e);
			errorReport.throwError(ITConstants.TA, "", e);
			return;
		}
	}
	
	
	public void noMoreTasks() {
		// Check now if all tasks have finished
		if (depGraph.getSize() == 0) {
			appTaskEvents.allTasksFinished();
			if (debug)
				logger.debug("Notification DONE: all tasks finished");
		}
		else endRequested = true;
	}
	
	
	public int synchronizeWithPreviousCreations() {
		return 0;
	}
	
	
	
	// TaskStatus interface
	
	public void notifyTaskEnd(int taskId, TaskEndStatus status, String message) {
		if (debug)
			logger.debug("Notification received for task " + taskId + " with end status "
					     + status + " and message \"" + message +"\"");
		
		if (status != TaskEndStatus.OK) {
			Task errTask = depGraph.get(taskId);
			errTask.setStatus(TaskState.FAILED);
			
			logger.error(TASK_FAILED + errTask + lineSep + message);
			errorReport.throwError(ITConstants.TA, TASK_FAILED + errTask + lineSep + message, null);
			return;
		}
		
		// Dependency-free tasks
		List<Task> toSchedule = new LinkedList<Task>();
		
		try {
			Iterator<Task> i = depGraph.getIteratorOverSuccessors(taskId);
			while (i.hasNext()) {
				Task succ = i.next();
				int succId = succ.getId();
				
				// Remove the dependency
				depGraph.removeEdge(taskId, succId);
				
				// Schedule if task has no more dependencies
				if (!depGraph.hasPredecessors(succId)) {
					succ.setStatus(TaskState.TO_SCHEDULE);
					toSchedule.add(succ);
				}
			}
		}
		catch (ElementNotFoundException e) {
			logger.error("Error removing the dependencies of task " + taskId, e);
			errorReport.throwError(ITConstants.TA, "", e);
			return;
		}
		
		if (!toSchedule.isEmpty()) {
			if (debug) {
				StringBuilder sb = new StringBuilder("All dependencies solved for tasks: ");
				for (Task t : toSchedule)
					sb.append(t.getId()).append("(").append(t.getMethod().getName()).append(") ");
				
				logger.debug(sb);
			}
			newTask.scheduleTasks(toSchedule);
		}
		
		// Add the task to the set of finished tasks
		Task finishedTask = depGraph.removeNode(taskId);
		finishedTask.setStatus(TaskState.FINISHED);
		finishedTasks.add(finishedTask);
		
		// Check if the finished task was the last writer of a file
		checkResultFileTransfer(finishedTask);
		
		if (endRequested) {
			// Check if all tasks have finished
			if (depGraph.getSize() == 0) {
				appTaskEvents.allTasksFinished();
				if (debug)
					logger.debug("Notification DONE: all tasks finished");
			}
		}
		// Check if the application is waiting for the finished task
		else if (taskWaited == taskId) {
			appTaskEvents.lastWriterTaskFinished();
			if (debug)
				logger.debug("Notification DONE: end of task " + taskWaited);
			
			taskWaited = NO_TASK;
		}
	}
	
	
	public void subscribeToTaskEnd(int fileId) {
		Integer lastWriterId = writers.get(fileId);
		
		// Check whether exists a last writer task for fileId and it has already finished
		if (lastWriterId == null || depGraph.get(lastWriterId) == null)
			appTaskEvents.lastWriterTaskFinished();
		else
			taskWaited = lastWriterId;
	}

	
	public Task getTaskInfo(int taskId) {
		return depGraph.get(taskId);
	}
	
	
	// Private method to check if a finished task is the last writer of its file parameters and eventually order the necessary transfers
	private void checkResultFileTransfer(Task t) {
		List<Integer> fileIds = new LinkedList<Integer>();
		for (Parameter p : t.getMethod().getParameters()) {
			switch (p.getType()) {
				case FILE_T:
					FileParameter fp = (FileParameter)p;
					switch (fp.getDirection()) {
						case IN:
							break;
						default: // OUT or INOUT
							int fileId = fp.getFileAccessId().getFileId();
							if (writers.get(fileId) == t.getId())
								fileIds.add(fileId);
							break;
					}
					break;
					
				default:
					break;
			}
		}
		// Order the transfer of the result files
		if (fileIds.size() > 0) newTransfer.transferBackResultFiles(fileIds);
	}
	
	
	
	// Controller interfaces implementation
	
	// Binding controller interface
	
	public String[] listFc() {
	    return new String[] { "NewTask" , "FileAccess" , "NewTransfer", "AppTaskEvents" , "ErrorReport"};
	}

	public Object lookupFc(final String cItf) {
		if (cItf.equals("NewTask")) {
			return newTask;
	    }
		else if (cItf.equals("FileAccess")) {
			return fileAccess;
	    }
		else if (cItf.equals("NewTransfer")) {
			return newTransfer;
	    }
		else if (cItf.equals("AppTaskEvents")) {
			return appTaskEvents;
	    }
		else if (cItf.equals("ErrorReport")) {
			return errorReport;
	    }
	    return null;
	}

	public void bindFc(final String cItf, final Object sItf) {
		if (cItf.equals("NewTask")) {
			newTask = (Schedule)sItf;
	    }
		else if (cItf.equals("FileAccess")) {
			fileAccess = (FileAccess)sItf;
	    }
		else if (cItf.equals("NewTransfer")) {
			newTransfer = (FileTransfer)sItf;
	    }
		else if (cItf.equals("AppTaskEvents")) {
			appTaskEvents = (AppTaskEvents)sItf;
	    }
		else if (cItf.equals("ErrorReport")) {
			errorReport = (ITError)sItf;
	    }
	}

	public void unbindFc(final String cItf) {
		if (cItf.equals("NewTask")) {
			newTask = null;
		}
		else if (cItf.equals("FileAccess")) {
			fileAccess = null;
		}
	    else if (cItf.equals("NewTransfer")) {
	    	newTransfer = null;
		}
		else if (cItf.equals("AppTaskEvents")) {
			appTaskEvents = null;
		}
		else if (cItf.equals("ErrorReport")) {
			errorReport = null;
		}
	}
	
}
