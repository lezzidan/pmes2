
package integratedtoolkit.util;

import java.util.Map;
import java.util.TreeMap;


/* Class that allows creation and management of groups
 * Modifications (addition and deletion of groups) must be synchronized
 */ 
public class GroupManager {

	private static final int FIRST_GROUP_ID = 1;
	
	private Map<Integer,Integer> groups;
	private int nextGroupId;
	
	
	public GroupManager() {
		this.groups = new TreeMap<Integer,Integer>();
		this.nextGroupId = FIRST_GROUP_ID;
	}
	
	public int addGroup() {
		return addGroup(0);
	}
	
	public synchronized int addGroup(int numberOfMembers) {
		groups.put(nextGroupId, numberOfMembers);
		return nextGroupId++;
	}
	
	public synchronized void removeGroup(int groupId) {
		groups.remove(groupId);
	}
	
	public void addMember(int groupId) throws ElementNotFoundException {
		addMembers(groupId, 1);
	}
	
	public synchronized void addMembers(int groupId, int numberOfMembers)
	 						 throws ElementNotFoundException {
		Integer n;
		if ((n = groups.get(groupId)) == null)
			throw new ElementNotFoundException();
		
		groups.put(groupId, n + numberOfMembers);
	}
	
	public int removeMember(int groupId) throws ElementNotFoundException {
		return removeMembers(groupId, 1);
	}

	public synchronized int removeMembers(int groupId, int numberOfMembers)
							throws ElementNotFoundException {
		Integer n;
		if ((n = groups.get(groupId)) == null)
			throw new ElementNotFoundException();
		
		if (n > numberOfMembers) n -= numberOfMembers;
		else					 n = 0;
		
		groups.put(groupId, n);
		return n;
	}
	
	public synchronized boolean exists(int groupId) {
		return groups.containsKey(groupId);
	}
	
	public synchronized boolean hasMembers(int groupId)
								throws ElementNotFoundException {
		Integer n;
		if ((n = groups.get(groupId)) == null)
			throw new ElementNotFoundException();
		
		return n > 0;
	}
	
	public synchronized void clear() {
		groups.clear();
		nextGroupId = FIRST_GROUP_ID;
	}
	
}
