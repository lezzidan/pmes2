package es.bsc.pmes.managers;

import es.bsc.conn.exceptions.ConnException;
//import es.bsc.conn.exceptions.ConnectorException;
import es.bsc.conn.rocci.ROCCI;
//import es.bsc.conn.clients.rocci.RocciClient;

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
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by scorella on 8/5/16.
 * Singleton class
 */

public class InfrastructureManager {
    private static InfrastructureManager infrastructureManager = new InfrastructureManager();
    private ROCCI rocciClient;
    //private RocciClient rocciClient;
    private HashMap<String, Resource> activeResources;
    private SystemStatus systemStatus;
    private String provider;
    private String occiEndPoint;
    private String auth;
    private String ca_path;
    private String link;
    private ArrayList<String> commands;
    private ArrayList<String> auth_keys;

    private static final Logger logger = LogManager.getLogger(InfrastructureManager.class);

    private InfrastructureManager() {
        this.activeResources = new HashMap<>();
        this.provider = "ONE"; //openNebula by default
        this.systemStatus = new SystemStatus();
        this.auth_keys = new ArrayList<>();
        this.commands = new ArrayList<>();
        this.ca_path = "";
        this.link = "";
        this.occiEndPoint = "";
        try {
            configureResources();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

    }

    public static InfrastructureManager getInfrastructureManager(){
        return infrastructureManager;
    }

    public String createResource(HardwareDescription hd, SoftwareDescription sd, HashMap<String, String> prop){
        // TODO: revisar tipo salida
        logger.trace("Creating Resource");
        if (Objects.equals("ONE", this.provider) || Objects.equals("OpenStack", this.provider)) {
            try {
                rocciClient = new ROCCI(prop);
                String vrID = (String) rocciClient.create(hd, sd, prop);
                logger.trace("compute id: " + vrID);

                VirtualResource vr = rocciClient.waitUntilCreation(vrID);

                logger.trace("VM id: " + vr.getId());
                logger.trace("VM ip: " + vr.getIp());

                // Update System Status
                //TODO: occi doesn't give information about what host will be host the VM
                Host h = systemStatus.getCluster().get(0); //test purposes: always get first Host
                systemStatus.update(h, hd.getTotalComputingUnits(), hd.getMemorySize());
                logger.trace("IM update: "+hd.getTotalComputingUnits() +" "+ hd.getMemorySize());
                Resource newResource = new Resource(vr.getIp(), prop, vr);
                activeResources.put((String) vr.getId(), newResource);
                return (String) vr.getId();

            } catch (ConnException e) {
                logger.error("Error creating resource");
                e.printStackTrace();
                return "-1";
            }
        }
        else {
            logger.error("Provider "+this.provider+" not supported");
            return null;
        }
    }

    public HashMap<String, String> configureResource(JobDefinition jobDef){
        HashMap<String, String> properties = new HashMap<>();
        // Default rocci server configuration
        properties.put("Server", this.occiEndPoint);
        properties.put("auth", this.auth);
        properties.put("link", this.link);
        if (this.auth.equals("token")) {
            logger.trace("Authentication method: token");
            logger.trace("token: "+jobDef.getUser().getCredentials().get("token"));
            properties.put("token", jobDef.getUser().getCredentials().get("token"));
        }
        else {
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
        properties.put("context", "user_data=\"file://"+contextDataFile+"\"");
        return properties;
    }

    public String createContextDataFile(JobDefinition jobDef){
        logger.trace("Creating context data file");
        String dir = jobDef.getJobName();
        String path = "/home/pmes/pmes/jobs/"+dir+"/context.login";
        logger.trace("Creating context data file "+path);
        try {
            String user = jobDef.getUser().getUsername();
            String gid = jobDef.getUser().getCredentials().get("gid");
            String uid = jobDef.getUser().getCredentials().get("uid");
            String mount = jobDef.getMountPath();
            /*
            //Test Purposes
            if (mount.equals("bsccv02")) {
                mount = "/transplant/testUser/test/";
            }
            logger.trace("MOUNT PATH: "+mount);
            logger.trace("UID and GID: "+uid+" "+gid);
            if (!uid.equals("306") || !gid.equals("306")){
                uid = "306";
                gid = "306";
            }*/
            //TODO: if uid and gid are null fails
            PrintWriter writer = new PrintWriter(path, "UTF-8");
            // Cloud-init is not working properly on ubuntu16. Only bootcmd commands work correctly.
            writer.println("#cloud-config");
            writer.println("bootcmd:");
            writer.println("  - sudo groupadd -g "+gid+" transplant");
            writer.println("  - sudo useradd -m -d /home/"+user+" -s /bin/bash --uid "+uid+" --gid "+gid+" -G root "+user);
            if (!mount.equals("")) {
                writer.println("  - sudo mkdir -p /transplant");
                // this line should change if the mount method is not CIFS.
                writer.println("  - sudo mount -t cifs //192.168.122.253/INBTransplant /transplant -o user=guest,password=guestTransplant01,rsize=130048,sec=ntlmssp");
                // change home directory to mount path.
                writer.println("  - sudo mv /home/" + user + " /tmp");
                writer.println("  - sudo ln -s " + mount + " /home/" + user);
            }
            // enable ssh localhost (COMPSs requirement)
            writer.println("  - sudo -u "+user+" ssh-keygen -f /home/"+user+"/.ssh/id_rsa -t rsa -N \'\'" );
            writer.println("  - cat /home/"+user+"/.ssh/id_rsa.pub >> /home/"+user+"/.ssh/authorized_keys");
            // COMPSs environment variables
            //writer.println("  - echo \"export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64\" >> /home/"+user+"/.bashrc");  // BSC
            writer.println("  - echo \"export JAVA_HOME=/usr/lib/jvm/java-8-oracle/\" >> /home/"+user+"/.bashrc");              // EBI
            //writer.println("  - echo \"export PATH=$PATH:/opt/COMPSs/Runtime/scripts/user:/opt/COMPSs/Bindings/c/bin\" >> /home/"+user+"/.bashrc");
            writer.println("  - echo \"source /opt/COMPSs/compssenv\" >> /home/"+user+"/.bashrc"); //COMPSs 2.0

            // Add commands that are in config file.
            for (String cmd: this.commands) {
                writer.println("  - "+cmd);
            }

            // Add all ssh-keys that are in config file.
            for (String key:this.auth_keys) {
                writer.println("  - echo \""+key+"\" >> /home/"+user+"/.ssh/authorized_keys");
            }

            // Redirect cloud-init log to a file
            writer.println("output: {all: '| tee -a /var/log/cloud-init-output.log'}");
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return path;
    }

    public void destroyResource(String Id){
        VirtualResource vr = activeResources.get(Id).getVr();
        logger.trace("Destroying VM " + Id);
        rocciClient.destroy(vr.getId());
        // TODO: test if destroy is done correctly
        // Update System Status
        //TODO: occi doesn't give information about what host will be host the VM
        // Host h = systemStatus.getCluster().get(0); //test purposes: always get first Host
        //systemStatus.update(h, -vr.getHd().getTotalComputingUnits(), -vr.getHd().getMemorySize());
        //logger.trace("IM update: "+h.getUsedCores()+" "+h.getUsedMemory());
    }

    public void configureResources() throws ParserConfigurationException {
        File fXML = new File("/home/pmes/pmes/config/config.xml"); //TODO path
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = null;
        try {
            doc = dBuilder.parse(fXML);
        } catch (SAXException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        doc.getDocumentElement().normalize();
        //TODO: set log path dir, log level
        //System.out.println(doc.getDocumentElement().getElementsByTagName("logLevel").item(0).getTextContent());
        //System.out.println(doc.getDocumentElement().getElementsByTagName("logPath").item(0).getTextContent());

        this.provider = doc.getDocumentElement().getElementsByTagName("providerName").item(0).getAttributes().getNamedItem("Name").getTextContent();
        this.occiEndPoint = doc.getDocumentElement().getElementsByTagName("endPointROCCI").item(0).getTextContent();
        this.auth = doc.getDocumentElement().getElementsByTagName("auth").item(0).getTextContent();
        this.ca_path = doc.getDocumentElement().getElementsByTagName("ca-path").item(0).getTextContent();
        this.link = doc.getDocumentElement().getElementsByTagName("link").item(0).getTextContent();

        NodeList nlist = doc.getElementsByTagName("host");
        for (int i = 0; i < nlist.getLength(); i++){
            Node node = nlist.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE){
                Element element = (Element) node;
                String name = element.getElementsByTagName("name").item(0).getTextContent();
                Integer cpu = Integer.valueOf(element.getElementsByTagName("MAX_CPU").item(0).getTextContent());
                Float mem = Float.valueOf(element.getElementsByTagName("MAX_MEM").item(0).getTextContent());
                Host host = new Host(name, cpu, mem);
                systemStatus.addHost(host);
            }
        }

        NodeList keys = doc.getElementsByTagName("key");
        for (int i = 0; i < keys.getLength(); i++){
            Node node = keys.item(i);
            this.auth_keys.add(node.getTextContent());
        }

        NodeList cmds = doc.getElementsByTagName("cmd");
        for (int i = 0; i < cmds.getLength(); i++){
            Node node = cmds.item(i);
            this.commands.add(node.getTextContent());
        }
    }


    /** GETTERS AND SETTERS*/
    public HashMap<String, Resource> getActiveResources() { return activeResources; }

    public void setActiveResources(HashMap<String, Resource> activeResources) { this.activeResources = activeResources; }

    public String getProvider() { return provider; }

    public void setProvider(String provider) { this.provider = provider; }

    public SystemStatus getSystemStatus(){
        return this.systemStatus; }
}
