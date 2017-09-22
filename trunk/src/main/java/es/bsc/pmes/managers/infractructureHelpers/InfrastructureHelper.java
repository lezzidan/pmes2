package es.bsc.pmes.managers.infractructureHelpers;

import java.util.List;
import java.util.Map;

import es.bsc.conn.exceptions.ConnException;
import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.conn.types.VirtualResource;
import es.bsc.pmes.types.JobDefinition;

/**
 *
 *
 */
public abstract class InfrastructureHelper {

	// Common infrastructure helper parameters
	private String workspace; // workspace path indicated in config xml
	private List<String> commands; // Commands to include within the contextualization
	private List<String> authKeys; // Ssh authorized keys (for the contextualization)

	/* Constructor */
	public InfrastructureHelper(String workspace, List<String> commands, List<String> auth_keys) {
		this.workspace = workspace;
		this.commands = commands;
		this.authKeys = auth_keys;
	}

	/**
	 * ************************************************************************
	 * GETTERS
	 * ************************************************************************
	 */

	public String getWorkspace() {
		return workspace;
	}

	public List<String> getCommands() {
		return commands;
	}

	public List<String> getAuth_keys() {
		return authKeys;
	}

	/**
	 * ************************************************************************
	 * ABSTRACT METHODS
	 * ************************************************************************
	 */

	/**
	 * Create resource configuration.
	 * 
	 * This method is used to process the configuration and create a
	 * contextualization hashmap.
	 * 
	 * @param jobDef
	 *            Job definition
	 * @param configuration
	 *            Specific configuration (cfg file)
	 * @return Contextualization K,V hashmap
	 */
	public abstract Map<String, String> configureResource(JobDefinition jobDef, Map<String, String> configuration);

	/**
	 * Create resource method.
	 * 
	 * This method must be used to implement how to create a new resource within
	 * each infrastructure helper.
	 * 
	 * @param hd
	 *            Hardware description
	 * @param sd
	 *            Software description
	 * @param prop
	 *            Properties
	 * @param configuration
	 *            Specific configuration (cfg file)
	 * @return A new VirtualResource instance
	 * @throws ConnException
	 *             when fails to create the new resource.
	 */
	public abstract VirtualResource createResource(HardwareDescription hd, SoftwareDescription sd,
			Map<String, String> prop, Map<String, String> configuration) throws ConnException;

	/**
	 * Destroy a resource.
	 * 
	 * Remove a resource with the given id.
	 * 
	 * @param Id
	 *            Resource id to remove
	 */
	public abstract void destroyResource(String Id);

}
