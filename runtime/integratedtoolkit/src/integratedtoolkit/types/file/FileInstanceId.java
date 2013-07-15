
package integratedtoolkit.types.file;

import java.io.Serializable;


// A File Instance is identified by its file and version identifiers 
public class FileInstanceId implements Serializable, Comparable {

	// Time stamp
    private static String timeStamp = Long.toString(System.currentTimeMillis());
	
	// File instance identifier fields
	private int fileId;
	private int versionId;
	
	// Renaming for this file version
	private String renaming;

	
	public FileInstanceId() { }
	
	public FileInstanceId(int fileId, int versionId) {
		this.fileId = fileId;
		this.versionId = versionId;
		this.renaming = "f" + fileId + "v" + versionId + "_" + timeStamp + ".IT";
	}
	
	public int getFileId() {
		return fileId;
	}
	
	public int getVersionId() {
		return versionId;
	}
	
	public String getRenaming() {
		return renaming;
	}
	
	// Override the toString method
	public String toString() {
		return "f" + fileId + "v" + versionId;
	}
	
	// Comparable interface implementation
	public int compareTo(Object fId) throws NullPointerException, ClassCastException {
		if (fId == null)
			throw new NullPointerException();
		
	    if (!(fId instanceof FileInstanceId))
	    	throw new ClassCastException("A FileInstanceId object expected");
	    
	    FileInstanceId fileId = (FileInstanceId)fId;
	    
	    // First compare file identifiers
	    if (fileId.getFileId() != this.getFileId())
	    	return fileId.getFileId() - this.getFileId();
	    // If same file identifier, compare version identifiers
	    else
	    	return fileId.getVersionId() - this.getVersionId();	
	}
	
}
