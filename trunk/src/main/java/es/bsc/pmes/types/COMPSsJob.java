package es.bsc.pmes.types;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import es.bsc.compss.types.project.ProjectFile;
import es.bsc.compss.types.project.exceptions.InvalidElementException;
import es.bsc.compss.types.project.jaxb.CloudPropertiesType;
import es.bsc.compss.types.project.jaxb.ImageType;
import es.bsc.compss.types.project.jaxb.InstanceTypeType;
import es.bsc.compss.types.resources.ResourcesFile;
import es.bsc.pmes.managers.ConfigurationManager;

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
		CloudPropertiesType cloudProperties = ProjectFile.createCloudProperties(cm.getConnectorProperties());

		List<ImageType> images = new ArrayList<ImageType>();
		images.add(image);

		List<InstanceTypeType> instances = new ArrayList<InstanceTypeType>();
		instances.add(instance);

		try {
			// Add the master as a compute node
			ProjectFile project = new ProjectFile(logger);
			project.addComputeNode("master", compssHome, compssWorkingDir, "pmes", cores);

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
			resources.addComputeNode("master", "proc", cores, "es.bsc.compss.nio.master.NIOAdaptor", 43110, 43100);
			es.bsc.compss.types.resources.jaxb.ImageType image = ResourcesFile.createImage(imgName,
					"es.bsc.compss.nio.master.NIOAdaptor", 43110, 43100);
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

	/** Generate resources.xml */
	public void generateResources2() {
		// TODO
		try {
			// Create Document
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();
			doc.setXmlStandalone(true);
			Element rootElement = doc.createElement("ResourcesList");
			doc.appendChild(rootElement);

			Element cloudProvider = doc.createElement("CloudProvider");
			rootElement.appendChild(cloudProvider);
			cloudProvider.setAttribute("Name", "ProviderName"); // TODO parametrizar

			Element endpoint = doc.createElement("Endpoint");
			cloudProvider.appendChild(endpoint);

			Element server = doc.createElement("Server");
			server.appendChild(doc.createTextNode("https://bscgrid20.bsc.es:11443/")); // TODO parametrizar
			endpoint.appendChild(server);

			Element connector = doc.createElement("Connector");
			connector.appendChild(doc.createTextNode("integratedtoolkit.connectors.rocci.ROCCI")); // TODO parametrizar
			endpoint.appendChild(connector);

			Element images = doc.createElement("Images");
			cloudProvider.appendChild(images);

			Element image = doc.createElement("Image");
			image.setAttribute("Name", this.getJobDef().getImg().getImageName());
			images.appendChild(image);

			Element creationTime = doc.createElement("CreationTime");
			creationTime.appendChild(doc.createTextNode("60"));
			image.appendChild(creationTime);

			Element adaptors = doc.createElement("Adaptors");
			image.appendChild(adaptors);

			Element adaptor = doc.createElement("Adaptor");
			adaptor.setAttribute("Name", "integratedtoolkit.nio.master.NIOAdaptor");
			adaptors.appendChild(adaptor);

			Element submissionSystem = doc.createElement("SubmissionSystem");
			adaptor.appendChild(submissionSystem);

			Element interactive = doc.createElement("Interactive");
			submissionSystem.appendChild(interactive);

			Element ports = doc.createElement("Ports");
			adaptor.appendChild(ports);

			Element minport = doc.createElement("MinPort");
			minport.appendChild(doc.createTextNode("43100")); // TODO parametrizar
			ports.appendChild(minport);

			Element maxport = doc.createElement("MaxPort");
			maxport.appendChild(doc.createTextNode("43110")); // TODO parametrizar
			ports.appendChild(maxport);

			Element instanceTypes = doc.createElement("InstanceTypes");
			cloudProvider.appendChild(instanceTypes);

			String[] instanceTypesNames = { "small", "medium", "large", "extra_large" };
			String[] processorName = { "Processor1", "Processor1", "Processor1", "Processor1" }; // TODO parametrizar
			String[] computingUnitsValue = { "1", "4", "8", "16" }; // TODO parametrizar
			String[] priceValue = { "0.085", "0.212", "0.34", "0.68" }; // TODO parametrizar
			for (int i = 0; i < instanceTypesNames.length; i++) {
				Element instanceType = doc.createElement("InstanceType");
				instanceTypes.appendChild(instanceType);
				instanceType.setAttribute("Name", instanceTypesNames[i]);

				Element processor = doc.createElement("Processor");
				instanceType.appendChild(processor);
				processor.setAttribute("Name", processorName[i]);

				Element computingUnits = doc.createElement("ComputingUnits");
				processor.appendChild(computingUnits);
				computingUnits.appendChild(doc.createTextNode(computingUnitsValue[i]));

				Element price = doc.createElement("Price");
				instanceType.appendChild(price);

				Element timeUnit = doc.createElement("TimeUnit");
				price.appendChild(timeUnit);
				timeUnit.appendChild(doc.createTextNode("1"));

				Element pricePerUnit = doc.createElement("PricePerUnit");
				price.appendChild(pricePerUnit);
				pricePerUnit.appendChild(doc.createTextNode(priceValue[i]));
			}

			// Write to xml
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(
					new File("/home/bscuser/subversion/projects/pmes2/trunk/src/main/resources/resources.xml")); // TODO
																													// complete
																													// path

			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.transform(source, result);

			logger.debug("resources.xml generated and saved");

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

}
