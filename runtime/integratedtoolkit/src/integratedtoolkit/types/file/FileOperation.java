
package integratedtoolkit.types.file;

import java.lang.reflect.Method;
import java.util.List;
import java.util.LinkedList;

import integratedtoolkit.components.FileTransfer.FileRole;

import org.gridlab.gat.io.File;
import org.gridlab.gat.io.LogicalFile;


public abstract class FileOperation {
	
	public enum OpEndState {
		OP_OK,
		OP_IN_PROGRESS,
		OP_FAILED,
		OP_PREPARATION_FAILED;
	}
	
	// Identifiers of the groups to which the operation belongs
    private List<Integer> groupIds;
	// Identifier of the file version affected by the operation
	private FileInstanceId fId;
	// Role of the file involved in the operation
	private FileRole role;
	// Method to be invoked when operation finishes
	private Method postProcess;
	// State the operation has finished with
	private OpEndState endState;
	// Possibly thrown exception
	private Exception exception;
	
	
	public FileOperation(FileInstanceId fId, int groupId, FileRole role, Method postProcess) {
		this.fId = fId;
		this.groupIds = new LinkedList<Integer>();
		this.groupIds.add(groupId);
		this.role = role;
		this.postProcess = postProcess;
	}
	
	public FileOperation(FileInstanceId fId, List<Integer> groupIds, FileRole role, Method postProcess) {
		this.fId = fId;
		this.groupIds = groupIds;
		this.role = role;
		this.postProcess = postProcess;
	}
	
	public List<Integer> getGroupIds() {
		return groupIds;
	}
	
	public FileInstanceId getFileInstanceId() {
		return fId;
	}
	
	public FileRole getRole() {
		return role;
	}
	
	public Method getPostProcessMethod() {
		return postProcess;
	}
	
	public OpEndState getEndState() {
		return endState;
	}
	
	public Exception getException() {
		return exception;
	}
	
	public void addGroupId(int groupId) {
		this.groupIds.add(groupId);
	}
	public void setEndState(OpEndState state) {
		this.endState = state;
	}
	
	public void setException(Exception e) {
		this.exception = e;
	}
	
	
	public static class Copy extends FileOperation {
		private LogicalFile lf;
		private String targetName;
		private Location targetLocation;
		
		public Copy(FileInstanceId fId,
					int gId,
					FileRole role,
					LogicalFile lf,
					String targetName,
					Location targetLoc,
					Method postProcess) {
			
			super(fId, gId, role, postProcess);
			
			this.lf = lf;
			this.targetName = targetName;
			this.targetLocation = targetLoc;
		}
		
		public LogicalFile getLogicalFile() {
			return lf;
		}
		
		public String getTargetName() {
			return targetName;
		}
			
		public Location getTargetLocation() {
			return targetLocation;
		}
		
	}
	
	
	public static class Delete extends FileOperation {
		private File file;
		
		public Delete(int gId,
					  FileRole role,
					  File file,
					  Method postProcess) {
			
			super(null, gId, role, postProcess);
			
			this.file = file;
		}
		
		public File getFile() {
			return file;
		}
		
	}
	
}
