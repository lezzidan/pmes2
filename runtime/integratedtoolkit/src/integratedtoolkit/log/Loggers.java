
package integratedtoolkit.log;


public interface Loggers {

	// Integrated Toolkit
	public static final String IT 		= "integratedtoolkit";
	
	// Loader
	public static final String LOADER 	= IT + ".Loader";
	
	// API
	public static final String API 		= IT + ".API";
	
	// Components
	public static final String ALL_COMP = IT + ".components";
	public static final String TA_COMP 	= ALL_COMP + ".TaskAnalyser";
	public static final String TS_COMP 	= ALL_COMP + ".TaskScheduler";
	public static final String JM_COMP 	= ALL_COMP + ".JobManager";
	public static final String FIP_COMP = ALL_COMP + ".FileInfoProvider";
	public static final String FTM_COMP = ALL_COMP + ".FileTransferManager";
	
	// Worker
	public static final String WORKER 	= IT + ".Worker";
	
}
