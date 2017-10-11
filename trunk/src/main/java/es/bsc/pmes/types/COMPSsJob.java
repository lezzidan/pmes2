package es.bsc.pmes.types;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.bsc.compss.types.project.ProjectFile;
import es.bsc.compss.types.project.exceptions.InvalidElementException;
import es.bsc.compss.types.project.jaxb.CloudPropertiesType;
import es.bsc.compss.types.project.jaxb.ImageType;
import es.bsc.compss.types.project.jaxb.InstanceTypeType;
import es.bsc.compss.types.resources.ResourcesFile;
import es.bsc.pmes.managers.ConfigurationManager;
import es.bsc.pmes.managers.InfrastructureManager;

/**
 * Created by scorella on 9/19/16.
 */
public class COMPSsJob extends Job {

	private static final Logger logger = LogManager.getLogger(COMPSsJob.class.getName());

	public COMPSsJob() {
		super();
	}

	public static void main(String args[]) throws Exception {

	}

	public void generateConfigFiles(Path destination) {
		this.generateProject(destination);
		this.generateResources(destination);
	}

	private void generateProject(Path destination) {
		logger.debug("Generating project.xml file...");

		JobDefinition jobDef = this.getJobDef();
		ConfigurationManager cm = ConfigurationManager.getConfigurationManager();

		String imgName = jobDef.getImg().getImageName();
		String imgType = jobDef.getImg().getImageType();
		int cores = jobDef.getCores();
		String compssHome = cm.getCompssHome();
		String compssWorkingDir = cm.getCompssWorkingDir();
		String providerName = cm.getProviderName();

		// Minus 1 because the master is used as a worker (and is already one VM)
		int maxVMs = jobDef.getMaximumVMs() - 1;
		int minVMs = jobDef.getMinimumVMs() - 1;
		int initialVMs = jobDef.getInitialVMs() - 1;

		ImageType image = ProjectFile.createImage(imgName, compssHome, compssWorkingDir, cores);
		InstanceTypeType instance = ProjectFile.createInstance(imgType);

		Map<String, String> properties = InfrastructureManager.getInfrastructureManager()
				.configureResource(this.getJobDef());

		// FIXME hardcoded
		properties.put("context", "user_data=\"file://" + compssWorkingDir + "/context.login\"");

		CloudPropertiesType cloudProperties = ProjectFile.createCloudProperties(properties);

		List<ImageType> images = new ArrayList<ImageType>();
		images.add(image);

		List<InstanceTypeType> instances = new ArrayList<InstanceTypeType>();
		instances.add(instance);

		try {
			// Add the master as a compute node
			ProjectFile project = new ProjectFile(logger);
			project.addComputeNode(this.getResource(0).getIp(), compssHome, compssWorkingDir, "pmes", cores);

			// Setup the cloud provider for elasticity
			project.addCloudProvider(providerName, images, instances, maxVMs, cloudProperties);
			project.setCloudProperties(initialVMs, minVMs, maxVMs);

			project.toFile(destination.resolve("project.xml").toFile());
		} catch (InvalidElementException | JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	public void generateResources(Path destination) {
		logger.debug("Generating resources.xml file...");

		JobDefinition jobDef = this.getJobDef();
		ConfigurationManager cm = ConfigurationManager.getConfigurationManager();

		int cores = jobDef.getCores();
		float memory = jobDef.getMemory();
		float disk = jobDef.getDisk();
		String imgName = jobDef.getImg().getImageName();
		String imgType = jobDef.getImg().getImageType();
		String providerName = cm.getProviderName();
		String endpoint = cm.getEndpoint();
		String connectorJar = cm.getCompssConnectorJar();
		String connectorClass = cm.getCompssConnectorClass();

		try {
			ResourcesFile resources = new ResourcesFile(logger);
			// TODO: hardcoded
			resources.addComputeNode(this.getResource(0).getIp(), "proc", cores, "es.bsc.compss.nio.master.NIOAdaptor",
					43110, 43100);
			es.bsc.compss.types.resources.jaxb.ImageType image = ResourcesFile.createImage(imgName,
					"es.bsc.compss.nio.master.NIOAdaptor", 43110, 43100);
			// Creation time
			image.getAdaptorsOrOperatingSystemOrSoftware().add(60);
			es.bsc.compss.types.resources.jaxb.InstanceTypeType instance = ResourcesFile.createInstance(imgType, "proc",
					cores, memory, disk);

			List<es.bsc.compss.types.resources.jaxb.ImageType> images = new ArrayList<es.bsc.compss.types.resources.jaxb.ImageType>();
			images.add(image);

			List<es.bsc.compss.types.resources.jaxb.InstanceTypeType> instances = new ArrayList<es.bsc.compss.types.resources.jaxb.InstanceTypeType>();
			instances.add(instance);

			resources.addCloudProvider(providerName, endpoint, connectorJar, connectorClass, images, instances);
			resources.toFile(destination.resolve("resources.xml").toFile());

		} catch (JAXBException | es.bsc.compss.types.resources.exceptions.InvalidElementException e) {
			throw new RuntimeException(e);
		}
	}
}
