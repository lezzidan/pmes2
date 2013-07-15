
package integratedtoolkit;

public interface ITConstants {
	
	// Component names
	public static final String IT					= "integratedtoolkit.IntegratedToolkit";
	public static final String TA 					= "Task Analyser";
	public static final String TS 					= "Task Scheduler";
	public static final String JM 					= "Job Manager";
	public static final String FM 					= "File Manager";
	public static final String FIP 					= "File Information Provider";
	public static final String FTM 					= "File Transfer Manager";
	
	// Dynamic system properties
	public static final String IT_APP_NAME 			= "it.appName";
	public static final String IT_DEPLOYMENT 		= "it.deployment";
	public static final String GAT_BROKER_ADAPTOR	= "it.gat.broker.adaptor";
	public static final String GAT_FILE_ADAPTOR   	= "it.gat.file.adaptor";
	public static final String IT_PROJ_FILE 		= "it.project.file";
	public static final String IT_RES_FILE 			= "it.resources.file";
	public static final String IT_CONSTR_FILE 		= "it.constraints.file";
	public static final String IT_HIST_FILE			= "it.hist.file";
	public static final String IT_SLA_ENABLED		= "it.slaEnabled";
	public static final String IT_SLA_ID			= "it.slaId";
	public static final String IT_LOCATIONS			= "it.locations";
	public static final String IT_LANG			 	= "it.lang";
	public static final String LOG4J		 		= "log4j.configuration";
	
	// Deployment
	public static final String IT_JVM 				= "ITJvm";
	
	// Initialisation
	public static final String INIT_OK 				= "OK";
	
	// Resources
	public static final String IT_RES_SCHEMA 		= "/xml/resources/resource_schema.xsd";
	
	// Projects
	public static final String IT_PROJ_SCHEMA 		= "/xml/projects/project_schema.xsd";
	
	// Historical
        public static final String IT_HIST_SCHEMA             = "/xml/historical/historical_schema.xsd";

	// GAT
	public static final String GAT_ADAPTOR		= "gat.adaptor.path";
	public static final String GAT_ADAPTOR_LOC 		= "/lib/adaptors";
	public static final String GAT_DEBUG			= "gat.debug";
	
	//Minimum update threshold of speed matrix
	public static final Integer NET_SPEED_THRESHOLD	= 30; //In MBytes	

	// Project properties
	public static final String INSTALL_DIR 		= "InstallDir";
	public static final String WORKING_DIR 		= "WorkingDir";
	public static final String USER 			= "User";
	public static final String LIMIT_OF_TASKS 		= "LimitOfTasks";
	public static final String LIMIT_OF_JOBS 		= "LimitOfJobs";
	public static final String MAX_CLUSTER_SIZE 		= "MaxClusterSize";

	public static final String NET_SPEED 		= "NetSpeed";
	public static final String DEFAULT_NET_SPEED		= "100"; //in Mbps	

	//Runtime properties
	public static final String OLDAVAIL_WEIGHT            = "0.4"; //40% of weight on availability ranking
        public static final String NEWAVAIL_WEIGHT            = "0.6"; //60% of weight on availability ranking
	public static final String DEFAULT_MAX_RETRIES	= "5"; //Maximum of retries before leave worker out of the run.
	public static final String CONFIDENCE_INC_STEP	= "0.05"; //Increment step of 5% in availability confidence.

}	



