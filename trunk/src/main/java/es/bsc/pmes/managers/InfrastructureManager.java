package es.bsc.pmes.managers;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.bsc.conn.exceptions.ConnException;
import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.conn.types.VirtualResource;
import es.bsc.pmes.managers.infractructureHelpers.InfrastructureHelper;
import es.bsc.pmes.types.Host;
import es.bsc.pmes.types.JobDefinition;
import es.bsc.pmes.types.Resource;
import es.bsc.pmes.types.SystemStatus;

/**
 * INFRASTRUCTURE MANAGER Class.
 *
 * This class is aimed at managing the underlying infrastructure. To this end,
 * uses the connectors available at the conn package.
 *
 * - Singleton class. - Uses generic Connector class. - Currently works with the
 * rOCCI connector. - Enabled to add Mesos connector.
 *
 * - Configuration: - Xml file with the general configuration - Cfg file
 * (referenced by the xml file) with infrastructure specific parameters.
 *
 * @author scorella on 8/5/16.
 */
public class InfrastructureManager {

	/* Main logger */
	private static final Logger logger = LogManager.getLogger(InfrastructureManager.class);

	/* Static infrastructure manager */
	private static InfrastructureManager infrastructureManager = new InfrastructureManager();
	private InfrastructureHelper infrastructureHelper;

	/* Active resources */
	private Map<String, Resource> activeResources;
	/* SystemStatus object */
	private SystemStatus systemStatus;

	/**
	 * CONSTRUCTOR. Default constructor.
	 */
	private InfrastructureManager() {
		this.systemStatus = new SystemStatus();
		this.activeResources = new HashMap<String, Resource>();
		this.configureResources();
	}

	/**
	 * CONFIGURE RESOURCES METHOD.
	 *
	 * Instantiates the infrastructure helper and adds hosts to system status.
	 * 
	 */
	public void configureResources() {
		ConfigurationManager cm = ConfigurationManager.getConfigurationManager();
		String connector = "es.bsc.pmes.managers.infractructureHelpers." + cm.getConnectorClass();
		String workspace = cm.getWorkspace();
		List<String> commands = cm.getCommands();
		List<String> authKeys = cm.getAuthKeys();

		try {
			Constructor<?> c = Class.forName(connector).getConstructor(String.class, List.class, List.class);
			this.infrastructureHelper = (InfrastructureHelper) c.newInstance(workspace, commands, authKeys);

		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Connector class " + connector + " not found (check config file).", e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Could not find the appropriate constructor for " + connector + " class.", e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		for (Host h : cm.getHosts()) {
			this.systemStatus.addHost(h);
		}
	}

	/**
	 * ************************************************************************
	 * GETTERS AND SETTERS.
	 * ************************************************************************
	 */
	/**
	 * Infrastructure manager getter
	 *
	 * @return the static infrastructureManager instance
	 */
	public static InfrastructureManager getInfrastructureManager() {
		return infrastructureManager;
	}

	/**
	 * Active resources getter
	 *
	 * @return Hashmap containing the active resources
	 */
	public Map<String, Resource> getActiveResources() {
		return activeResources;
	}

	/**
	 * Active resources setter
	 *
	 * @param activeResources
	 *            The new active resources
	 */
	public void setActiveResources(HashMap<String, Resource> activeResources) {
		this.activeResources = activeResources;
	}

	/**
	 * System status getter
	 *
	 * @return The system status
	 */
	public SystemStatus getSystemStatus() {
		return this.systemStatus;
	}

	/**
	 * ************************************************************************
	 * INFRASTRUCTURE MANAGER FUNCTIONS.
	 * ***********************************************************************
	 */
	/**
	 * CREATE RESOURCE METHOD.
	 *
	 * This method creates a resource from the information provided using the
	 * connector.
	 *
	 * @param hd
	 *            The resource hardware description
	 * @param sd
	 *            The resource software descripption
	 * @param prop
	 *            Properties hashmap
	 * @return String with the creation id ("-1" if error, null if unsupported
	 *         provider)
	 */
	public String createResource(HardwareDescription hd, SoftwareDescription sd, Map<String, String> prop) {
		logger.debug("Creating Resource");

		try {
			Map<String, String> properties = ConfigurationManager.getConfigurationManager().getConnectorProperties();

			VirtualResource vr = this.infrastructureHelper.createResource(hd, sd, prop, properties);
			// Update System Status
			// OCCI doesn't give information about what host will be hosting the VM
			Host h = systemStatus.getCluster().get(0); // test purposes: always get first Host
			systemStatus.update(h, hd.getTotalComputingUnits(), hd.getMemorySize());
			logger.debug("IM update: " + hd.getTotalComputingUnits() + " " + hd.getMemorySize());
			Resource newResource = new Resource(vr.getIp(), prop, vr);
			activeResources.put((String) vr.getId(), newResource);
			return (String) vr.getId();
		} catch (ConnException e) {
			logger.error("Error creating resource: ", e);
			return "-1";
		}
	}

	/**
	 * RESOURCE CONFIGURATION METHOD.
	 *
	 * This method creates the resource properties hashmap from the job definition.
	 *
	 * @param jobDef
	 *            Job definition
	 * @return Properties hashmap
	 */
	public Map<String, String> configureResource(JobDefinition jobDef) {
		Map<String, String> properties = ConfigurationManager.getConfigurationManager().getConnectorProperties();
		return this.infrastructureHelper.configureResource(jobDef, properties);
	}

	/**
	 * DESTROY A RESOURCE.
	 *
	 * Deletes a virtual resource with the given Id
	 *
	 * @param Id
	 *            Id of the resource to be destroyed
	 */
	public void destroyResource(String Id) {
		VirtualResource vr = activeResources.get(Id).getVr();
		logger.debug("Destroying VM " + Id);
		this.infrastructureHelper.destroyResource((String) vr.getId());
	}
}
