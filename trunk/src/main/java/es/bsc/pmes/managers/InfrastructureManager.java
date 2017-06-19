package es.bsc.pmes.managers;

import es.bsc.conn.exceptions.ConnException;
import es.bsc.conn.rocci.ROCCI;

import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.conn.types.VirtualResource;

import es.bsc.pmes.types.Host;
import es.bsc.pmes.types.JobDefinition;
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

/**
 * INFRASTRUCTURE MANAGER Class.
 *
 * This class is aimed at managing the underlying infrastructure. To this end,
 * uses the connectors available at the conn package.
 *
 * - Singleton class. - Currently using the rOCCI connector.
 *
 * @author scorella on 8/5/16.
 */
public class InfrastructureManager {

    /* Static infrastructure manager */
    private static InfrastructureManager infrastructureManager = new InfrastructureManager();
    /* Connector to use */
    private ROCCI rocciClient;
    /* Active resources hashmap - defined in the configuration xml */
    private HashMap<String, Resource> activeResources;
    /* SystemStatus object */
    private SystemStatus systemStatus;
    /* Information contained within the pmes configuration xml */
    private String provider;             // Provider (e.g. ONE, OpenStack)
    private String occiEndPoint;         // rOCCI endpoint
    private String auth;                 // Authentication method (e.g. x509, token)
    private String ca_path;              // Certification authority path
    private String link;                 // Network link - Defined by the provider
    private String link2;                // Network link 2 - If extra network link is needed
    private ArrayList<String> commands;  // Commands to include within the contextualization
    private ArrayList<String> auth_keys; // Ssh authorized keys (for the contextualization)
    private String logLevel;             // Log level (e.g. DEBUG, INFO)
    private String logPath;              // Path where to put the logs

    /* Main logger */
    private static final Logger logger = LogManager.getLogger(InfrastructureManager.class);

