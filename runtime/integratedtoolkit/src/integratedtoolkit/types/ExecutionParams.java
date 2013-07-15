
package integratedtoolkit.types;

import java.io.Serializable;


public class ExecutionParams implements Serializable {
	
	// Parameters of a concrete execution of a task
	private String user;
	private String host;
	private String installDir;
	private String workingDir;
	private int cost;
	private String queue;
	
	
	public ExecutionParams(String user,
						   String host,
						   String installDir,
						   String workingDir) {
		this(user, host, installDir, workingDir, 0, null);
	}
	
	public ExecutionParams(String user,
						   String host,
						   String installDir,
						   String workingDir,
						   int cost,
						   String queue) {
		this.user = user;
		this.host = host;
		this.installDir = installDir;
		this.workingDir = workingDir;
		this.cost = cost;
		this.queue = queue;
	}
	
	public String getUser() {
		return user;
	}
	
	public String getHost() {
		return host;
	}
	
	public String getInstallDir() {
		return installDir;
	}
	
	public String getWorkingDir() {
		return workingDir;
	}
	
	public int getCost() {
		return cost;
	}
	
	public String getQueue() {
		return queue;
	}
	
}
