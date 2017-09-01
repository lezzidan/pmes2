package es.bsc.pmes.managers;

import es.bsc.conn.exceptions.ConnException;
import es.bsc.conn.Connector;

import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.conn.types.VirtualResource;

import es.bsc.pmes.types.Host;
import es.bsc.pmes.types.JobDefinition;
import es.bsc.pmes.types.Resource;
import es.bsc.pmes.types.SystemStatus;

import es.bsc.pmes.managers.infractructureHelpers.InfrastructureHelper;
import es.bsc.pmes.managers.infractructureHelpers.rOCCIHelper;
import es.bsc.pmes.managers.infractructureHelpers.MESOSHelper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * INFRASTRUCTURE MANAGER Class.
 *
 * This class is aimed at managing the underlying infrastructure. To this end,
 * uses the connectors available at the conn package.
 *
 * - Singleton class. - Uses generic Connector class. 
 *                    - Currently works with the rOCCI connector.
 *                    - Enabled to add Mesos connector.
 *
 * - Configuration: - Xml file with the general configuration 
 *                  - Cfg file (referenced by the xml file) with infrastructure specific parameters.
 *
 * @author scorella on 8/5/16.
 */
public class InfrastructureManager {

    /* Static infrastructure manager */
    private static InfrastructureManager infrastructureManager = new InfrastructureManager();
    private InfrastructureHelper infrastructureHelper;

    /* Main config xml file */
    // This configuration file includes only the general parameters
    // (the ones that are needed and useful for all connectors)
    private final String config_xml = "/home/pmes/pmes/config/config.xml";  // TODO: set this path as configuration (flag?)
    private String workspace;

    /* Information contained within the pmes configuration xml */
    private String conn;                 // Connector to use as string
    private Connector conn_client;       // The actual connector to use
    private ArrayList<String> commands;  // Commands to include within the contextualization
    private ArrayList<String> auth_keys; // Ssh authorized keys (for the contextualization)
    private String logLevel;             // Log level (e.g. DEBUG, INFO)
    private String logPath;              // Path where to put the logs

    /* Project related information */
    // This configuration file includes the connector configuration parameters.
    // It may also include infrastructure paramenters.
    private String config_file;
    /* Information contained within the config_file (K, V) (E.g.- key=value) */
    private HashMap<String, String> configuration;

    /* Active resources */
    private HashMap<String, Resource> activeResources;
    /* SystemStatus object */
    private SystemStatus systemStatus;

    /* Main logger */
    private static final Logger logger = LogManager.getLogger(InfrastructureManager.class);

