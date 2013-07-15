
package integratedtoolkit.types;

import integratedtoolkit.log.Loggers;

import java.io.Serializable;
import java.util.TreeMap;

import org.apache.log4j.Logger;


public class Method implements Serializable {

	// Method id management
	private static int nextId = 0;
	private static TreeMap<String,Integer> signatureToId = new TreeMap<String,Integer>();
	
	// Method attributes
	private int methodId;
	private String declaringClass;
	private String methodName;
	private Parameter[] parameters;
	
	
	public static int newId(String methodClass, String methodName, Parameter[] parameters) {
		StringBuilder buffer = new StringBuilder();
		
		buffer.append(methodName).append("(");
		int numPars = parameters.length;
		if (numPars > 0) {
			buffer.append(parameters[0].getType());
			for (int i = 1; i < numPars; i++)
				buffer.append(",").append(parameters[i].getType());
		}
		buffer.append(")").append(methodClass);
		
		String signature = buffer.toString();
		Integer id = signatureToId.get(signature);
		
		if (id == null) {
			// First access to this method, store and return the new id
			signatureToId.put(signature, nextId);
			return nextId++;
		}
		else
			// The method has been accessed before, return its id
			return id;
	}
	
	// Java method
	public Method(String methodClass, String methodName, Parameter[] parameters) {
		this.methodId = newId(methodClass, methodName, parameters);
		this.declaringClass = methodClass;
		this.methodName = methodName;
		this.parameters = parameters;
	}
	
	// C method
	public Method(int methodId, Parameter[] parameters) {
		this.methodId = methodId;
		this.parameters = parameters;
		this.methodName = Integer.toString(methodId);
	}
	
	public int getId() {
		return methodId;
	}
	
	public String getDeclaringClass() {
		return declaringClass;
	}
	
	public String getName() {
		return methodName;
	}
	
	public Parameter[] getParameters() {
		return parameters;
	}
	
	public String toString() {
		StringBuilder buffer = new StringBuilder();

		buffer.append("[Method id: ").append(getId()).append("]");
		buffer.append(", [Method class: ").append(getDeclaringClass()).append("]");
		buffer.append(", [Method name: ").append(getName()).append("]");
		buffer.append(", [Method id: ").append(getId()).append("]");
		buffer.append(", [Parameters:");

		for (Parameter p : getParameters())
			buffer.append(" [").append(p.toString()).append("]");
		
		buffer.append("]");
		
		return buffer.toString();
	}
	
}
