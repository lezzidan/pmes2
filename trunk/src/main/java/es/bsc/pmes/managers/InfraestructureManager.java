package es.bsc.pmes.managers;

import es.bsc.conn.exceptions.ConnectorException;
import es.bsc.conn.rocci.ROCCI;
import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.conn.types.VirtualResource;

import es.bsc.pmes.types.JobDefinition;
import es.bsc.pmes.types.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Objects;

/**
 * Created by scorella on 8/5/16.
 */

public class InfraestructureManager {
    private ROCCI rocciClient;
    private HashMap<String, Resource> activeResources;
    private String provider;

    private static final Logger logger = LogManager.getLogger(InfraestructureManager.class.getName());

    public InfraestructureManager() {
        this.activeResources = new HashMap<>();
        this.provider = "one";
    }

    public InfraestructureManager(String provider) {
        this.activeResources = new HashMap<>();
        this.provider = provider;
    }

    public String createResource(HardwareDescription hd, SoftwareDescription sd, HashMap<String, String> prop){
        // TODO: revisar tipo salida
        logger.trace("Creating Resource");
        if (Objects.equals("one", this.provider)) {
            /*HardwareDescription hd = new HardwareDescription();
            SoftwareDescription sd = new SoftwareDescription();
            sd.setImageName("uuid_pmestestingocci_68");
            sd.setImageType("small");

            prop.put("Server", "https://rocci-server.bsc.es:11443");
            prop.put("auth", "x509");
            prop.put("password", "~/certs/test/scorella_test.key");
            prop.put("ca-path", "/etc/grid-security/certificates/");
            prop.put("user-cred", "~/certs/test/scorella_test.pem");
            prop.put("owner", "scorella");
            prop.put("jobname", "test");
            prop.put("context", "user_data=\"file://$PWD/tmpfedcloud.login\"");*/

            try {
                rocciClient = new ROCCI(prop);
                VirtualResource vr = (VirtualResource) rocciClient.create(hd, sd, prop);
                vr = rocciClient.waitUntilCreation(vr);

                logger.trace("VM id: " + vr.getId());
                logger.trace("VM ip: " + vr.getIp());

                // TODO: Usar VirtualResource
                Resource newResource = new Resource(vr.getIp(), prop, vr);
                activeResources.put((String) vr.getId(), newResource);
                return (String) vr.getId();

            } catch (ConnectorException e) {
                logger.error("Error creating resource");
                e.printStackTrace();
            }
        }
        else {
            logger.error("Provider connector not supported");
            return null;
        }
        return null;
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
        properties.put("context", "user_data=\"file://$PWD/tmpfedcloud.login\"");

        return properties;
    }

    public void destroyResource(String Id){
        VirtualResource vr = activeResources.get(Id).getVr();
        logger.trace("Destroying VM " + Id);
        rocciClient.destroy(vr.getId());

    }

    public void getSystemStatus(){
        // TODO
    }

    /** GETTERS AND SETTERS*/
    public HashMap<String, Resource> getActiveResources() { return activeResources; }

    public void setActiveResources(HashMap<String, Resource> activeResources) { this.activeResources = activeResources; }

    public String getProvider() { return provider; }

    public void setProvider(String provider) { this.provider = provider; }
}
