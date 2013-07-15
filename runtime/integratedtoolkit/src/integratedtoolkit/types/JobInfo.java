
package integratedtoolkit.types;

import org.gridlab.gat.resources.Job;


public class JobInfo {
	
	// Job identifier management
	private static int nextJobId;
	private static final int FIRST_JOB_ID = 1;

	// Job history
	public enum JobHistory {
		NEW,
		RESUBMITTED,
		RESCHEDULED;
	}
	
	// Information of the job
	private int jobId;
	private Method method;
	private ExecutionParams execParams;
	private Job gatJob;
	private JobHistory history;
	
	public static void init() {
		nextJobId = FIRST_JOB_ID;
	}
	
	public JobInfo(Method method, ExecutionParams execParams) {
		this.jobId = nextJobId++;
		this.method = method;
		this.execParams = execParams;
		this.history = JobHistory.NEW;
	}
	
	public int getJobId() {
		return jobId;
	}
	
	public Method getMethod() {
		return method;
	}
	
	public ExecutionParams getExecutionParams() {
		return execParams;
	}
	
	public Job getGATJob() {
		return gatJob;
	}
	
	public JobHistory getHistory() {
		return history;
	}
	
	public void setExecutionParams(ExecutionParams newExecParams) {
		this.execParams = newExecParams;
	}
	
	public void setGATJob(Job gatJob) {
		this.gatJob = gatJob;
	}
	
	public void setHistory(JobHistory newHistoryState) {
		this.history = newHistoryState;
	}
	
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		
		buffer.append("[[Job id: ").append(getJobId()).append("]");
		buffer.append(", ").append(getMethod().toString());
		buffer.append(", [Target host: ").append(getExecutionParams().getHost()).append("]");
		buffer.append(", [User: ").append(getExecutionParams().getUser()).append("]]");
		
		return buffer.toString();
	}
	
}
