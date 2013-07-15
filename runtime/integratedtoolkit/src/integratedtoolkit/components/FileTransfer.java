
package integratedtoolkit.components;

import integratedtoolkit.interfaces.ITFileTransfer;

import integratedtoolkit.types.file.Location;
import integratedtoolkit.types.file.FileAccessId;
import integratedtoolkit.types.file.FileInstanceId;

import java.util.List;


// To request the transfer of a local or remote ﬁle to a destination host
public interface FileTransfer extends ITFileTransfer {
	
	public static final int FILES_READY = 0;
	
	// Different roles for a file involved in an operation
	public enum FileRole {
		JOB_FILE,		// Input file of a job
		OPEN_FILE,		// File to be opened by the application
		RESULT_FILE,	// Result file of the application
		DELETE_FILE,	// Intermediate renamed file to be deleted
		RAW_FILE;		// File which must not be tracked by the FM
	}
	
	/* Returns the identifier of the group of transfers requested,
	 * or FILES_READY if no transfer has been necessary
	 */
	int transferFiles(List<FileAccessId> fileAccesses, Location targetLocation);
	
	// Returns the identifier of the operation (file copy)
	int copyFile(FileInstanceId fId, FileRole role,
				 String targetName, Location targetLocation);
	
	// Transfers a list of result files back to the application host
	void transferBackResultFiles(List<Integer> fileIds);
	
}
