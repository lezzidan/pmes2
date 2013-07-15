
package integratedtoolkit.loader.partial;

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
	
	private static final String IT_IMPL = "integratedtoolkit.api.impl.IntegratedToolkitImpl";
	
	private Method[] remoteMethods;
	private String itExeVar;
    private String slaId;
	
	private static final Logger logger = Logger.getLogger(Loggers.LOADER);
	private static final boolean debug = logger.isDebugEnabled();
	
	private static final boolean slaEnabled = System.getProperty(ITConstants.IT_SLA_ENABLED) != null
	  										  && System.getProperty(ITConstants.IT_SLA_ENABLED).equals("true")
	  										  ? true : false;
	
	
	public ITAppEditor(Method[] remoteMethods, String varName) {
        this(remoteMethods, varName, null);
    }
	
	public ITAppEditor(Method[] remoteMethods, String varName, String slaId) {
        super();

        int rml = remoteMethods.length;
        logger.debug("Editing " + rml + " method" + (rml > 1 ? "s" : "") + " for application " + varName);
                   
        this.slaId = slaId;
        this.remoteMethods = remoteMethods;
        this.itExeVar = varName;
    }
	
	
	 // Create the reference to the execution interface after creating the Integrated Toolkit 
    public void edit(NewExpr ne) throws CannotCompileException {
        if (ne.getClassName().equals(IT_IMPL)) {
        	logger.debug("Replacing call to integratedtoolkit.api.impl.IntegratedToolkitImpl");
        	
        	ne.replace("$_ = $proceed($$); " + itExeVar + " = (ITExecution)$_;");
        }
    }

    // Replace calls to remote methods by calls to executeTask
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
    		String methodClass = mc.getClassName();
    		int numParams = declaredMethod.getParameterTypes().length;
    		Annotation[][] paramAnnot = declaredMethod.getParameterAnnotations();
    		
    		if (debug) {
    			logger.debug("Found call to remote method " + methodName + " in " + methodClass);
    			logger.debug("The method has " + numParams + " parameter" + (numParams > 1 ? "s" : ""));
    		}
    		
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
	    			
	    			if (debug)
	    				logger.debug("  Parameter " + (i + 1) + " has type " + ((ParamMetadata) paramAnnot[i][0]).type());
	    			
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
    			logger.debug("Replacing local method call by: " + executeTask.toString() + 
    					     ", with protection in case the call must be run locally");
    		
    		/* Replace the call to the method by the call to executeTask,
    		 * but only if the IT is started and accepts remote tasks.
    		 * Otherwise the method is executed locally.
    		 */
    		mc.replace("if (" + IT_IMPL + ".acceptingTasks()) " +
    				   executeTask.toString() +
    				   "else " +
    				   "$_ = $proceed($$);"
    				  );
    	}
    }
    
}
