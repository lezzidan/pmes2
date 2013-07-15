
package integratedtoolkit.util;


public class ElementNotFoundException extends Exception {

	public ElementNotFoundException() {
		super("Cannot find the requested element");
	}
	
	public ElementNotFoundException(String message) {
		super(message);
	}
	
}
