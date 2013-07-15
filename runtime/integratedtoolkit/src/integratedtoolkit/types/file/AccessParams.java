
package integratedtoolkit.types.file;

import java.io.Serializable;

import integratedtoolkit.interfaces.ITFileAccess.AccessMode;


// Parameters of access to a file
public class AccessParams implements Serializable {

	private String name;
	private String path;
	private String host;
	private AccessMode mode;
	
	public AccessParams (String name, String path, String host, AccessMode mode) {
		this.name = name;
		this.path = path;
		this.host = host;
		this.mode = mode;
	}
	
	public String getName() {
		return name;
	}
	
	public String getPath() {
		return path;
	}
	
	public String getHost() {
		return host;
	}
	
	public AccessMode getMode() {
		return mode;
	}
	
}
