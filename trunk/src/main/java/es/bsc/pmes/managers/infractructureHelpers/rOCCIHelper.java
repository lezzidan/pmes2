package es.bsc.pmes.managers.infractructureHelpers;

import es.bsc.conn.exceptions.ConnException;
import es.bsc.conn.rocci.ROCCI;
import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.conn.types.VirtualResource;
import es.bsc.pmes.types.JobDefinition;
import es.bsc.pmes.types.MountPoint;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * rOCCI connector helper.
 * 
 * Extends the Infrastructure helper class.
 * This class is used within the Infrastructure Manager class.
 */
public class rOCCIHelper extends InfrastructureHelper {

    // Messos connector client
    private ROCCI conn_client;
    
    /* Main logger */
    private static final Logger logger = LogManager.getLogger(rOCCIHelper.class);

    /* Constructor */
    public rOCCIHelper(String workspace, ArrayList<String> commands, ArrayList<String> auth_keys) {
        super(workspace, commands, auth_keys);
    }
    
    /**
     * ************************************************************************
     * OVERRIDING METHODS
     * ************************************************************************
     */

    @Override
    public HashMap<String, String> configureResource(JobDefinition jobDef, HashMap<String, String> configuration) {
        HashMap<String, String> properties = new HashMap<>();
        String occiEndPoint = configuration.get("endPointROCCI");
        String auth = configuration.get("auth");
        String ca_path = configuration.get("ca-path");
        String link = configuration.get("link");
        String link2 = configuration.get("link2");

        // Default rOCCI server configuration
        properties.put("Server", occiEndPoint);
        properties.put("auth", auth);
        properties.put("link", link);
        if (link2 != null){
            properties.put("link2", link2);
        }

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

        String contextDataFile = createContextDataFile(jobDef, configuration);
        properties.put("context", "user_data=\"file://" + contextDataFile + "\"");

        return properties;
    }
    
    @Override
    public VirtualResource createResource(HardwareDescription hd, SoftwareDescription sd, HashMap<String, String> prop, HashMap<String, String> configuration) throws ConnException {
        String provider = configuration.get("providerName");
        if (Objects.equals("ONE", provider) || Objects.equals("OpenStack", provider)) {
            conn_client = new ROCCI(prop);
            String vrID = (String) conn_client.create(hd, sd, prop);
            logger.trace("compute id: " + vrID);

            VirtualResource vr = conn_client.waitUntilCreation(vrID);

            logger.trace("VM id: " + vr.getId());
            logger.trace("VM ip: " + vr.getIp());
            return vr;
        } else {
            // Unsupported provider
            logger.error("Provider " + provider + " not supported");

            return null;
        }
    }

    @Override
    public void destroyResource(String Id) {

        this.conn_client.destroy(Id);
        // TODO: test if destroy is done correctly
    }

    /**
     * ************************************************************************
     * AUXILIARY METHODS
     * ************************************************************************
     */
    
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
    public String createContextDataFile(JobDefinition jobDef, HashMap<String, String> configuration) {
        logger.trace("Creating context data file");
        String dir = jobDef.getJobName();
        String path = this.getWorkspace() + "/jobs/" + dir + "/context.login";
        logger.trace("Creating context data file " + path);
        try {
            // String infrastructure = jobDef.getInfrastructure();  // TODO: REMOVE INFRASTRUCTURE FROM JOB DEFINITION
            String infrastructure = configuration.get("infrastructure");
            String nfs_server = configuration.get("nfs_server");
            String vm_java_home = configuration.get("vm_java_home");
            String vm_compss_home = configuration.get("vm_compss_home");

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
                        break;
                }
            }

            // enable ssh localhost (COMPSs requirement)
            writer.println("  - sudo -u " + user + " ssh-keygen -f /home/" + user + "/.ssh/id_rsa -t rsa -N \'\'");
            writer.println("  - cat /home/" + user + "/.ssh/id_rsa.pub >> /home/" + user + "/.ssh/authorized_keys");

            // COMPSs environment variables
            // Be careful with the distribution and JAVA installation.
            writer.println("  - echo \"export JAVA_HOME=" + vm_java_home + "\" >> /home/" + user + "/.bashrc");    // Check that there is a symbolic link
            writer.println("  - echo \"source " + vm_compss_home + "/compssenv\" >> /home/" + user + "/.bashrc");

            // Add commands that are in config file.
            for (String cmd : this.getCommands()) {
                writer.println("  - " + cmd);
            }

            // Add all ssh-keys that are in config file.
            for (String key : this.getAuth_keys()) {
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

}
