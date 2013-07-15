
package integratedtoolkit.types;

import java.io.Serializable;

import integratedtoolkit.api.ITExecution.*;
import integratedtoolkit.types.file.FileAccessId;


public class Parameter implements Serializable {
	
	// Parameter fields
	private ParamType type;
	private ParamDirection direction;
	
	
	public Parameter (ParamType type, ParamDirection direction) {
		this.type = type;
		this.direction = direction;
	}
	
	public ParamType getType() {
		return type;
	}
	
	public ParamDirection getDirection() {
		return direction;
	}
	
	
	public static class FileParameter extends Parameter {
		
		// File parameter fields
		private String fileName;
		private String path;
		private String host;
		private FileAccessId faId;
		
		
		public FileParameter(ParamType type,
							 ParamDirection direction,
							 String name,
							 String path,
							 String host) {
			
			super(type, direction);
			
			this.fileName = name;
			this.path = path;
			this.host = host;
			this.faId = null;
		}
		
		public String getName() {
			return fileName;
		}
		
		public String getPath() {
			return path;
		}
		
		public String getHost() {
			return host;
		}
		
		public FileAccessId getFileAccessId() {
			return faId;
		}
		
		public void setName(String name) {
			this.fileName = name;
		}
		
		public void setFileAccessId(FileAccessId faId) {
			this.faId = faId;
		}
		
		public String toString() {
			return 	 getHost() + ":"
				   + getPath()
				   + getName() + " "
				   + getType() + " "
				   + getDirection();
		}
	}
	
	
	public static class BasicTypeParameter extends Parameter {
		/* Basic type parameter can be:
		 * - boolean
		 * - char
		 * - String
		 * - byte
		 * - short
		 * - int
		 * - long
		 * - float
		 * - double
		 */
		
		private Object value;
		
		public BasicTypeParameter(ParamType type,
								  ParamDirection direction,
								  Object value) {
			super(type, direction);
			this.value = value;
		}
		
		public Object getValue() {
			return value;
		}
		
		public void setValue(Object value) {
			this.value = value;
		}
		
		public String toString() {
			return 	 value + " "
				   + getType() + " "
				   + getDirection();
		}	
	}
	
}
