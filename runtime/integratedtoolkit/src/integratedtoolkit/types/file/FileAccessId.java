
package integratedtoolkit.types.file;

import java.io.Serializable;


public abstract class FileAccessId implements Serializable {

	public abstract int getFileId();
	
	
	// Read access
	public static class RAccessId extends FileAccessId {
	
		// File version read
		private FileInstanceId readFileInstance;
		
		public RAccessId() { }
		
		public RAccessId(int fileId, int RVersionId) {
			this.readFileInstance = new FileInstanceId(fileId, RVersionId);
		}
		
		public int getFileId() {
			return readFileInstance.getFileId();
		}
		
		public int getRVersionId() {
			return readFileInstance.getVersionId();
		}
		
		public FileInstanceId getReadFileInstance() {
			return readFileInstance;
		}
		
		public String toString() {
			return "Read file: " + readFileInstance;
		}
		
	}
	
	// Write access
	public static class WAccessId extends FileAccessId {
	
		// File version written
		private FileInstanceId writtenFileInstance;
		
		public WAccessId() { }
		
		public WAccessId(int fileId, int WVersionId) {
			this.writtenFileInstance = new FileInstanceId(fileId, WVersionId);
		}
		
		public int getFileId() {
			return writtenFileInstance.getFileId();
		}
		
		public int getWVersionId() {
			return writtenFileInstance.getVersionId();
		}
		
		public FileInstanceId getWrittenFileInstance() {
			return writtenFileInstance;
		}
		
		public String toString() {
			return "Written file: " + writtenFileInstance;
		}
		
	}
	
	
	// Read-Write access
	public static class RWAccessId extends FileAccessId {
	
		// File version read
		private FileInstanceId readFileInstance;
		// File version written
		private FileInstanceId writtenFileInstance;
		
		public RWAccessId() { }
		
		public RWAccessId(int fileId, int RVersionId, int WVersionId) {
			this.readFileInstance = new FileInstanceId(fileId, RVersionId);
			this.writtenFileInstance = new FileInstanceId(fileId, WVersionId);
		}
		
		public int getFileId() {
			return readFileInstance.getFileId();
			// or return writtenFileInstance.getFileId();
		}
		
		public int getRVersionId() {
			return readFileInstance.getVersionId();
		}
		
		public int getWVersionId() {
			return writtenFileInstance.getVersionId();
		}
		
		public FileInstanceId getReadFileInstance() {
			return readFileInstance;
		}
		
		public FileInstanceId getWrittenFileInstance() {
			return writtenFileInstance;
		}
		
		public String toString() {
			return "Read file: " + readFileInstance + ", Written file: " + writtenFileInstance;
		}
		
	}
	
}
