
package integratedtoolkit.components.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Iterator;

import gnu.trove.map.hash.*;

import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import org.objectweb.fractal.api.control.BindingController;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.RunActive;
import org.objectweb.proactive.Service;
import org.objectweb.proactive.core.util.wrapper.StringWrapper;

import org.gridlab.gat.GAT;
import org.gridlab.gat.GATContext;
import org.gridlab.gat.URI;
import org.gridlab.gat.io.File;

import integratedtoolkit.ITConstants;
import integratedtoolkit.components.FileAccess;
import integratedtoolkit.components.FileInformation;
import integratedtoolkit.components.FileInfoUpdate;
import integratedtoolkit.components.Preparation;
import integratedtoolkit.components.SchedulerUpdate;
import integratedtoolkit.util.ProjectManager;
import integratedtoolkit.interfaces.ITError;
import integratedtoolkit.log.Loggers;
import integratedtoolkit.types.file.*;
import integratedtoolkit.types.file.FileAccessId.*;


public class FileInfoProvider implements FileAccess, FileInformation, Preparation,
										 BindingController,
										 RunActive {
	
	// Client interfaces
	private FileInfoUpdate newVersion;
	private ITError errorReport;
	private SchedulerUpdate schedFIUpdate;

	// GAT context
        private GATContext context;

        // GAT broker adaptor information
        private boolean userNeeded;
        private boolean usingGlobus;

	// Map: filename:host:path -> file identifier
	private Map<String,Integer> nameToId;

	// Map: file identifier -> file information
	private Map<Integer,FileInfo> idToFile;

	// Map: Original registered filenames of IN direction files -> file identifier
	private Map<String,Integer> originalNameToId;

	// Map: Original registered filenames of IN direction files -> last modification date of file
	private Map<String,Long> fileLastModDate;
	
	// Component logger - No need to configure, ProActive does
	private static final Logger logger = Logger.getLogger(Loggers.FIP_COMP);
	private static final boolean debug = logger.isDebugEnabled();
	
	//ProjectManager
        private ProjectManager projManager;
	private static final String PROJ_LOAD_ERR                         = "Error loading project information";	

	//Locations storing system availability (On/Off)
        private static final boolean locationsOn = System.getProperty(ITConstants.IT_LOCATIONS) != null
                                                                          && System.getProperty(ITConstants.IT_LOCATIONS).equals("true")
                                                                          ? true : false;
        // Changed Files flag respect to the last tun
        boolean  firstChangedFileDetected = false;
        
        //Performance debug
         private boolean  timeMeasuring = false;
	 private double timeInSizeCalc = 0;
	 private double timeGetFileLoc = 0;
	 private double timeStoreLoc = 0;

	public FileInfoProvider() { }
	
	
	// RunActive interface
	
	public void runActivity(Body body) {
		body.setImmediateService("terminate", new Class[] {});
		
		Service service = new Service(body);
		service.fifoServing();
	}
	
	// Server interfaces implementation
	
	// Preparation interface
	
	public StringWrapper initialize() {

		logger.debug("Start Initializing FIP");

		// GAT adaptor path
                System.setProperty(ITConstants.GAT_ADAPTOR,
                                                   System.getenv("GAT_LOCATION") + ITConstants.GAT_ADAPTOR_LOC);

                if (context == null) {
                        context = new GATContext();
                        String brokerAdaptor = System.getProperty(ITConstants.GAT_BROKER_ADAPTOR),
                               fileAdaptor   = System.getProperty(ITConstants.GAT_FILE_ADAPTOR);
                        context.addPreference("ResourceBroker.adaptor.name", brokerAdaptor);
                        context.addPreference("File.adaptor.name", fileAdaptor + ", local");
                        usingGlobus = brokerAdaptor.equalsIgnoreCase("globus");
                        userNeeded = brokerAdaptor.regionMatches(true, 0, "ssh", 0, 3);
                }


		if (nameToId == null)
			 nameToId = new THashMap<String,Integer>();
		else
			nameToId.clear();

		if (idToFile == null)
			idToFile = new THashMap<Integer,FileInfo>();
		else
			idToFile.clear();

		if (originalNameToId == null)
			originalNameToId = new THashMap<String,Integer>();
		else
			originalNameToId.clear();

	        if(locationsOn){
		  if (fileLastModDate == null)
			 fileLastModDate = new THashMap<String,Long>();
                  else
                          fileLastModDate.clear();
	        }

	        if(projManager == null)
		 {
		   try {
                         projManager = new ProjectManager();
                       }
                       catch (Exception e) {
                          logger.error(PROJ_LOAD_ERR, e);
                          return new StringWrapper(ITConstants.JM + ": " + PROJ_LOAD_ERR);
                       }
		}
	
		FileInfo.init();
		
		logger.info("Initialization finished");

		if(locationsOn)
		  logger.info("Locations system enabled");
		else
		  logger.info("Locations system disabled");    		

		return new StringWrapper(ITConstants.INIT_OK);
	}
	
	
	public void cleanup() {

                //Storing locations of IN direction files
		if(locationsOn){
		  storeLocations();
		}

            //Debug
          if(timeMeasuring){
	    logger.info(" ");
	    logger.info("Debug run time of functions (in seconds) : ");
	    logger.info("Total time on getFileSizeAndLastMod -> "+timeInSizeCalc/1000);
	    logger.info("Total time on getFileLocations -> "+ timeGetFileLoc/1000);
	    logger.info("Total time on storeLocations -> "+ timeStoreLoc/1000);
            logger.info(" ");
          }
	    logger.info("Cleanup done");
	}	
	
	
	// FileAccess interface
	
	public FileAccessId registerFileAccess(
			String fileName,
			String path,
			String host,
			AccessMode mode) {
		
		FileInfo fileInfo;
		String locationKey = fileName + ":" + host + ":" + path;
		Integer fileId = nameToId.get(locationKey);
		List<Location> locations = new LinkedList<Location>();
                double iTimeStamp = 0;
		
		//Adding file initial location
		locations.add(new Location(host, path));		 		

		// First access to this file
		if (fileId == null) {

			if (debug){
			  logger.debug("FIRST access to " + host + ":" + path + fileName);
			}

			// Update mappings
			fileInfo = new FileInfo(fileName, host, path);
			fileId = fileInfo.getFileId();
			nameToId.put(locationKey, fileId);
			idToFile.put(fileId, fileInfo);

			//Updating registered IN files list
			if(mode == AccessMode.R){
                          originalNameToId.put(fileName,fileId);
			}

		       //Calculating the actual file size and modification date
                        List<Long> sizeLastModSet = getFileSizeAndLastMod(fileInfo.getOriginalName(),host,path);
		
                        long size = sizeLastModSet.get(0);

                        //Consulting the last modification date of file
                        long lastMod = sizeLastModSet.get(1);

                        //Reading the last stored modification date
		        long previous_lastMod = projManager.getFileLocationsLastMod(fileInfo.getOriginalName());
                        
                        if(debug){
			 logger.debug("");
                         logger.debug("File Size: "+fileInfo.getOriginalName()+" | Size -> "+size+" bytes");
                         logger.debug("Actual Run File Modification Date: "+fileInfo.getOriginalName()+" | Date -> "+lastMod);
                         logger.debug("Previous Run File Modification Date: "+fileInfo.getOriginalName()+" | Date -> "+previous_lastMod);
			 logger.debug("");
                        }

		if(locationsOn && projManager.getFileLocations(fileInfo.getOriginalName()) != null && (lastMod == previous_lastMod))
			{
                           //Adding the location of replicas
			  String l = null;
			  List<String> locat = null;

                          if(timeMeasuring) iTimeStamp = System.currentTimeMillis();
                          
                           locat = projManager.getFileLocations(fileInfo.getOriginalName());
			 
                          if(timeMeasuring) timeGetFileLoc+= System.currentTimeMillis() - iTimeStamp;
                          
                          Iterator<String> i = locat.iterator();

			  //Cleaning original locations of file in order to add the location list only
			  locations.clear();

              	          while (i.hasNext()) {
			    l = i.next();

			    try{
                               URI locationURI = new URI(l);
				locations.add(new Location(locationURI.getHost(),"/"+locationURI.getPath()));

			       if(debug)
                               logger.debug("Adding IN Location -> Host: "+locationURI.getHost()+" Path: "+"/"+locationURI.getPath());
				
			       //Adding to FIP each IN file location
			       fileInfo.addLocationForVersion(fileInfo.getLastVersionId(),locationURI.getHost(),"/"+locationURI.getPath());
                            }
                            catch (URISyntaxException e) { 
                                 logger.error("Syntax error creating Target URI in file checker ", e);
                            }
                          }
                        }
                        else{
                        //If some IN files are different from the last run, clean execution historical time
                          if(!firstChangedFileDetected){
                               schedFIUpdate.resetExecTimeHistorical();
                               firstChangedFileDetected = true;
                          }
			}
             	
			//Adding to FIP the size of file version
			fileInfo.addSizeForVersion(fileInfo.getLastVersionId(),size);
		
                        //Adding to FIP the last modification date of actual run file version
			fileInfo.addLastModForVersion(fileInfo.getLastVersionId(),lastMod);

			//Adding to fileLastModDate store the last modification date of file
			if(locationsOn && (mode == AccessMode.R)) {
			   fileLastModDate.put(fileInfo.getOriginalName(),lastMod);
                        }

			// Inform the File Transfer Manager about the new file
			int firstVersionId = fileInfo.getLastVersionId();
			if (mode != AccessMode.W){
				newVersion.newFileVersion(new FileInstanceId(fileId, firstVersionId),
                                                                                fileName,
                                                                                locations);				
			}

		       //Adding FileInfo to TS file information
		       schedFIUpdate.setFileInformation(fileInfo);
		}

		// The file has already been accessed
		else {
			if (debug)
				logger.debug("Another access to " + host + ":" + path + fileName);
			
			fileInfo = idToFile.get(fileId);
		}

		// Version management
		int RVersionId, WVersionId;
		FileAccessId faId = null;
		switch (mode) {
			case R:
				RVersionId = fileInfo.getLastVersionId();
				faId = new RAccessId(fileId, RVersionId);
				if (debug) {
					logger.debug("Access:");
					logger.debug("  * Type: R");
					logger.debug("  * Read File: f" + faId.getFileId() + "v" + ((RAccessId)faId).getRVersionId());
				}
				break;
				
			case W:
				fileInfo.addVersion();
				WVersionId = fileInfo.getLastVersionId();
				faId = new WAccessId(fileId, WVersionId);
				//newVersion.newFileVersion(((WAccessId)faId).getWrittenFileInstance());
				if (debug) {
					logger.debug("Access:");
					logger.debug("  * Type: W");
					logger.debug("  * Write File: f" + faId.getFileId() + "v" + ((WAccessId)faId).getWVersionId());
				}
				break;
				
			case RW:
				RVersionId = fileInfo.getLastVersionId();
				fileInfo.addVersion();			
				WVersionId = fileInfo.getLastVersionId();
				faId = new RWAccessId(fileId, RVersionId, WVersionId);
				//newVersion.newFileVersion(((RWAccessId)faId).getWrittenFileInstance());
				if (debug) {
					logger.debug("Access:");
					logger.debug("  * Type: RW");
					logger.debug("  * Read File: f" + faId.getFileId() + "v" + ((RWAccessId)faId).getRVersionId());
					logger.debug("  * Write File: f" + faId.getFileId() + "v" + ((RWAccessId)faId).getWVersionId());
				}
				break;
		}

		return faId;
	}
	
	
	public List<FileAccessId> registerFileAccesses(List<AccessParams> accesses) {
		List<FileAccessId> faIds = new ArrayList<FileAccessId>(accesses.size());
		for (AccessParams access : accesses) {
			faIds.add(registerFileAccess(access.getName(),
		 	 	   	 	 				 access.getPath(),
		 	 	   	 	 				 access.getHost(),
		 	 	   	 	 				 access.getMode()));
		}
		return faIds;
	}
	
	
	public boolean alreadyAccessed(String fileName,
			   					   String path,
			   					   String host) {

		String locationKey = fileName + ":" + host + ":" + path;
		Integer fileId = nameToId.get(locationKey);
		
		return fileId != null;
	}

	
	
	// FileInformation interface
	
	public String getName(FileInstanceId fId) {
		return fId.getRenaming();
	}
	
	
	public List<Location> getLocations(FileInstanceId fId) {
		FileInfo info = idToFile.get(fId.getFileId());
		return info.getLocationsForVersion(fId.getVersionId());
	}

	
	public Set<ResultFile> getResultFiles(List<Integer> fileIds) {
		Set<ResultFile> resultFiles = new TreeSet<ResultFile>();
		
		for (Integer fileId : fileIds) {
			FileInfo info = idToFile.get(fileId);
			FileInstanceId fId = new FileInstanceId(info.getFileId(), info.getLastVersionId());
			String originalName = info.getOriginalName();
			Location originalLocation = info.getOriginalLocation();
				
			ResultFile rf = new ResultFile(fId,					
										   originalName,
										   originalLocation
										   );
			resultFiles.add(rf);
		}
		
		return resultFiles;
	}

	public void addSize(FileInstanceId fId,List<Long> sizeModDate){

	        FileInfo info = idToFile.get(fId.getFileId());
                long size = sizeModDate.get(0);
                long lastMod = sizeModDate.get(1);
                info.addSizeForVersion(fId.getVersionId(),size);
                info.addLastModForVersion(fId.getVersionId(),lastMod);

                /*if(debug){
                   logger.debug("Adding the size of file: "+info.getOriginalName()+" Size -> "+size+" bytes");
                   logger.debug("Adding the last mod of file: "+info.getOriginalName()+" LastMod -> "+lastMod);    
                 }*/ 
	}


	public long getSize(FileInstanceId fId){

                FileInfo info = idToFile.get(fId.getFileId());
                long size =info.getSizeForVersion(fId.getVersionId());
		return size;
        }

	public void addLocation(FileInstanceId fId,
							String newHost,
							String newPath) {
		
		FileInfo info = idToFile.get(fId.getFileId());
		info.addLocationForVersion(fId.getVersionId(), newHost, newPath);
	}

	
	public void removeLocation(FileInstanceId fId,
							   String oldHost,
							   String oldPath) {

		FileInfo info = idToFile.get(fId.getFileId());
		info.removeLocationForVersion(fId.getVersionId(), oldHost, oldPath);
	}

	public String getOriginalName(FileInstanceId fId){
		FileInfo info = idToFile.get(fId.getFileId());
		return info.getOriginalName();
	}


	public List<String> getFileLocations(String fileName){

	  List<String> locations = new ArrayList<String>();

          if(originalNameToId.get(fileName) == null){
             //Return empty location list
	     return locations;
          }

          Integer origNameId = originalNameToId.get(fileName);
	  FileInfo info = idToFile.get(origNameId);
	  List<Location> locList = info.getLocationsForVersion(info.getLastVersionId());

	 for(int i=0; i < locList.size();i++){
            Location l = locList.get(i);
	    String s ="file://"+l.getHost()+l.getPath();
	    locations.add(s);
          }
	   return locations;
	}


	public List<Long> getFileSizeAndLastMod(String fileName,String host,String path){
	   
           String target = null;
           String targetUser = null;
	   URI sourceURI = null;	
	   File f = null;
	   long size = 0;
	   long lastmod = 0;
	   List<Long> l = new ArrayList<Long>();
           double iTimeStamp = 0;

           if(timeMeasuring) iTimeStamp = System.currentTimeMillis();
           
           //Asking for source file size
           //It will check if the source of file is a worker or a datanode
           if(projManager.getProperty(host, ITConstants.USER) != null){
              targetUser = projManager.getProperty(host, ITConstants.USER);
           }
           else{
              targetUser = projManager.getDataNodeProperty(host, ITConstants.USER);
           }

           if (userNeeded && targetUser != null)
               targetUser += "@";
           else
               targetUser = "";

           try{
                sourceURI = new URI("any://"
                                    + targetUser
                                    + host + "/"
                                    + path
                                    + fileName);
              }
              catch (URISyntaxException e) {
                logger.error("Syntax error creating Source URI in size file checker ", e);
              }

              try{
                   f = GAT.createFile(context,sourceURI);
                 }
                 catch (Exception e) {
                   logger.error("Error checking file existance ", e);
                 }

                 size = f.length();
		 lastmod = f.lastModified();
		 l.add(size);
                 l.add(lastmod);

	   if(timeMeasuring) timeInSizeCalc+= System.currentTimeMillis() - iTimeStamp;
                 
          return l;
	}


	public void storeLocations(){
	 double iTimeStamp = 0;

         if(timeMeasuring) iTimeStamp = System.currentTimeMillis();

         if(originalNameToId.size() != 0){
           logger.info("Storing new file locations on project file");
           Map<String,List<String>> store =  new TreeMap<String,List<String>>();

           for (Map.Entry<String,Integer> e : originalNameToId.entrySet()) {	    
                List<String> s = getFileLocations(e.getKey());
		store.put(e.getKey(),s);
           }
           //Storing new file locations on project file
	   projManager.setFileLocations(store, fileLastModDate);
	  }
          if(timeMeasuring) timeStoreLoc = System.currentTimeMillis() - iTimeStamp;
         }

	// Controller interfaces implementation
	
	// Binding controller interface
	
	public String[] listFc() {
	    return new String[] { "NewVersion" , "ErrorReport" , "SchedFIUpdate" };
	}
	
	public Object lookupFc(final String cItf) {
		if (cItf.equals("NewVersion")) {
			return newVersion;
	    }
		else if (cItf.equals("ErrorReport")) {
			return errorReport;
	    }
		else if (cItf.equals("SchedFIUpdate")) {
			return schedFIUpdate;
	    }
	    return null;
	}
	
	public void bindFc(final String cItf, final Object sItf) {
		if (cItf.equals("NewVersion")) {
			newVersion = (FileInfoUpdate)sItf;
	    }
		else if (cItf.equals("ErrorReport")) {
			errorReport = (ITError)sItf;
	    }
		else if(cItf.equals("SchedFIUpdate")) {
			schedFIUpdate = (SchedulerUpdate)sItf;
	    }
	}
	
	public void unbindFc(final String cItf) {
	    if (cItf.equals("NewVersion")) {
	    	newVersion = null;
	    }
	    else if (cItf.equals("ErrorReport")) {
	    	errorReport = null;
	    }
            else if (cItf.equals("SchedFIUpdate")) {
                schedFIUpdate = null;
            }
	}
	
}
