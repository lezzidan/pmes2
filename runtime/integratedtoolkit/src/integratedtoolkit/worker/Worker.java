
package integratedtoolkit.worker;

import java.lang.reflect.Method;


public class Worker {

	// Parameter type constants
	private static final int FILE_PAR 	= 0;
	private static final int BOOL_PAR 	= 1;
	private static final int CHAR_PAR 	= 2;
	private static final int STRING_PAR = 3;
	private static final int BYTE_PAR 	= 4;
	private static final int SHORT_PAR 	= 5;
	private static final int INT_PAR 	= 6;
	private static final int LONG_PAR 	= 7;
	private static final int FLOAT_PAR 	= 8;
	private static final int DOUBLE_PAR = 9;
	
	
	public static void main(String args[]) {
		boolean debug = Boolean.parseBoolean(args[0]);
		String className = args[1];
		String methodName = args[2];
		int numParams = Integer.parseInt(args[3]);
		
		if (args.length < 2*numParams + 4) {
			System.err.println("Insufficient number of arguments");
			System.exit(1);
		}
		
		// Parse the parameter types and values
		int pos = 4;
		Class types[] = new Class[numParams];
		Object values[] = new Object[numParams];
		for (int i = 0; i < numParams; i++) {
			// We need to use wrapper classes for basic types, reflection will unwrap automatically
			switch (Integer.parseInt(args[pos])) {
				case FILE_PAR:
					types[i] = String.class;
					values[i] = args[pos+1];
					break;
				case BOOL_PAR:
					types[i] = boolean.class;
					values[i] = new Boolean(args[pos+1]);
					break;
				case CHAR_PAR:
					types[i] = char.class;
					values[i] = new Character(args[pos+1].charAt(0));
					break;
				case STRING_PAR:
					types[i] = String.class;
					int numSubStrings = Integer.parseInt(args[pos+1]);
					String aux = "";
					for (int j = 2; j <= numSubStrings + 1; j++) {
						aux += args[pos+j];
						if (j < numSubStrings + 1) aux += " ";
					}
					values[i] = aux;
					pos += numSubStrings;
					break;
				case BYTE_PAR:
					types[i] = byte.class;
					values[i] = new Byte(args[pos+1]);
					break;
				case SHORT_PAR:
					types[i] = short.class;
					values[i] = new Short(args[pos+1]);
					break;
				case INT_PAR:
					types[i] = int.class;
					values[i] = new Integer(args[pos+1]);
					break;
				case LONG_PAR:
					types[i] = long.class;
					values[i] = new Long(args[pos+1]);
					break;
				case FLOAT_PAR:
					types[i] = float.class;
					values[i] = new Float(args[pos+1]);
					break;
				case DOUBLE_PAR:
					types[i] = double.class;
					values[i] = new Double(args[pos+1]);
					break;
			}
			pos += 2;
		}
		
		if (debug) {
			// Print request information
			System.out.println("WORKER - Parameters of execution:");
			System.out.println("  * Method class: " + className);
			System.out.println("  * Method name: " + methodName);
			System.out.print("  * Parameter types:");
			for (Class c : types)
				System.out.print(" " + c.getName());
			System.out.println("");
			System.out.print("  * Parameter values:");
			for (Object v : values)
				System.out.print(" " + v);
			System.out.println("");
		}
		
		// Use reflection to get the requested method
		Method method = null;
		try {
			Class methodClass = Class.forName(className);
			method = methodClass.getMethod(methodName, types);
		}
		catch (ClassNotFoundException e) {
			System.err.println("Application class not found");
			System.exit(1);
		}
		catch (SecurityException e) {
			System.err.println("Security exception");
			e.printStackTrace();
			System.exit(1);
		}
		catch (NoSuchMethodException e) {
			System.err.println("Requested method not found");
			System.exit(1);
		}
		
		// Invoke the requested method
		try {
			method.invoke(null, values);
		}
		catch (Exception e) {
			System.err.println("Error invoking requested method");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
