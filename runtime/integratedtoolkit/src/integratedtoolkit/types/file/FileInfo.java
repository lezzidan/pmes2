
package integratedtoolkit.types.file;

import java.io.Serializable;

import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Set;
import java.util.TreeSet;

// Information about a file and its versions
public class FileInfo implements Serializable {
	
	private static final int FIRST_FILE_ID 		= 1;
	private static final int FIRST_VERSION_ID 	= 1;
	
	// File identifier
	int fileId;
	
	// Original name and location of the file
	private String origName;
	private Location origLocation;

	// Versions of the file
	// Map: version identifier -> version
	private Map<Integer,Version> versions;
	
	// File and version identifier management
	private static int nextFileId;
	private int currentVersionId;

	
	public static void init() {
		nextFileId = FIRST_FILE_ID;
	}
	
	public FileInfo(String origName, String origHost, String origPath) {
		this.fileId = nextFileId++;
		this.origName = origName;
		this.origLocation = new Location(origHost, origPath);
		
		this.versions = new TreeMap<Integer,Version>();
		this.currentVersionId = FIRST_VERSION_ID;
		Version firstVersion = new Version();
		firstVersion.addLocation(origHost, origPath);
		this.versions.put(currentVersionId, firstVersion);
	}
	
	public int getFileId() {
		return fileId;
	}
	
	public int getLastVersionId() {
		return currentVersionId;
	}
	
	public FileInstanceId getLastFileInstanceId() {
		return new FileInstanceId(fileId, currentVersionId);
	}
	
	public String getOriginalName() {
		return origName;
	}
	
	public Location getOriginalLocation() {
		return origLocation;
	}
	
	public int getNumberOfVersions() {
		return versions.size();
	}
	
	public List<Location> getLocationsForVersion(int versionId) {
		return versions.get(versionId).getLocations();
	}

	public long getSizeForVersion(int versionId){
		return versions.get(versionId).getSize();
	}

	public long getLastModForVersion(int versionId){
                return versions.get(versionId).getLastMod();
        }
		
	public void addVersion() {
		versions.put(++currentVersionId, new Version());
	}
	
	public void addLocationForVersion(int versionId, String host, String path) {
		Version v = versions.get(versionId);
		v.addLocation(host, path);
	}
		
	public void removeVersion(int versionId) {
		versions.remove(versionId);
	}
		
	public void removeLocationForVersion(int versionId, String host, String path) {
		Version v = versions.get(versionId);
		v.removeLocation(host, path);
	}

	public void addSizeForVersion(int versionId, long size) {
                Version v = versions.get(versionId);
                v.addSize(size);
        }

	public void addLastModForVersion(int versionId, long lastmod) {
                Version v = versions.get(versionId);
                v.addLastMod(lastmod);
        }

		
	private class Version implements Serializable {
		// Locations with a copy of this version
		private List<Location> locations;
		private Set<Location> uniqueLocations;
		//Size of file version
		private long size=0;
		
		//Last modification Date of file version
		private long lastMod=0;		

		public Version() {
			this.locations = new LinkedList<Location>();
			this.uniqueLocations = new TreeSet<Location>();
		}
		
		public List<Location> getLocations() {
			return locations;
		}
		
		public void addLocation(String host, String path) {

			int prevSize = uniqueLocations.size();
			uniqueLocations.add(new Location(host,path));
			
			//There is new Locations, avoiding to repeat locations in the list.
			if(prevSize != uniqueLocations.size()){
			  locations.add(new Location(host, path));
			}
		}
		
		public void removeLocation(String host, String path) {
			locations.remove(new Location(host, path));
		}			
		
		public void addSize(long s){
		        size = s;
		}

		public void addLastMod(long d){
                        lastMod = d;
                }

		public long getSize(){
		        return size;
		}

		public long getLastMod(){
                        return lastMod;
                }
	}
		
}
