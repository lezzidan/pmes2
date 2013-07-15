
package integratedtoolkit.interfaces;

import integratedtoolkit.types.file.FileAccessId;


public interface ITFileAccess {

	// File access modes
	public enum AccessMode {
		R,
		W,
		RW;
	}
	
	// Returns an identifier for the file
	FileAccessId registerFileAccess(String fileName,
									String path,
									String host,
									AccessMode mode);
	
	// Returns true if the file has been accessed by a task before
	boolean alreadyAccessed(String fileName,
							String path,
							String host);
	
}
