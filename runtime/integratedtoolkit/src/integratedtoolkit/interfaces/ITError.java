
package integratedtoolkit.interfaces;


public interface ITError {

	void throwError(String componentName,
					String errorMessage,
					Exception exception);
	
}
