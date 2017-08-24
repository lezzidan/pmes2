package es.bsc.pmes.managers;

import es.bsc.conn.exceptions.ConnException;
import es.bsc.conn.Connector;
import es.bsc.conn.rocci.ROCCI;
import es.bsc.conn.mesos.Mesos;

import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.conn.types.VirtualResource;

import es.bsc.pmes.types.Host;
import es.bsc.pmes.types.JobDefinition;
import es.bsc.pmes.types.MountPoint;
import es.bsc.pmes.types.Resource;
import es.bsc.pmes.types.SystemStatus;

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
import java.util.Objects;
import java.util.TreeMap;

/**
 * INFRASTRUCTURE MANAGER Class.
 *
 * This class is aimed at managing the underlying infrastructure. To this end,
 * uses the connectors available at the conn package.
 *
 * - Singleton class. 
 *     - Uses generic Connector class.
 *     - Currently works with the rOCCI connector.
 *     - Enabled to add Mesos connector.
 * 
 * - Configuration:
 *     - Xml file with the general configuration
 *     - Cfg file (referenced by the xml file) with infrastructure specific parameters.
 *
 * @author scorella on 8/5/16.
 */
public class InfrastructureManager {

    /* Static infrastructure manager */
    private static InfrastructureManager infrastructureManager = new InfrastructureManager();

    /* Main config xml file */
    // This configuration file includes only the general parameters
    // (the ones that are needed and useful for all connectors)
    private final String config_xml = "/home/pmes/pmes/config/config.xml";  // TODO: set this path as configuration (flag?)
    private String workspace;    
    
    /* Information contained within the pmes configuration xml */
    /* Connector to use */
    private String conn;
    private Connector conn_client;
    private ArrayList<String> commands;  // Commands to include within the contextualization
    private ArrayList<String> auth_keys; // Ssh authorized keys (for the contextualization)
    private String logLevel;             // Log level (e.g. DEBUG, INFO)
    private String logPath;              // Path where to put the logs

