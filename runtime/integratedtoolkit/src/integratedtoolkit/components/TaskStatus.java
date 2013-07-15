
package integratedtoolkit.components;

import integratedtoolkit.interfaces.ITTaskSubscription;
import integratedtoolkit.types.Task;


/* To inform about the end of a task
 * To wait until the last writer task of a given file finishes
 * To get the task information for a reschedule (abnormal end of task)
 */
public interface TaskStatus extends ITTaskSubscription {
	
	enum TaskEndStatus {
		OK,
		FAILED;
	}
	
	void notifyTaskEnd(int taskId, TaskEndStatus status, String message);
	
	Task getTaskInfo(int taskId);
	
}
