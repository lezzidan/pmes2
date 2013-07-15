
package integratedtoolkit.types.file;

import java.io.Serializable;


public class Location implements Serializable, Comparable {
	
	private String host;
	private String path;
	
	/* Alternative implementation:
	 * - Disk name
	 * - Relative path
	 */
	
	public Location(String host, String path) {
		this.host = host;
		this.path = path;
	}
	
	public String getHost() {
		return this.host;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	// Comparable interface implementation
	public int compareTo(Object loc) throws NullPointerException, ClassCastException {
		if (loc == null)
			throw new NullPointerException();
		
	    if (!(loc instanceof Location))
	    	throw new ClassCastException("A Location object expected");
	    
	    int compHost;
	    // First compare hosts
	    if ( (compHost = this.getHost().compareTo(((Location)loc).getHost())) != 0 )
	    	return compHost;
	    // If same host, compare paths
	    else
	    	return this.getPath().compareTo(((Location)loc).getPath());
	}
	
	
	// Override the toString method
	public String toString() {
		return host + ":" + path; 
	}
	
}
