package es.bsc.pmes.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import es.bsc.pmes.types.Host;

/**
 * 
 * Reads XML config file and provides methods to access the different
 * configuration parameters.
 * 
 * @author jalvarez
 *
 */
public class ConfigurationManager {

	// Global properties
	public static final String JOB_DIR = "/jobs";
	public static final String PROJECT_DIR = "/Runtime/configuration/xml/projects";
	public static final String RESOURCES_DIR = "/Runtime/configuration/xml/resources";

	private static final Logger logger = LogManager.getLogger(ConfigurationManager.class);

	// TODO: these paths could be relative to the installation path (maybe in
	// tomcat?)
	private static final String CONFIG_FILE = "/home/pmes/pmes/config/config.xml";
	private static final String SCHEMA_FILE = "/home/pmes/pmes/config/config.xsd";

	private static ConfigurationManager cm = new ConfigurationManager();

	/* Information contained within the PMES configuration XML */
	private String workspace;
	private String connector; // Connector to use as string
	private List<String> commands; // Commands to include within the contextualization
	private List<String> authKeys; // Ssh authorized keys (for the contextualization)
	private int timeout; // Timeout for SSH connections
	private int pollingInterval; // Polling interval for SSH connectivity
	private List<Host> hosts;
	private Map<String, String> properties;
	private String compssHome;
	private String compssWorkingDir;
	private String providerName;
	private String endpoint;
	private String compssConnectorJar;
	private String compssConnectorClass;

	private ConfigurationManager() {
		this.commands = new ArrayList<String>();
		this.authKeys = new ArrayList<String>();
		this.hosts = new ArrayList<Host>();
		this.properties = new HashMap<String, String>();

		this.validateConfigFile();
		this.parseConfigFile();
	}

	public static ConfigurationManager getConfigurationManager() {
		return cm;
	}

	public String getProviderName() {
		return providerName;
	}

	public String getCompssConnectorJar() {
		return compssConnectorJar;
	}

	public String getCompssConnectorClass() {
		return compssConnectorClass;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public String getWorkspace() {
		return workspace;
	}

	public String getCompssHome() {
		return compssHome;
	}

	public String getCompssWorkingDir() {
		return compssWorkingDir;
	}

	public String getConnectorClass() {
		return connector;
	}

	public List<String> getCommands() {
		return new ArrayList<String>(this.commands);
	}

	public List<String> getAuthKeys() {
		return authKeys;
	}

	public int getTimeout() {
		return timeout;
	}

	public int getPollingInterval() {
		return pollingInterval;
	}

	public List<Host> getHosts() {
		return new ArrayList<Host>(this.hosts);
	}

	public Map<String, String> getConnectorProperties() {
		return new HashMap<String, String>(this.properties);
	}

	private void validateConfigFile() {
		// Validate config XML
		File configFile = new File(CONFIG_FILE);
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Source xmlFile = new StreamSource(configFile);

		try {
			Schema schema = schemaFactory.newSchema(new StreamSource(new File(SCHEMA_FILE)));
			Validator validator = schema.newValidator();
			validator.validate(xmlFile);
		} catch (SAXException e) {
			logger.error(xmlFile + " is not a valid config file.");
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void parseConfigFile() {
		// Retrieve the information from config xml
		File configFile = new File(CONFIG_FILE);
		Document doc = null;

		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(configFile);
			doc.getDocumentElement().normalize();
		} catch (SAXException | IOException | ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
		String logLevel = doc.getDocumentElement().getElementsByTagName("logLevel").item(0).getTextContent();
		this.setLogLevel(logLevel);

		this.connector = doc.getDocumentElement().getElementsByTagName("connector").item(0).getAttributes()
				.getNamedItem("className").getTextContent();
		this.workspace = doc.getDocumentElement().getElementsByTagName("workspace").item(0).getTextContent();
		this.compssHome = doc.getDocumentElement().getElementsByTagName("installDir").item(0).getTextContent();
		this.compssWorkingDir = doc.getDocumentElement().getElementsByTagName("workingDir").item(0).getTextContent();
		this.compssConnectorJar = doc.getDocumentElement().getElementsByTagName("connectorJar").item(0)
				.getTextContent();
		this.compssConnectorClass = doc.getDocumentElement().getElementsByTagName("connectorClass").item(0)
				.getTextContent();
		this.providerName = doc.getDocumentElement().getElementsByTagName("providerName").item(0).getTextContent();
		this.endpoint = doc.getDocumentElement().getElementsByTagName("endpoint").item(0).getTextContent();
		this.timeout = Integer
				.parseInt(doc.getDocumentElement().getElementsByTagName("timeout").item(0).getTextContent());
		this.pollingInterval = Integer
				.parseInt(doc.getDocumentElement().getElementsByTagName("pollingInterval").item(0).getTextContent());

		// Get connector properties
		NodeList propList = doc.getElementsByTagName("property");

		for (int i = 0; i < propList.getLength(); i++) {
			Node node = propList.item(i);

			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String key = element.getElementsByTagName("key").item(0).getTextContent();
				String value = element.getElementsByTagName("value").item(0).getTextContent();
				properties.put(key, value);
			}
		}

		// Get hosts
		NodeList hostList = doc.getElementsByTagName("host");

		for (int i = 0; i < hostList.getLength(); i++) {
			Node node = hostList.item(i);

			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String name = element.getElementsByTagName("name").item(0).getTextContent();
				Integer cpu = Integer.valueOf(element.getElementsByTagName("MAX_CPU").item(0).getTextContent());
				Float mem = Float.valueOf(element.getElementsByTagName("MAX_MEM").item(0).getTextContent());
				Host host = new Host(name, cpu, mem);
				this.hosts.add(host);
			}
		}

		NodeList keys = doc.getElementsByTagName("key");
		for (int i = 0; i < keys.getLength(); i++) {
			Node node = keys.item(i);
			this.authKeys.add(node.getTextContent());
		}

		NodeList cmds = doc.getElementsByTagName("cmd");
		for (int i = 0; i < cmds.getLength(); i++) {
			Node node = cmds.item(i);
			this.commands.add(node.getTextContent());
		}
	}

	private void setLogLevel(String level) {
		switch (level) {
		case "DEBUG":
			Configurator.setRootLevel(Level.DEBUG);
			break;

		case "ERROR":
			Configurator.setRootLevel(Level.ERROR);
			break;

		case "WARN":
			Configurator.setRootLevel(Level.WARN);
			break;

		case "TRACE":
			Configurator.setRootLevel(Level.TRACE);
			break;

		case "FATAL":
			Configurator.setRootLevel(Level.FATAL);
			break;

		case "OFF":
			Configurator.setRootLevel(Level.OFF);
			break;

		case "ALL":
			Configurator.setRootLevel(Level.ALL);
			break;

		default:
			Configurator.setRootLevel(Level.INFO);
			break;
		}
	}

	public static void main(String args[]) throws Exception {
		ConfigurationManager.getConfigurationManager();
	}
}