    /**
     * CONSTRUCTOR. Default constructor.
     */
    private InfrastructureManager() {
        this.workspace = "/tmp";
        this.conn = "undefined";
        this.config_file = "/tmp/undefined.txt";
        this.activeResources = new HashMap<>();
        this.systemStatus = new SystemStatus();
        this.auth_keys = new ArrayList<>();
        this.commands = new ArrayList<>();
        this.logLevel = "DEBUG";
        this.logPath = "/tmp";
        this.configuration = new HashMap<String, String>();
        try {
            configureResources();
        } catch (ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * CONFIGURE RESOURCES METHOD.
     *
     * Retrieves the information from the config xml and sets the Infrastructure
     * Manager values.
     *
     * @throws ParserConfigurationException
     * @throws java.io.FileNotFoundException
     */
    public void configureResources() throws ParserConfigurationException, IOException {
        // Retrieve the information from config xml
        File fXML = new File(this.config_xml);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        Document doc = null;
        try {
            doc = dBuilder.parse(fXML);
        } catch (SAXException e) {
            e.printStackTrace();
        }
        doc.getDocumentElement().normalize();

        this.workspace = doc.getDocumentElement().getElementsByTagName("workspace").item(0).getTextContent();
        this.conn = doc.getDocumentElement().getElementsByTagName("connector").item(0).getTextContent();
        this.config_file = doc.getDocumentElement().getElementsByTagName("configFile").item(0).getTextContent();
        this.logLevel = doc.getDocumentElement().getElementsByTagName("logLevel").item(0).getTextContent();
        this.logPath = doc.getDocumentElement().getElementsByTagName("logPath").item(0).getTextContent();

        NodeList nlist = doc.getElementsByTagName("host");
        for (int i = 0; i < nlist.getLength(); i++) {
            Node node = nlist.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String name = element.getElementsByTagName("name").item(0).getTextContent();
                Integer cpu = Integer.valueOf(element.getElementsByTagName("MAX_CPU").item(0).getTextContent());
                Float mem = Float.valueOf(element.getElementsByTagName("MAX_MEM").item(0).getTextContent());
                Host host = new Host(name, cpu, mem);
                systemStatus.addHost(host);
            }
        }

        NodeList keys = doc.getElementsByTagName("key");
        for (int i = 0; i < keys.getLength(); i++) {
            Node node = keys.item(i);
            this.auth_keys.add(node.getTextContent());
        }

        NodeList cmds = doc.getElementsByTagName("cmd");
        for (int i = 0; i < cmds.getLength(); i++) {
            Node node = cmds.item(i);
            this.commands.add(node.getTextContent());
        }

        // Retrieve the configuration from the config file
        BufferedReader bfr = new BufferedReader(new FileReader(new File(config_file)));
        String line;
        while ((line = bfr.readLine()) != null) {
            if (!line.startsWith("#") && !line.isEmpty()) {
                String[] pair = line.trim().split("=");
                this.configuration.put(pair[0].trim(), pair[1].trim());
            }
        }
        bfr.close();

        // Instantiate the appropiate infrastructure helper
        switch (this.conn) {
            case "ROCCI":
                this.infrastructureHelper = new rOCCIHelper(this.workspace, this.commands, this.auth_keys);
                break;
            case "MESOS":
                this.infrastructureHelper = new MESOSHelper(this.workspace, this.commands, this.auth_keys);
                break;
            default:
                throw new UnsupportedOperationException("Undefined connector or helper not implemented.");
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
    public HashMap<String, Resource> getActiveResources() {
        return activeResources;
    }

    /**
     * Active resources setter
     *
     * @param activeResources The new active resources
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
     * @param hd The resource hardware description
     * @param sd The resource software descripption
     * @param prop Properties hashmap
     * @return String with the creation id ("-1" if error, null if unsupported
     * provider)
     */
    public String createResource(HardwareDescription hd, SoftwareDescription sd, HashMap<String, String> prop) {
        logger.trace("Creating Resource");
        try {
            VirtualResource vr = infrastructureHelper.createResource(hd, sd, prop, this.configuration);
            // Update System Status
            // OCCI doesn't give information about what host will be hosting the VM
            Host h = systemStatus.getCluster().get(0); //test purposes: always get first Host
            systemStatus.update(h, hd.getTotalComputingUnits(), hd.getMemorySize());
            logger.trace("IM update: " + hd.getTotalComputingUnits() + " " + hd.getMemorySize());
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
     * This method creates the resource properties hashmap from the job
     * definition.
     *
     * @param jobDef Job definition
     * @return Properties hashmap
     */
    public HashMap<String, String> configureResource(JobDefinition jobDef) {
        HashMap<String, String> properties = new HashMap<>();
        properties = this.infrastructureHelper.configureResource(jobDef, this.configuration);
        return properties;
    }

    /**
     * DESTROY A RESOURCE.
     *
     * Deletes a virtual resource with the given Id
     *
     * @param Id Id of the resource to be destroyed
     */
    public void destroyResource(String Id) {
        VirtualResource vr = activeResources.get(Id).getVr();
        logger.trace("Destroying VM " + Id);
        this.infrastructureHelper.destroyResource((String) vr.getId());
    }

}
