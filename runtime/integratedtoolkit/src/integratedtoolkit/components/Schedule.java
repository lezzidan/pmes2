
package integratedtoolkit.components;

import java.util.List;
import java.util.Map;
import integratedtoolkit.types.Task;

/* To request the scheduling of a set of tasks
 * To request the rescheduling of a job in another host (for fault tolerance purposes)
 */
public interface Schedule {

	void scheduleTasks(List<Task> tasks);
	
	void rescheduleJob(int jobId);

        void updateWTCounters(String host, Integer methodId, Integer count);

	void addHostExTimeMeanValue(String host, float enlapsedTime, int methodId);
	
	void addOkSubmit(String host);

	void addFailedSubmit(String host);

	Map<String,Integer> getHostToRetry();
}
