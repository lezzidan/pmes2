
package integratedtoolkit.loader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Random;

import integratedtoolkit.types.annotations.ClassName;
import integratedtoolkit.types.annotations.RealMethodName;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.MethodCall;


public class LoaderUtils {

	// Return the called method if it is in the remote list
    public static Method checkRemote(CtMethod method, Method[] remoteMethods)
      throws NotFoundException {
    	mainLoop: for (Method remote : remoteMethods) {
    		// Check if methods have the same name
    		String nameRemote;
    		Annotation realNameAnnot = remote.getAnnotation(RealMethodName.class);
    		if (realNameAnnot == null)
				nameRemote = remote.getName();
			else
				nameRemote = ((RealMethodName)realNameAnnot).value();
    		if (!nameRemote.equals(method.getName()))
    			continue;
    		
    		// Check if methods belong to the same class
	    	if (!remote.getAnnotation(ClassName.class).value()
	    			.equals(method.getDeclaringClass().getName()))
	    		continue;
	    	
	    	// Check that methods have the same number of parameters
	    	CtClass[] paramClassCurrent = method.getParameterTypes();
	    	Class[] paramClassRemote = remote.getParameterTypes();
	    	if (paramClassCurrent.length != paramClassRemote.length)
	    		continue;
	    	
	    	// Check that parameter types match
	    	for (int i = 0; i < paramClassCurrent.length; i++) {
	    		if (!paramClassCurrent[i].getName()
	    				.equals(paramClassRemote[i].getCanonicalName()))
	    			continue mainLoop;
	    	}
	    	
	    	// Methods match!
	    	return remote;
    	}
    	
    	// The method is not in the remote list
	    return null;
    }
	
    
    // Check whether the method call is a close of a stream
    public static boolean isStreamClose(MethodCall mc) {
    	if (mc.getMethodName().equals("close")) {
    		String fullName = mc.getClassName();
    		if (fullName.startsWith("java.io.")) {
    			String className = fullName.substring(8);
	    		if (   className.equals("FileInputStream")//58,700
	    			|| className.equals("FileOutputStream")//57,700
	    			|| className.equals("InputStreamReader")//61,200
	    			|| className.equals("BufferedReader")//36,400
	    			|| className.equals("FileWriter")//33,900
	    			|| className.equals("PrintWriter")//35,200
	    			|| className.equals("FileReader")//16,800
	    			|| className.equals("OutputStreamWriter")//15,700
	    			|| className.equals("BufferedInputStream")//15,100
	    			|| className.equals("BufferedOutputStream")//10,500
	    			|| className.equals("BufferedWriter")//11,800
	    			|| className.equals("PrintStream")//6,000
	    			|| className.equals("RandomAccessFile")//5,000
	    			|| className.equals("DataInputStream")//7,000
	    			|| className.equals("DataOutputStream")) {//7,000
	    			return true;
	    		}
    		}
    	}
    	return false;
    }
    
    
    // Return a random numeric string
	public static String randomName(int length, String prefix) {
        if (length < 1)
        	return prefix;
        
        Random r = new Random();
        StringBuilder buffer = new StringBuilder();
        int gap = ('9' + 1) - '0';
        
        for (int i = 0; i < length; i++) {
            char c = (char)(r.nextInt(gap) + '0');
            buffer.append(c);
        }
        
        return prefix + buffer.toString();
    }
	
	
	// Check if the method is the main method
	public static boolean isMainMethod(CtMethod m) throws NotFoundException {
		return (m.getName().equals("main")
				&& m.getParameterTypes().length == 1
				&& m.getParameterTypes()[0].getName().equals(String[].class.getCanonicalName()));
	}
	
	
	// Add WithUR to the method name parameter of the executeTask call
	public static StringBuilder replaceMethodName(StringBuilder executeTask, String methodName) {
		String patternStr = ",\"" + methodName + "\",";
		int start = executeTask.toString().indexOf(patternStr);
		int end = start + patternStr.length();
		return executeTask.replace(start, end, ",\"" + methodName + "WithUR\",");
	}

	
	// Add SLA params to the executeTask call
	public static StringBuilder modifyString(StringBuilder executeTask,
									   		 int numParams,
									   		 String appNameParam,
									   		 String slaIdParam,
									   		 String urNameParam,
									   		 String primaryHostParam,
									   		 String transferIdParam) {
    	
		// Number of new params we add
		int newParams = 5;

		// String of new params
		StringBuilder params = new StringBuilder(appNameParam);
		params.append(",");
		params.append(slaIdParam);
		params.append(",");
		params.append(urNameParam);
		params.append(",");
		params.append(primaryHostParam);
		params.append(",");
		params.append(transferIdParam);
		params.append("});");

		String patternStr;

		if (numParams == 0)
			patternStr = 0 + ",null);";
		else
			patternStr = "," + numParams + ",";

		int start = executeTask.toString().indexOf(patternStr);
		int end = start + patternStr.length();

		if (numParams == 0)
			return executeTask.replace(start, end, newParams + ",new Object[]{" + params);
		else {
			executeTask.replace(start + 1, end - 1, Integer.toString(numParams + newParams));
			executeTask.replace(executeTask.length() - 3, executeTask.length(), "");
			return executeTask.append("," + params);
		}
	}
	
}
