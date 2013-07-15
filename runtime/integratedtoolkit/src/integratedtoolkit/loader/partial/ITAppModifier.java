
package integratedtoolkit.loader.partial;

import java.lang.reflect.Method;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;

import org.apache.log4j.Logger;

import integratedtoolkit.ITConstants;
import integratedtoolkit.loader.LoaderUtils;
import integratedtoolkit.log.Loggers;


public class ITAppModifier {

    private static final Logger logger = Logger.getLogger(Loggers.LOADER);
    private static final boolean debug = logger.isDebugEnabled();
    
	/**
     *
     * @param appName
     * @return
     * @throws java.lang.Exception
     */
    public Class modify(String appName)
            throws NotFoundException, CannotCompileException, ClassNotFoundException {
        return modify(appName, null);
    }

    /**
     *
     * @param appName
     * @param slaId
     * @return
     * @throws java.lang.Exception
     */
    public Class modify(String appName, String slaId)
            throws NotFoundException, CannotCompileException, ClassNotFoundException {

       // Use the application editor to include the IT API calls on the application code
        ClassPool cp = ClassPool.getDefault();
        cp.importPackage("integratedtoolkit.api");

        CtClass appClass = cp.get(appName);
        CtClass itExecClass = cp.get("integratedtoolkit.api.ITExecution");

        String varName = LoaderUtils.randomName(5, "it");
        String itExeVar = varName + "Exe";
        CtField itExeField = new CtField(itExecClass, itExeVar, appClass);
        itExeField.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
        appClass.addField(itExeField);

        // The annotated interface for the application
        Class annotItf = Class.forName(appName + "Itf");

        // Remote methods declared in the annotated interface
        // (a remote method is a method that is declared in the interface)
        Method[] remoteMethods = annotItf.getMethods();
        int rml = remoteMethods.length;
        
        // Methods declared in the application
        CtMethod[] declaredMethods = appClass.getDeclaredMethods();
        int dml = declaredMethods.length;
        
        if (debug) {
        	logger.debug("There " + (rml > 1 ? "are " : "is ") + rml + " remote method" + (rml > 1 ? "s" : ""));
        	for (Method m : remoteMethods)
        		logger.debug("  " + m.toGenericString());
        	
        	logger.debug("There" + (dml > 1 ? " are " : " is ") + dml + " declared method" + (dml > 1 ? "s" : "") + " in " + appName);
        }

        // Create IT App Editor
        ITAppEditor itAppEditor = new ITAppEditor(remoteMethods, itExeVar, slaId);

        /* Find the methods declared in the application class that will be instrumented
         * - Main
         * - Constructors
         * - Methods that are not in the remote list
         */
        for (CtMethod m : declaredMethods) {
            if (LoaderUtils.checkRemote(m, remoteMethods) == null) {
                // Not defined in the interface, we must instrument it
            	if (debug)
            		logger.debug("Instrumenting method " + m.getName() + " with " + itExeVar);

                // IT AppEditor specifies how to modify the methods
                m.instrument(itAppEditor);

                if (LoaderUtils.isMainMethod(m)) {
                    if (System.getProperty(ITConstants.IT_SLA_ENABLED) != null
                    	&& System.getProperty(ITConstants.IT_SLA_ENABLED).equals("true")) {
                    	logger.debug("Adding UsageRecord.start() and .end() to start and end of main method");
                        
                    	String urClass = "integratedtoolkit.util.ur.UsageRecord";
                        String fileName = System.currentTimeMillis() + "-" + appName + ".ur.xml";
                        String params = "\"" + appName + "\", \"" + slaId + "\", \"" + fileName + "\", \"true\"" + ", \"0\"";
                        m.insertBefore(urClass + ".start(" + params + ");");
                        m.insertAfter(urClass + ".end();", true);
                    }
                    
                    logger.debug("Inserting System.exit(0) at the end of main");
                    
                    m.insertAfter("System.exit(0);", true);
                }
            }
        }

        for (CtConstructor c : appClass.getDeclaredConstructors()) {
            logger.debug("Instrumenting constructor " + c.getLongName());
            c.instrument(itAppEditor);
        }

        return appClass.toClass();
    }
    
}
