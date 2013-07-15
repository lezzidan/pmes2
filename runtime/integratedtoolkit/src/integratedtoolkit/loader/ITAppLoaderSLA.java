package integratedtoolkit.loader;

import integratedtoolkit.ITConstants;
import integratedtoolkit.log.Loggers;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author Michael Parkin
 * @date 27-March-2008
 */
public class ITAppLoaderSLA extends ITAppLoader {

    private static final Logger logger = Logger.getLogger(Loggers.LOADER);

    public static void main(String[] args) throws Exception {
        
        // Configure log4j for the JVM where the loader, the application and the IT API belong
        PropertyConfigurator.configure(System.getProperty(ITConstants.LOG4J));

        if (args.length < 3) {
            logger.fatal("Error: missing arguments for loader");
            System.exit(1);
        }

        // Prepare the arguments and execute the modified application
        String[] appArgs = new String[args.length - 3];
        System.arraycopy(args, 3, appArgs, 0, appArgs.length);

        // Load the application
        load(args[0], args[1], args[2], appArgs);
    }
}
