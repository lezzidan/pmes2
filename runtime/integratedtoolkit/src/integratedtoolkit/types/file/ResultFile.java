
package integratedtoolkit.types.file;

import java.io.Serializable;


public class ResultFile implements Serializable, Comparable {
	
	FileInstanceId fId;
	String originalName;
	Location originalLocation;
	
	
	public ResultFile (FileInstanceId fId, String name, Location loc) {
		this.fId = fId;
		this.originalName = name;
		this.originalLocation = loc;
	}

	public FileInstanceId getFileInstanceId() {
		return fId;
	}
	
	public String getOriginalName() {
		return originalName;
	}
	
	public Location getOriginalLocation() {
		return originalLocation;
	}
	
	
	// Comparable interface implementation
	public int compareTo(Object resFile) throws NullPointerException, ClassCastException {
		if (resFile == null)
			throw new NullPointerException();
		
	    if (!(resFile instanceof ResultFile))
	    	throw new ClassCastException("A ResultFile object expected");
	    
	    ResultFile resultFile = (ResultFile)resFile;
	    
	    // Compare file identifiers
	    return this.getFileInstanceId().compareTo(resultFile.getFileInstanceId());
	}
	
}