    /* Project related information */
    // This configuration file includes the connector configuration parameters.
    // It may also include infrastructure paramenters.
    private String config_file;
    /* Information contained within the config_file (K, V) */
    // E.g.- key=value
    TreeMap<String, String> configuration;

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
        this.configuration = new TreeMap<String, String>();
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
    }

    /**
     *************************************************************************
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
            switch (this.conn) {
                case "ROCCI":
                    String provider = this.configuration.get("providerName");
                    String link2 = this.configuration.get("link2");
                    if (Objects.equals("ONE", provider) || Objects.equals("OpenStack", provider)) {
                        conn_client = new ROCCI(prop);
                        String vrID = (String) conn_client.create(hd, sd, prop);
                        logger.trace("compute id: " + vrID);

                        VirtualResource vr = conn_client.waitUntilCreation(vrID);

                        // The NFS at EBI requires a second network interface
                        // Uses the /etc/network/interfaces.d/eth1.cfg created with cloud-init
                        if (Objects.equals("OpenStack", provider) && !"".equals(link2)) {
                            // If we are using OpenStack, like at EBI, it is necessary 
                            // to attach a new network interface for mounting the nfs.
                            logger.trace("[EBI] Attaching new network interface for storage sharing purposes (NFS).");
                            ((ROCCI) conn_client).attachLink(vrID, link2);
                        }

                        logger.trace("VM id: " + vr.getId());
                        logger.trace("VM ip: " + vr.getIp());

                        // Update System Status
                        // TODO: occi doesn't give information about what host will be hosting the VM
                        Host h = systemStatus.getCluster().get(0); //test purposes: always get first Host
                        systemStatus.update(h, hd.getTotalComputingUnits(), hd.getMemorySize());
                        logger.trace("IM update: " + hd.getTotalComputingUnits() + " " + hd.getMemorySize());
                        Resource newResource = new Resource(vr.getIp(), prop, vr);
                        activeResources.put((String) vr.getId(), newResource);
                        return (String) vr.getId();

                    } else {
                        // Unsupported provider
                        logger.error("Provider " + provider + " not supported");
                        return null;
                    }
                case "MESOS":
                    // Instantiate mesos connector:
                    conn_client = new Mesos(prop);

                    // TODO: Add mesos stuff.
                    return null;
                default:
                    // Unsupported connector
                    logger.error("Unsupported connector");
                    return "-1";
            }
        } catch (ConnException e) {
            logger.error("Error creating resource");
            e.printStackTrace();
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

        switch (this.conn) {
            case "ROCCI":
                String occiEndPoint = this.configuration.get("endPointROCCI");
                String auth = this.configuration.get("auth");
                String ca_path = this.configuration.get("ca-path");
                String link = this.configuration.get("link");

                // Default rOCCI server configuration
                properties.put("Server", occiEndPoint);
                properties.put("auth", auth);
                properties.put("link", link);

                if (auth.equals("token")) {
                    logger.trace("Authentication method: token");
                    logger.trace("token: " + jobDef.getUser().getCredentials().get("token"));
                    properties.put("token", jobDef.getUser().getCredentials().get("token"));
                } else {
                    logger.trace("Authentication method: pem-key");
                    properties.put("ca-path", ca_path);
                    String keyPath = jobDef.getUser().getCredentials().get("key");
                    String pemPath = jobDef.getUser().getCredentials().get("pem");
                    properties.put("password", keyPath);
                    properties.put("user-cred", pemPath);
                }

                properties.put("owner", jobDef.getUser().getUsername());
                properties.put("jobname", jobDef.getJobName());

                String contextDataFile = createContextDataFile(jobDef);
                properties.put("context", "user_data=\"file://" + contextDataFile + "\"");
                break;
            case "MESOS":
                // TODO: Add MESOS stuff.
                break;
            default:
                // TODO: default stuff?
                // If nothing is done, properties will be empty
                break;
        }
        return properties;
    }

    /**
     * CREATE CONTEXT FILE METHOD -- Just for ROCCI.
     * 
     * Called from configureResource function.
     *
     * This method creates the context file from the job definition taking into
     * account the peculiarities of the infrastructure. Currently considers the
     * cloud-init configuration format.
     *
     * @param jobDef Job definition
     * @return Path where the context file has been stored
     */
    public String createContextDataFile(JobDefinition jobDef) {
        logger.trace("Creating context data file");
        String dir = jobDef.getJobName();
        String path = this.workspace + "/jobs/" + dir + "/context.login";
        logger.trace("Creating context data file " + path);
        try {
            //String infrastructure = jobDef.getInfrastructure();  // TODO: REMOVE INFRASTRUCTURE FROM JOB DEFINITION
            String infrastructure = this.configuration.get("infrastructure");
            String nfs_server = this.configuration.get("nfs_server");
            String vm_java_home = this.configuration.get("vm_java_home");
            String vm_compss_home = this.configuration.get("vm_compss_home");
            
            String user = jobDef.getUser().getUsername();

            //TODO: check that uid and gid are not null
            String gid = jobDef.getUser().getCredentials().get("gid");
            String uid = jobDef.getUser().getCredentials().get("uid");

            // Retrieve mount points
            ArrayList<MountPoint> mountPoints = jobDef.getMountPoints();

            PrintWriter writer = new PrintWriter(path, "UTF-8");
            // Cloud-init is not working properly on ubuntu16. Only bootcmd commands work correctly.
            writer.println("#cloud-config");
            writer.println("bootcmd:");
            writer.println("  - sudo groupadd -g " + gid + " transplant");
            writer.println("  - sudo useradd -m -d /home/" + user + " -s /bin/bash --uid " + uid + " --gid " + gid + " -G root " + user);
            // NFS mounting
            if (mountPoints.size() > 0) {
                // There are mount points to add to cloud-init
                logger.trace("Adding mounting point to context for the infrastructure: " + infrastructure);
                switch (infrastructure) {
                    case "mug-bsc":
                        for (int i = 0; i < mountPoints.size(); i++) {
                            MountPoint mp = mountPoints.get(i);
                            String target = mp.getTarget();
                            String device = mp.getDevice();
                            String permissions = mp.getPermissions();
                            writer.println("  - sudo mkdir -p " + target);
                            if (permissions.equals("r")) {
                                writer.println("  - sudo mount -o ro -t nfs " + nfs_server + ":" + device + " " + target);
                            } else {
                                writer.println("  - sudo mount -t nfs " + nfs_server + ":" + device + " " + target);
                            }
                        }
                        break;
                    case "mug-irb":
                        logger.trace("[[[ ERROR ]]]: TODO: Add the lines to mount the shared storage at IRB.");
                        break;
                    case "mug-ebi":
                        // Create extra configuration file for second adaptor and restart the new interface to get the IP
                        // This second interface is intended to be used for the NFS storage.
                        // TODO: Check that this link can be done with the same occi query
                        writer.println("  - sudo cp /etc/network/interfaces.d/eth0.cfg /etc/network/interfaces.d/eth1.cfg");
                        writer.println("  - sudo sed -i 's/0/1/g' /etc/network/interfaces.d/eth1.cfg");
                        writer.println("  - sudo ifdown eth1 && sudo ifup eth1");
                        for (int i = 0; i < mountPoints.size(); i++) {
                            MountPoint mp = mountPoints.get(i);
                            String target = mp.getTarget();
                            String device = mp.getDevice();
                            String permissions = mp.getPermissions();
                            writer.println("  - sudo mkdir -p " + target);
                            if (permissions.equals("r")) {
                                writer.println("  - sudo mount -o ro -t nfs " + nfs_server + ":" + device + " " + target);
                            } else {
                                writer.println("  - sudo mount -t nfs " + nfs_server + ":" + device + " " + target);
                            }
                        }
                        break;
                    default:
                        logger.trace("[[[ ERROR ]]]: UNRECOGNIZED INFRASTRUCTURE WHERE TO MOUNT THE FOLDERS.");
                        // TODO: Add and throw new exception
                }
            }

            // enable ssh localhost (COMPSs requirement)
            writer.println("  - sudo -u " + user + " ssh-keygen -f /home/" + user + "/.ssh/id_rsa -t rsa -N \'\'");
            writer.println("  - cat /home/" + user + "/.ssh/id_rsa.pub >> /home/" + user + "/.ssh/authorized_keys");

            // COMPSs environment variables
            // Be careful with the distribution and JAVA installation.
            writer.println("  - echo \"export JAVA_HOME=" + vm_java_home + "\" >> /home/" + user + "/.bashrc");           // Check that there is a symbolic link
            writer.println("  - echo \"source " + vm_compss_home + "/compssenv\" >> /home/" + user + "/.bashrc");  // COMPSs 2.0

            // Add commands that are in config file.
            for (String cmd : this.commands) {
                writer.println("  - " + cmd);
            }

            // Add all ssh-keys that are in config file.
            for (String key : this.auth_keys) {
                writer.println("  - echo \"" + key + "\" >> /home/" + user + "/.ssh/authorized_keys");
            }

            // Redirect cloud-init log to a file
            writer.println("output: {all: '| tee -a /var/log/cloud-init-output.log'}");
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return path;
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
        this.conn_client.destroy(vr.getId());
        // TODO: test if destroy is done correctly
        // Update System Status
        // TODO: occi doesn't give information about what host will be host the VM
        // Host h = systemStatus.getCluster().get(0); //test purposes: always get first Host
        // systemStatus.update(h, -vr.getHd().getTotalComputingUnits(), -vr.getHd().getMemorySize());
        // logger.trace("IM update: "+h.getUsedCores()+" "+h.getUsedMemory());
    }

}
