
package integratedtoolkit.api;


public interface ITExecution {

	// Parameter types
	public enum ParamType {
		FILE_T,
		BOOLEAN_T,
		CHAR_T,
		STRING_T,
		BYTE_T,
		SHORT_T,
		INT_T,
		LONG_T,
		FLOAT_T,
		DOUBLE_T;
		// OBJECT_T;
	}

	// Parameter directions
	public enum ParamDirection {
		IN,
		OUT,
		INOUT;
	}
	
	public void executeTask(String methodClass,
							String methodName,
							int parameterCount,
							Object... parameters);
	
}
