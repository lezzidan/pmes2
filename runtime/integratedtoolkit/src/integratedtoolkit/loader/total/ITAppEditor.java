
package integratedtoolkit.loader.total;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import integratedtoolkit.ITConstants;
import integratedtoolkit.loader.LoaderUtils;
import integratedtoolkit.log.Loggers;
import integratedtoolkit.types.annotations.ParamMetadata;


public class ITAppEditor extends ExprEditor {
	
	private Method[] remoteMethods;
	private String itExeVar;
	private String itSRVar;
	private String slaId;
	
	private static final Logger logger = Logger.getLogger(Loggers.LOADER);
	private static final boolean debug = logger.isDebugEnabled();
	
	private static final boolean slaEnabled = System.getProperty(ITConstants.IT_SLA_ENABLED) != null
	  										  && System.getProperty(ITConstants.IT_SLA_ENABLED).equals("true")
	  										  ? true : false;
	
	
	public ITAppEditor(Method[] remoteMethods, String itExeVar, String itSRVar) {
        this(remoteMethods, itExeVar, itSRVar, null);
    }
	
	public ITAppEditor(Method[] remoteMethods, String itExeVar, String itSRVar, String slaId) {
		super();
		this.remoteMethods = remoteMethods;
		this.itExeVar = itExeVar;
		this.itSRVar = itSRVar;
		this.slaId = slaId;
	}
	
	
	// Instrument the creation of streams and stream wrappers
    public void edit(NewExpr ne) throws CannotCompileException {
    	String fullName = ne.getClassName();
    	if (fullName.startsWith("java.io.")) {
    		String className = fullName.substring(8);
    		
    		if (debug)
    			logger.debug("Inspecting the creation of an object of class " + className);
    		
    		// $$ = pars separated by commas, $args = pars in an array of objects
        	if (className.equals("FileInputStream"))
            	ne.replace("$_ = " + itSRVar + ".newFileInputStream($$);");
        	else if (className.equals("FileOutputStream"))
            	ne.replace("$_ = " + itSRVar + ".newFileOutputStream($$);");
        	else if (className.equals("InputStreamReader"))
            	ne.replace("$_ = " + itSRVar + ".newInputStreamReader($$);");
        	else if (className.equals("BufferedReader"))
            	ne.replace("$_ = " + itSRVar + ".newBufferedReader($$);");
        	else if (className.equals("FileWriter"))
            	ne.replace("$_ = " + itSRVar + ".newFileWriter($$);");
        	else if (className.equals("PrintWriter"))
            	ne.replace("$_ = " + itSRVar + ".newPrintWriter($$);");
        	else if (className.equals("FileReader"))
            	ne.replace("$_ = " + itSRVar + ".newFileReader($$);");
        	else if (className.equals("OutputStreamWriter"))
            	ne.replace("$_ = " + itSRVar + ".newOutputStreamWriter($$);");	
        	else if (className.equals("BufferedWriter"))
            	ne.replace("$_ = " + itSRVar + ".newBufferedWriter($$);");
        	else if (className.equals("PrintStream"))
            	ne.replace("$_ = " + itSRVar + ".newPrintStream($$);");
        	else if (className.equals("RandomAccessFile"))
            	ne.replace("$_ = " + itSRVar + ".newRandomAccessFile($$);");
        	else
        		ne.replace("$_ = $proceed($$); " +
            			   "if ($_ instanceof java.io.FilterInputStream || $_ instanceof java.io.FilterOutputStream) {" +
            				itSRVar + ".newFilterStream((Object)$1, (Object)$_); }");
    	}
    }
	
    
	/* Replace calls to remote methods by calls to executeTask
	 * Instrument the close of streams
	 */
    public void edit(MethodCall mc) throws CannotCompileException {
    	Method declaredMethod = null;
		try {
			declaredMethod = LoaderUtils.checkRemote(mc.getMethod(), remoteMethods);
		}
		catch (NotFoundException e) {
			throw new CannotCompileException(e);
		}
    	
    	if (declaredMethod != null) { // Current method must be executed remotely, change the call
    		String methodName = mc.getMethodName();
    		
    		if (debug)
    			logger.debug("Found call to remote method " + methodName);
    		
    		String methodClass = mc.getClassName();
			int numParams = declaredMethod.getParameterTypes().length;
			Annotation[][] paramAnnot = declaredMethod.getParameterAnnotations();
			
    		//	Build the executeTask call string
			StringBuilder executeTask = new StringBuilder();
    		executeTask.append(itExeVar).append(".executeTask(");
    		executeTask.append("\"").append(methodClass).append("\"").append(",");
    		executeTask.append("\"").append(methodName).append("\"").append(",");
    		executeTask.append(numParams);
    		
    		if (numParams == 0) {
    			executeTask.append(",null);");
    		}
    		else {
    			executeTask.append(",new Object[]{");
    		
	    		for (int i = 0; i < paramAnnot.length; i++) {
	    			String type = null, direction = null;
	    			
	    			/* Append the value of the current parameter according to the type.
	    			 * Basic types must be wrapped by an object first
	    			 */
	    			switch (((ParamMetadata)paramAnnot[i][0]).type()) {
						case FILE:
							type = "ITExecution.ParamType.FILE_T";
							executeTask.append("$").append(i+1).append(",");
							break;
						case BOOLEAN:
							type = "ITExecution.ParamType.BOOLEAN_T";
							executeTask.append("new Boolean(").append("$").append(i+1).append("),");
							break;
						case CHAR:
							type = "ITExecution.ParamType.CHAR_T";
							executeTask.append("new Character(").append("$").append(i+1).append("),");
							break;
						case STRING:
							type = "ITExecution.ParamType.STRING_T";
							executeTask.append("$").append(i+1).append(",");
							break;
						case BYTE:
							type = "ITExecution.ParamType.BYTE_T";
							executeTask.append("new Byte(").append("$").append(i+1).append("),");
							break;
						case SHORT:
							type = "ITExecution.ParamType.SHORT_T";
							executeTask.append("new Short(").append("$").append(i+1).append("),");
							break;
						case INT:
							type = "ITExecution.ParamType.INT_T";
							executeTask.append("new Integer(").append("$").append(i+1).append("),");
							break;
						case LONG:
							type = "ITExecution.ParamType.LONG_T";
							executeTask.append("new Long(").append("$").append(i+1).append("),");
							break;
						case FLOAT:
							type = "ITExecution.ParamType.FLOAT_T";
							executeTask.append("new Float(").append("$").append(i+1).append("),");
							break;
						case DOUBLE:
							type = "ITExecution.ParamType.DOUBLE_T";
							executeTask.append("new Double(").append("$").append(i+1).append("),");
							break;
					}
	    			
	    			switch (((ParamMetadata)paramAnnot[i][0]).direction()) {
						case IN:
							direction = "ITExecution.ParamDirection.IN";
							break;
						case OUT:
							direction = "ITExecution.ParamDirection.OUT";
							break;
						case INOUT:
							direction = "ITExecution.ParamDirection.INOUT";
							break;
						default: // null
							direction = "ITExecution.ParamDirection.IN";
							break;
	    			}
	    			
	    			// Append the type and the direction of the current parameter
	    			executeTask.append(type).append(",");
	    			executeTask.append(direction);
	    			if (i < paramAnnot.length -1)
	    				executeTask.append(",");
	    		}
	    		
	    		executeTask.append("});");
	    	}
	    	
    		if (slaEnabled) {
    			if (debug)
    				logger.debug("Changing method call to the UR enabled method...");
    			
    			executeTask = LoaderUtils.replaceMethodName(executeTask, methodName);

    			// 1st param is the appName
    			String appNameParam = "\"" + itExeVar + "\"" +
    								  ",ITExecution.ParamType.STRING_T,ITExecution.ParamDirection.IN";

    			// 2nd param is the slaId
    			String slaIdParam = "\"" + slaId + "\"" +
    								",ITExecution.ParamType.STRING_T,ITExecution.ParamDirection.IN";

    			// 3rd param is the usage record which is sent as an out file
    			// PLACEHOLDER VALUE AS THE CORRECT VALUE IS ASSIGNED
    			// IN THE TASK ANALYSER WHEN THE TASK IS LAUNCHED
    			String urNameParam = "\"dummy\"" +
    								 ",ITExecution.ParamType.FILE_T,ITExecution.ParamDirection.OUT";

    			// 4th param tells us if this is the main task (which it isn't, so always false)
    			String primaryHostParam = "\"false\"" +
    									  ",ITExecution.ParamType.STRING_T,ITExecution.ParamDirection.IN";

    			// 5th param is the id of the transfers of the job, useful for the generation of the trace (0 is default, no transfer)
    			String transferIdParam = "\"0\"" +
    									  ",ITExecution.ParamType.STRING_T,ITExecution.ParamDirection.IN";
    			
    			executeTask = LoaderUtils.modifyString(executeTask,
    												   numParams,
    												   appNameParam,
    												   slaIdParam,
    												   urNameParam,
    												   primaryHostParam,
    												   transferIdParam);
    		}
    		
    		if (debug)
    			logger.debug("Replacing local method call by: " + executeTask.toString());
    		
    		// Replace the call to the method by the call to executeTask
    		mc.replace(executeTask.toString());
    	}
    	else if (LoaderUtils.isStreamClose(mc)) {
    		if (debug)
    			logger.debug("Replacing close on a stream of class " + mc.getClassName());
    		
    		// Close call on a stream
    		mc.replace("$_ = $proceed($$); " + itSRVar + ".streamClosed($0);");
    	}
    }
    
}
