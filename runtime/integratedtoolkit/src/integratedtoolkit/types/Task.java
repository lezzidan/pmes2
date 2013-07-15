
package integratedtoolkit.types;

import java.io.Serializable;


public class Task implements Serializable, Comparable {
	
	// Task states
	public enum TaskState {
		PENDING,
		TO_SCHEDULE,
		TO_RESCHEDULE,
        TO_EXECUTE,
        FINISHED,
        FAILED,
        CANCELLED,
        CHECKPOINTED;
   	}
	
	private static final int FIRST_TASK_ID = 1;
	
	// Task fields
	private int taskId;
	private TaskState status;
	private Method method;
	
	// Task ID management
	private static int nextTaskId;
	
	
	public static void init() {
		nextTaskId = FIRST_TASK_ID;
	}
	
	// Java task
	public Task(String methodClass, String methodName, Parameter[] parameters) {
		this.taskId = nextTaskId++;
		this.status = TaskState.PENDING;
		this.method = new Method(methodClass, methodName, parameters);
	}
	
	// C task
	public Task(int methodId, Parameter[] parameters) {
		this.taskId = nextTaskId++;
		this.status = TaskState.PENDING;
		this.method = new Method(methodId, parameters);
	}
	
	public int getId() {
		return taskId;
	}
	
	public TaskState getStatus() {
		return status;
	}
	
	public void setStatus(TaskState status) {
		this.status = status;
	}
	
	public Method getMethod() {
		return method;
	}
	
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		
		buffer.append("[[Task id: ").append(getId()).append("]");
		buffer.append(", [Status: ").append(getStatus()).append("]");
		buffer.append(", ").append(getMethod().toString()).append("]");
		
		return buffer.toString();
	}
	
	// Comparable interface implementation
	
	public int compareTo(Object task) throws NullPointerException, ClassCastException {
		if (task == null)
			throw new NullPointerException();
		
	    if (!(task instanceof Task))
	    	throw new ClassCastException("A Task object expected");
	      
	    return this.getId() - ((Task)task).getId();
	}
	
}
