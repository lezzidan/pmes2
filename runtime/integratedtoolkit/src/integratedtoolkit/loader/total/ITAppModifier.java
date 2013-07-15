
package integratedtoolkit.loader.total;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;

import integratedtoolkit.ITConstants;
import integratedtoolkit.loader.LoaderUtils;
import integratedtoolkit.log.Loggers;


public class ITAppModifier {
	
	private static final Logger logger = Logger.getLogger(Loggers.LOADER);
	private static final boolean debug = logger.isDebugEnabled();
	
	
	public Class modify(String appName)
		throws NotFoundException, CannotCompileException, ClassNotFoundException {
		
		return modify(appName, null);
	}
	
	public Class modify(String appName, String slaId)
		throws NotFoundException, CannotCompileException, ClassNotFoundException {
		
	    Class annotItf = Class.forName(appName + "Itf");
	    // Methods declared in the annotated interface
	    Method[] remoteMethods = annotItf.getMethods();
	    
	    // Use the application editor to include the IT API calls on the application code
	    ClassPool cp = ClassPool.getDefault();
	    cp.importPackage("integratedtoolkit.api");
	    cp.importPackage("integratedtoolkit.api.impl");
	    cp.importPackage("integratedtoolkit.loader");
	    cp.importPackage("integratedtoolkit.loader.total");
	    
	    CtClass appClass = cp.get(appName);
	    CtClass itApiClass = cp.get("integratedtoolkit.api.IntegratedToolkit");
	    CtClass itExecClass = cp.get("integratedtoolkit.api.ITExecution");
	    CtClass itSRClass = cp.get("integratedtoolkit.loader.total.StreamRegistry");
	    
	    String varName = LoaderUtils.randomName(5, "it");
	    String itApiVar = varName + "Api";
	    String itExeVar = varName + "Exe";
	    String itSRVar = varName + "SR";
	    CtField itApiField = new CtField(itApiClass, itApiVar, appClass);
	    CtField itExeField = new CtField(itExecClass, itExeVar, appClass);
	    CtField itSRField = new CtField(itSRClass, itSRVar, appClass);
	    itApiField.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
	    itExeField.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
	    itSRField.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
	    appClass.addField(itApiField);
	    appClass.addField(itExeField);
	    appClass.addField(itSRField);
	    
	    // Create IT App Editor
        ITAppEditor itAppEditor = new ITAppEditor(remoteMethods, itExeVar, itSRVar, slaId);
	    
	    /* Find the methods declared in the application class that will be instrumented
	     * - Main
	     * - Constructors
	     * - Methods that are not in the remote list
	     */
	    for (CtMethod m : appClass.getDeclaredMethods()) {
	    	if (LoaderUtils.checkRemote(m, remoteMethods) == null) {
	    		// Not a remote method, we must instrument it
	    		if (debug)
	    			logger.debug("Instrumenting method " + m.getName());
	    		
	    		if (LoaderUtils.isMainMethod(m)) {
	    			logger.debug("Inserting calls at the beginning and at the end of main");
	    			
	    			StringBuilder toInsertBefore = new StringBuilder(),
	    						  toInsertAfter = new StringBuilder();
	    			
	    			if (System.getProperty(ITConstants.IT_SLA_ENABLED) != null
						&& System.getProperty(ITConstants.IT_SLA_ENABLED).equals("true")) {
                    	
	    				logger.debug("Adding UsageRecord.start() and .end() to start and end of main method");
                        
                    	String urClass = "integratedtoolkit.util.ur.UsageRecord";
                        String fileName = System.currentTimeMillis() + "-" + appName + ".ur.xml";
                        String params = "\"" + appName + "\", \"" + slaId + "\", \"" + fileName + "\", \"true\"" + ", \"0\"";
                        toInsertBefore.append(urClass + ".start(" + params + ");");
                        toInsertAfter.append(urClass + ".end();");
                    }
	    			
	    			/* Inserts at the beginning of the application 
	    			 * - Creation of the Integrated Toolkit
	    			 * - Creation of the stream registry to keep track of streams (with error handling)
	    			 * - Setting of the ITExecution interface variable
	    			 * - Start of the Integrated Toolkit
	    			 */
	    			toInsertBefore.append(itApiVar + " = new IntegratedToolkitImpl();")
	    						  .append(itExeVar + " = (ITExecution)" + itApiVar + ";")
	    						  .append(itApiVar + ".startIT();")
	    						  .append("try {")
	    						  .append(itSRVar + " = new StreamRegistry((LoaderAPI) " + itApiVar + " ); }")
	    						  .append("catch (Exception e) {")
	    						  .append("((LoaderAPI) " + itApiVar + ").loaderError(false);")
	    						  .append("System.out.println(\"LOADER ERROR: Cannot initialize stream registry\"); e.printStackTrace(); System.exit(1); }");
	    			logger.debug("To insert before is: \n" + toInsertBefore);
	    			m.insertBefore(toInsertBefore.toString());
	    			
	    			/* Inserts at the end of the application:
	    			 * - Stop of the Integrated Toolkit
	    			 * - End of application notification to the stream registry
	    			 * - Exit to make the JVM finish at the end of the application
	    			 */
	    			toInsertAfter.insert(0, itApiVar + ".stopIT(true); " +
	    						  			itSRVar + ".endOfApplication();");
	    			toInsertAfter.append("System.exit(0);");
	    			logger.debug("To insert after is: \n" + toInsertAfter);
	    			m.insertAfter(toInsertAfter.toString(), true);
	    		}
	    		
	    		m.instrument(itAppEditor);
	    	}
	    }
	    // Instrument constructors
	    for (CtConstructor c : appClass.getDeclaredConstructors()) {
	    	logger.debug("Instrumenting constructor " + c.getLongName());
	    	c.instrument(itAppEditor);
	    }
	    
	    /* Load the modified class into memory and return it.
	     * Generally, once a class is loaded into memory no further modifications
	     * can be performed on it.
	     */
	    return appClass.toClass();
	}

}