    /**
     * CONSTRUCTOR. Default constructor.
     */
    private InfrastructureManager() {
        this.activeResources = new HashMap<>();
        this.provider = "ONE"; // openNebula by default
        this.systemStatus = new SystemStatus();
        this.auth_keys = new ArrayList<>();
        this.commands = new ArrayList<>();
        this.ca_path = "";
        this.link = "";
        this.link2 = "";
        this.occiEndPoint = "";
        this.logLevel = "DEBUG";
        this.logPath = "/tmp";
        try {
            configureResources();
        } catch (ParserConfigurationException e) {
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
     */
    public void configureResources() throws ParserConfigurationException {
        File fXML = new File("/home/pmes/pmes/config/config.xml"); // TODO: set this path as configuration (flag?)
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = null;
        try {
            doc = dBuilder.parse(fXML);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        doc.getDocumentElement().normalize();

        this.provider = doc.getDocumentElement().getElementsByTagName("providerName").item(0).getAttributes().getNamedItem("Name").getTextContent();
        this.occiEndPoint = doc.getDocumentElement().getElementsByTagName("endPointROCCI").item(0).getTextContent();
        this.auth = doc.getDocumentElement().getElementsByTagName("auth").item(0).getTextContent();
        this.ca_path = doc.getDocumentElement().getElementsByTagName("ca-path").item(0).getTextContent();
        this.link = doc.getDocumentElement().getElementsByTagName("link").item(0).getTextContent();
        this.link2 = doc.getDocumentElement().getElementsByTagName("link2").item(0).getTextContent();
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
     * Provider getter
     *
     * @return The infrastructure provider
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Provider setter
     *
     * @param provider The new infrastructure provider
     */
    public void setProvider(String provider) {
        this.provider = provider;
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
        if (Objects.equals("ONE", this.provider) || Objects.equals("OpenStack", this.provider)) {
            try {
                rocciClient = new ROCCI(prop);
                String vrID = (String) rocciClient.create(hd, sd, prop);
                logger.trace("compute id: " + vrID);

                if (Objects.equals("OpenStack", this.provider) && !"".equals(this.link2)) {
                    // If we are using OpenStack, like at EBI, it is necessary 
                    // to attach a new network interface for mounting the nfs.
                    logger.trace("[EBI] Attaching new network interface for storage sharing purposes (nfs).");
                    // TODO: ADD THE CALL TO rOCCI client.
                    rocciClient.attachLink(vrID, this.link2);
                }

                VirtualResource vr = rocciClient.waitUntilCreation(vrID);

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
            } catch (ConnException e) {
                logger.error("Error creating resource");
                e.printStackTrace();
                return "-1";
            }
        } else {
            // Unsupported provider
            logger.error("Provider " + this.provider + " not supported");
            return null;
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

        // Default rOCCI server configuration
        properties.put("Server", this.occiEndPoint);
        properties.put("auth", this.auth);
        properties.put("link", this.link);

        if (this.auth.equals("token")) {
            logger.trace("Authentication method: token");
            logger.trace("token: " + jobDef.getUser().getCredentials().get("token"));
            properties.put("token", jobDef.getUser().getCredentials().get("token"));
        } else {
            logger.trace("Authentication method: pem-key");
            properties.put("ca-path", this.ca_path);
            String keyPath = jobDef.getUser().getCredentials().get("key");
            String pemPath = jobDef.getUser().getCredentials().get("pem");
            properties.put("password", keyPath);
            properties.put("user-cred", pemPath);
        }

        properties.put("owner", jobDef.getUser().getUsername());
        properties.put("jobname", jobDef.getJobName());

        String contextDataFile = createContextDataFile(jobDef);
        properties.put("context", "user_data=\"file://" + contextDataFile + "\"");
        return properties;
    }

    /**
     * CREATE CONTEXT FILE METHOD.
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
        String path = "/home/pmes/pmes/jobs/" + dir + "/context.login";
        logger.trace("Creating context data file " + path);
        try {
            String user = jobDef.getUser().getUsername();
            //TODO: check that uid and gid are not null
            String gid = jobDef.getUser().getCredentials().get("gid");
            String uid = jobDef.getUser().getCredentials().get("uid");
            String mount = jobDef.getMountPath();

            PrintWriter writer = new PrintWriter(path, "UTF-8");
            // Cloud-init is not working properly on ubuntu16. Only bootcmd commands work correctly.
            writer.println("#cloud-config");
            writer.println("bootcmd:");
            writer.println("  - sudo groupadd -g " + gid + " transplant");
            writer.println("  - sudo useradd -m -d /home/" + user + " -s /bin/bash --uid " + uid + " --gid " + gid + " -G root " + user);
            /*  // CIFS folder mounting example - Deprecated
            if (mount.equals("transplant")) {
                writer.println("  - sudo mkdir -p /transplant");
                // this line should change if the mount method is not CIFS.
                writer.println("  - sudo mount -t cifs //192.168.122.253/INBTransplant /transplant -o user=guest,password=guestTransplant01,rsize=130048,sec=ntlmssp");
                // change home directory to mount path.
                writer.println("  - sudo mv /home/" + user + " /tmp");
                writer.println("  - sudo ln -s " + mount + " /home/" + user);
            }*/
            // NFS mounting
            if (!mount.equals("")) {
                // cloud = mug-bsc
                // mountpoints = {dest : mountpoint, dest : mountpoint}  --- {"/sharedisk/":"/data/cloud/", "/sharedisk2/":"/data/cloud/public/"}
                logger.trace("Adding context lines for the shared storage.");
                String[] parts = mount.split(":/");
                String cloud = parts[0];
                String shared_path = parts[1];
                logger.trace("Cloud where to mount: " + cloud + " with folder: " + shared_path);
                if (cloud.equals("mug-bsc")) {
                    writer.println("  - sudo mkdir -p /sharedisk");
                    // this line should change if the mount method is not CIFS.
                    writer.println("  - sudo mount -t nfs 192.168.122.222:" + shared_path + " /sharedisk/");
                    // Hardcoded example:
                    // writer.println("  - sudo mkdir -p /mug-bsc");
                    // writer.println("  - sudo mount -t nfs 192.168.122.222:/data/cloud /mug-bsc/");
                }
                if (cloud.equals("mug-irb")) {
                    // TODO: ADD THE LINES TO MOUNT THE SHARED STORAGE AT EBI
                    logger.trace("[[[ ERROR ]]]: TODO: Add the lines to mount the shared storage at IRB.");
                }
                if (cloud.equals("mug-ebi")) {
                    // TODO: ADD THE LINES TO MOUNT THE SHARED STORAGE AT EBI
                    logger.trace("[[[ ERROR ]]]: TODO: Add the lines to mount the shared storage at EBI.");
                }
            }
            // enable ssh localhost (COMPSs requirement)
            writer.println("  - sudo -u " + user + " ssh-keygen -f /home/" + user + "/.ssh/id_rsa -t rsa -N \'\'");
            writer.println("  - cat /home/" + user + "/.ssh/id_rsa.pub >> /home/" + user + "/.ssh/authorized_keys");

            // COMPSs environment variables
            // Be careful with the distribution and JAVA installation.
            // TODO: this should be included into config xml.
            // writer.println("  - echo \"export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64\" >> /home/"+user+"/.bashrc");  // BSC
            writer.println("  - echo \"export JAVA_HOME=/usr/lib/jvm/java-8-oracle/\" >> /home/" + user + "/.bashrc");              // EBI
            //writer.println("  - echo \"export PATH=$PATH:/opt/COMPSs/Runtime/scripts/user:/opt/COMPSs/Bindings/c/bin\" >> /home/"+user+"/.bashrc");
            writer.println("  - echo \"source /opt/COMPSs/compssenv\" >> /home/" + user + "/.bashrc"); // COMPSs 2.0

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
        rocciClient.destroy(vr.getId());
        // TODO: test if destroy is done correctly
        // Update System Status
        // TODO: occi doesn't give information about what host will be host the VM
        // Host h = systemStatus.getCluster().get(0); //test purposes: always get first Host
        // systemStatus.update(h, -vr.getHd().getTotalComputingUnits(), -vr.getHd().getMemorySize());
        // logger.trace("IM update: "+h.getUsedCores()+" "+h.getUsedMemory());
    }

}
