
package integratedtoolkit.loader;

import java.lang.reflect.Method;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import integratedtoolkit.ITConstants;
import integratedtoolkit.log.Loggers;


public class ITAppLoader {

    private static final Logger logger = Logger.getLogger(Loggers.LOADER);
    private static final boolean debug = logger.isDebugEnabled();

    /**
     * Factored out loading function so that subclasses
     * of ITAppLoader can re-use this code
     *
     * @param chosenLoader
     * @param slaId
     * @param appName
     *
     */
    protected static void load(String chosenLoader, String slaId, String appName, String[] appArgs)
    	throws Exception {

        /* We will have two class loaders:
         * - Custom loader: to avoid conflict with the javassist version of ProActive
         * and the one we use, the custom loader will load our javassist version classes
         * and the classes that use them.
         * - System loader: parent of the custom loader, it will load the ProActive
         * javassist classes and all the rest of the classes (including the one of the
         * application, once it has been modified).
         */
        CustomLoader myLoader = new CustomLoader(new URL[]{});
        
        // Add the jars that the custom class loader needs
        String itLib = System.getenv("IT_LIB");
        myLoader.addFile(itLib + "/javassist/javassist.jar");
        myLoader.addFile(itLib + "/IT.jar");
        //myLoader.addFile(itLib + "/../../gridunawareapps/lib"+"/guapp.jar");

        /* The custom class loader must load the class that will modify the application and
         * invoke the modify method on an instance of this class
         */
        String loaderName = "integratedtoolkit.loader." + chosenLoader + ".ITAppModifier";
        Class modifierClass = myLoader.loadClass(loaderName);
        Object modifier = modifierClass.newInstance();

        logger.info("Loading application " + appName + " with loader " + chosenLoader);

        Class modAppClass;

        if ((slaId == null) || (slaId.trim().length() == 0)) {           
            Method method = modifierClass.getMethod("modify", new Class[]{String.class});
            modAppClass = (Class) method.invoke(modifier, new Object[]{appName});
        }
        else {
        	if (debug)
        		logger.debug("Adding SLA id (" + slaId + ") and app name (" + appName + ")");
            
            // Add this information to system so other
            // components can access them later
            System.setProperty(ITConstants.IT_SLA_ENABLED, "true");
            System.setProperty(ITConstants.IT_SLA_ID, slaId);
            
            // Now modify the application
            Method method = modifierClass.getMethod("modify", new Class[]{String.class, String.class});
            modAppClass = (Class) method.invoke(modifier, new Object[]{appName, slaId});                         
        }

        logger.info("Application " + appName + " instrumented, executing...");
        Method main = modAppClass.getDeclaredMethod("main", new Class[]{String[].class});
        main.invoke(null, new Object[]{appArgs});
    }

    public static void main(String[] args) throws Exception {

        // Configure log4j for the JVM where the loader, the application and the IT API belong
        PropertyConfigurator.configure(System.getProperty(ITConstants.LOG4J));

        if (args.length < 2) {
            logger.fatal("Error: missing arguments for loader");
            System.exit(1);
        }

        // Prepare the arguments and execute the modified application
        String[] appArgs = new String[args.length - 2];
        System.arraycopy(args, 2, appArgs, 0, appArgs.length);

        // Load the application
        load(args[0], null, args[1], appArgs);
    }
    
}
