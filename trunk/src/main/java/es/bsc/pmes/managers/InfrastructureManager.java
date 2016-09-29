package es.bsc.pmes.managers;

import es.bsc.conn.exceptions.ConnectorException;
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
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * Created by scorella on 8/5/16.
 * Singleton class
 */

public class InfrastructureManager {
    private static InfrastructureManager infrastructureManager = new InfrastructureManager();
    private ROCCI rocciClient;
    private HashMap<String, Resource> activeResources;
    private SystemStatus systemStatus;
    private String provider;

    private static final Logger logger = LogManager.getLogger(InfrastructureManager.class.getName());

    private InfrastructureManager() {
        this.activeResources = new HashMap<>();
        this.provider = "one"; //openNebula by default
        this.systemStatus = new SystemStatus();
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
        if (Objects.equals("one", this.provider)) {
            try {
                rocciClient = new ROCCI(prop);
                VirtualResource vr = (VirtualResource) rocciClient.create(hd, sd, prop);
                logger.trace("compute id: " + vr.getId());

                vr = rocciClient.waitUntilCreation(vr.getId());
                logger.trace("VM id: " + vr.getId());
                logger.trace("VM ip: " + vr.getIp());
                // Update System Status
                //TODO know host
                Host h = systemStatus.getCluster().get(0); //test purposes
                //systemStatus.update(h, vr.getHd().getTotalComputingUnits(), vr.getHd().getMemorySize());
                // TODO: Usar VirtualResource
                Resource newResource = new Resource(vr.getIp(), prop, vr);
                activeResources.put((String) vr.getId(), newResource);
                return (String) vr.getId();

            } catch (ConnectorException e) {
                logger.error("Error creating resource");
                e.printStackTrace();
                return "-1";
            }
        }
        else {
            logger.error("Provider connector not supported");
            return null;
        }
    }

    public HashMap<String, String> configureResource(JobDefinition jobDef){
        HashMap<String, String> properties = new HashMap<>();

        // Default rocci server configuration
        properties.put("Server", "https://rocci-server.bsc.es:11443");
        properties.put("auth", "x509");
        properties.put("ca-path", "/etc/grid-security/certificates/");

        String keyPath = jobDef.getUser().getCredentials().get("key");
        String pemPath = jobDef.getUser().getCredentials().get("pem");
        properties.put("password", keyPath);
        properties.put("user-cred", pemPath);

        properties.put("owner", jobDef.getUser().getUsername());
        properties.put("jobname", jobDef.getJobName());

        //TODO: context data file
        properties.put("context", "user_data=\\\"file:///home/pmes/pmes/config/tmpfedcloud.login\\\"");

        return properties;
    }

    public void destroyResource(String Id){
        VirtualResource vr = activeResources.get(Id).getVr();
        logger.trace("Destroying VM " + Id);
        rocciClient.destroy(vr.getId());

        // Update System Status
        //TODO know host
        Host h = systemStatus.getCluster().get(0); //test purposes
        //systemStatus.update(h, -vr.getHd().getTotalComputingUnits(), -vr.getHd().getMemorySize());

    }

    public void configureResources() throws ParserConfigurationException {
        // TODO: change path
        //File fXML = new File("/home/bscuser/subversion/projects/pmes2/trunk/src/main/resources/config.xml"); //TODO path
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
        System.out.println("Root element :"
                + doc.getDocumentElement().getNodeName());
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


    }


    /** GETTERS AND SETTERS*/
    public HashMap<String, Resource> getActiveResources() { return activeResources; }

    public void setActiveResources(HashMap<String, Resource> activeResources) { this.activeResources = activeResources; }

    public String getProvider() { return provider; }

    public void setProvider(String provider) { this.provider = provider; }

    public SystemStatus getSystemStatus(){
        return this.systemStatus; }
}
