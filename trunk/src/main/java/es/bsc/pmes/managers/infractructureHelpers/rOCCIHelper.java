package es.bsc.pmes.managers.infractructureHelpers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.bsc.compss.types.project.ProjectFile;
import es.bsc.compss.types.project.exceptions.InvalidElementException;
import es.bsc.compss.types.project.jaxb.CloudPropertiesType;
import es.bsc.compss.types.project.jaxb.ImageType;
import es.bsc.compss.types.project.jaxb.InstanceTypeType;
import es.bsc.compss.types.resources.ResourcesFile;
import es.bsc.conn.exceptions.ConnException;
import es.bsc.conn.rocci.ROCCI;
import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.conn.types.VirtualResource;
import es.bsc.pmes.managers.ConfigurationManager;
import es.bsc.pmes.managers.InfrastructureManager;
import es.bsc.pmes.types.COMPSsJob;
import es.bsc.pmes.types.JobDefinition;
import es.bsc.pmes.types.MountPoint;
import es.bsc.pmes.types.User;

/**
 * rOCCI connector helper.
 * 
 * Extends the Infrastructure helper class. This class is used within the
 * Infrastructure Manager class.
 */
public class rOCCIHelper extends InfrastructureHelper {

	// Messos connector client
	private ROCCI conn_client;

	/* Main logger */
	private static final Logger logger = LogManager.getLogger(rOCCIHelper.class);

	/* Constructor */
	public rOCCIHelper(String workspace, List<String> commands, List<String> auth_keys) {
		super(workspace, commands, auth_keys);
	}

	/**
	 * ************************************************************************
	 * OVERRIDING METHODS
	 * ************************************************************************
	 */

	@Override
	public Map<String, String> configureResource(JobDefinition jobDef, Map<String, String> configuration) {
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
		if (link2 != null) {
			properties.put("link2", link2);
		}

		if (auth.equals("token")) {
			logger.debug("Authentication method: token");
			logger.debug("token: " + jobDef.getUser().getCredentials().get("token"));
			properties.put("token", jobDef.getUser().getCredentials().get("token"));
		} else {
			logger.debug("Authentication method: pem-key");
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
	public VirtualResource createResource(HardwareDescription hd, SoftwareDescription sd, Map<String, String> prop,
			Map<String, String> configuration) throws ConnException {
		String provider = configuration.get("providerName");
		if (Objects.equals("ONE", provider) || Objects.equals("OpenStack", provider)) {
			conn_client = new ROCCI(prop);
			String vrID = (String) conn_client.create(hd, sd, prop);
			logger.debug("compute id: " + vrID);

			VirtualResource vr = conn_client.waitUntilCreation(vrID);

			logger.debug("VM id: " + vr.getId());
			logger.debug("VM ip: " + vr.getIp());
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
	 * @param jobDef
	 *            Job definition
	 * @return Path where the context file has been stored
	 */
	public String createContextDataFile(JobDefinition jobDef, Map<String, String> configuration) {
		logger.debug("Creating context data file");
		String dir = jobDef.getJobName();
		String path = this.getWorkspace() + "/jobs/" + dir + "/context.login";
		String occiEndPoint = configuration.get("endPointROCCI");
		logger.debug("Creating context data file " + path);
		try {
			// String infrastructure = jobDef.getInfrastructure(); // TODO: REMOVE
			// INFRASTRUCTURE FROM JOB DEFINITION
			String infrastructure = configuration.get("infrastructure");
			String nfs_server = configuration.get("nfs_server");
			// String vm_java_home = configuration.get("vm_java_home");
			// String vm_compss_home = configuration.get("vm_compss_home");
			String occiIP;

			// Get OCCI IP from hostname
			String hostname;

			try {
				URL u = new URL(occiEndPoint);
				hostname = u.getHost();
				InetAddress address = InetAddress.getByName(hostname);
				occiIP = address.getHostAddress();
			} catch (MalformedURLException | UnknownHostException e) {
				throw new RuntimeException(e);
			}

			String user = jobDef.getUser().getUsername();

			// TODO: check that uid and gid are not null
			String gid = jobDef.getUser().getCredentials().get("gid");
			String uid = jobDef.getUser().getCredentials().get("uid");

			// Retrieve mount points
			ArrayList<MountPoint> mountPoints = jobDef.getMountPoints();

			PrintWriter writer = new PrintWriter(path, "UTF-8");
			// Cloud-init is not working properly on ubuntu16. Only bootcmd commands work
			// correctly.
			writer.println("#cloud-config");
			writer.println("bootcmd:");
			writer.println("  - sudo groupadd -g " + gid + " transplant");
			writer.println("  - sudo useradd -m -d /home/" + user + " -s /bin/bash --uid " + uid + " --gid " + gid
					+ " -G root " + user);
			// NFS mounting
			if (mountPoints.size() > 0) {
				// There are mount points to add to cloud-init
				logger.debug("Adding mounting point to context for the infrastructure: " + infrastructure);
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
					logger.debug("[[[ ERROR ]]]: TODO: Add the lines to mount the shared storage at IRB.");
					break;
				case "mug-ebi":
					// Create extra configuration file for second adaptor and restart the new
					// interface to get the IP
					// This second interface is intended to be used for the NFS storage.
					// writer.println(" - sudo cp /etc/network/interfaces.d/eth0.cfg
					// /etc/network/interfaces.d/eth1.cfg");
					// writer.println(" - sudo sed -i 's/0/1/g'
					// /etc/network/interfaces.d/eth1.cfg");
					// writer.println(" - sudo ifdown eth1 && sudo ifup eth1");
					writer.println("  - sudo echo '# The secondary network interface' >> /etc/network/interfaces");
					writer.println("  - sudo echo 'auto eth1' >> /etc/network/interfaces");
					writer.println("  - sudo echo 'iface eth1 inet dhcp' >> /etc/network/interfaces");
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
					logger.debug("[[[ ERROR ]]]: UNRECOGNIZED INFRASTRUCTURE WHERE TO MOUNT THE FOLDERS.");
					// TODO: Add and throw new exception
					break;
				}
			}

			// enable ssh localhost (COMPSs requirement)
			writer.println("  - sudo -u " + user + " ssh-keygen -f /home/" + user + "/.ssh/id_rsa -t rsa -N \'\'");
			writer.println("  - cat /home/" + user + "/.ssh/id_rsa.pub >> /home/" + user + "/.ssh/authorized_keys");

			// COMPSs environment variables
			// Be careful with the distribution and JAVA installation.
			// writer.println(" - sudo echo \"JAVA_HOME=" + vm_java_home + "\" >>
			// /etc/environment"); // Check
			// the
			// cfg
			// file
			// writer.println(" - echo \"source " + vm_compss_home + "/compssenv\" >>
			// /home/" + user + "/.bashrc"); // Only needed if compssenv defined will
			// override any predefined COMPSs env vars of .bashrc.

			// Add OCCI IP to /etc/hosts so COMPSs master can resolve the OCCI endpoint
			writer.println("  - sudo echo \"" + occiIP + "   " + hostname + "\" >> /etc/hosts");

			// Add commands that are in config file.
			for (String cmd : this.getCommands()) {
				writer.println("  - " + cmd);
			}

			// Add all ssh-keys that are in config file.
			for (String key : this.getAuth_keys()) {
				writer.println("  - echo \"" + key + "\" >> /home/" + user + "/.ssh/authorized_keys");
				writer.println("  - echo \"" + key + "\" >> /home/pmes/.ssh/authorized_keys");
			}

			// Redirect cloud-init log to a file
			writer.println("output: {all: '| tee -a /var/log/cloud-init-output.log'}");
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return path;
	}

	private Path createWorkerContextDataFile(JobDefinition jobDef, Map<String, String> configuration,
			String masterKey) {
		logger.debug("Creating context data file");
		String dir = jobDef.getJobName();
		Path path = Paths.get(this.getWorkspace(), "jobs", dir, "master_context.login");
		String occiEndPoint = configuration.get("endPointROCCI");
		logger.debug("Creating context data file " + path);
		try {
			// String infrastructure = jobDef.getInfrastructure(); // TODO: REMOVE
			// INFRASTRUCTURE FROM JOB DEFINITION
			String infrastructure = configuration.get("infrastructure");
			String nfs_server = configuration.get("nfs_server");
			// String vm_java_home = configuration.get("vm_java_home");
			// String vm_compss_home = configuration.get("vm_compss_home");
			String occiIP;

			// Get OCCI IP from hostname
			String hostname;

			try {
				URL u = new URL(occiEndPoint);
				hostname = u.getHost();
				InetAddress address = InetAddress.getByName(hostname);
				occiIP = address.getHostAddress();
			} catch (MalformedURLException | UnknownHostException e) {
				throw new RuntimeException(e);
			}

			String user = jobDef.getUser().getUsername();

			// TODO: check that uid and gid are not null
			String gid = jobDef.getUser().getCredentials().get("gid");
			String uid = jobDef.getUser().getCredentials().get("uid");

			// Retrieve mount points
			ArrayList<MountPoint> mountPoints = jobDef.getMountPoints();

			PrintWriter writer = new PrintWriter(path.toString(), "UTF-8");
			// Cloud-init is not working properly on ubuntu16. Only bootcmd commands work
			// correctly.
			writer.println("#cloud-config");
			writer.println("bootcmd:");
			writer.println("  - sudo groupadd -g " + gid + " transplant");
			writer.println("  - sudo useradd -m -d /home/" + user + " -s /bin/bash --uid " + uid + " --gid " + gid
					+ " -G root " + user);
			// NFS mounting
			if (mountPoints.size() > 0) {
				// There are mount points to add to cloud-init
				logger.debug("Adding mounting point to context for the infrastructure: " + infrastructure);
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
					logger.debug("[[[ ERROR ]]]: TODO: Add the lines to mount the shared storage at IRB.");
					break;
				case "mug-ebi":
					// Create extra configuration file for second adaptor and restart the new
					// interface to get the IP
					// This second interface is intended to be used for the NFS storage.
					// writer.println(" - sudo cp /etc/network/interfaces.d/eth0.cfg
					// /etc/network/interfaces.d/eth1.cfg");
					// writer.println(" - sudo sed -i 's/0/1/g'
					// /etc/network/interfaces.d/eth1.cfg");
					// writer.println(" - sudo ifdown eth1 && sudo ifup eth1");
					writer.println("  - sudo echo '# The secondary network interface' >> /etc/network/interfaces");
					writer.println("  - sudo echo 'auto eth1' >> /etc/network/interfaces");
					writer.println("  - sudo echo 'iface eth1 inet dhcp' >> /etc/network/interfaces");
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
					logger.debug("[[[ ERROR ]]]: UNRECOGNIZED INFRASTRUCTURE WHERE TO MOUNT THE FOLDERS.");
					// TODO: Add and throw new exception
					break;
				}
			}

			// enable ssh localhost (COMPSs requirement)
			writer.println("  - sudo -u " + user + " ssh-keygen -f /home/" + user + "/.ssh/id_rsa -t rsa -N \'\'");
			writer.println("  - cat /home/" + user + "/.ssh/id_rsa.pub >> /home/" + user + "/.ssh/authorized_keys");
			writer.println("  - echo \"" + masterKey + "\" >> /home/" + user + "/.ssh/authorized_keys");

			// COMPSs environment variables
			// Be careful with the distribution and JAVA installation.
			// writer.println(" - sudo echo \"JAVA_HOME=" + vm_java_home + "\" >>
			// /etc/environment"); // Check
			// the
			// cfg
			// file
			// writer.println(" - echo \"source " + vm_compss_home + "/compssenv\" >>
			// /home/" + user + "/.bashrc"); // Only needed if compssenv defined will
			// override any predefined COMPSs env vars of .bashrc.

			// Add OCCI IP to /etc/hosts so COMPSs master can resolve the OCCI endpoint
			writer.println("  - sudo echo \"" + occiIP + "   " + hostname + "\" >> /etc/hosts");

			// Add commands that are in config file.
			for (String cmd : this.getCommands()) {
				writer.println("  - " + cmd);
			}

			// Add all ssh-keys that are in config file.
			for (String key : this.getAuth_keys()) {
				writer.println("  - echo \"" + key + "\" >> /home/" + user + "/.ssh/authorized_keys");
				writer.println("  - echo \"" + key + "\" >> /home/pmes/.ssh/authorized_keys");
			}

			// Redirect cloud-init log to a file
			writer.println("output: {all: '| tee -a /var/log/cloud-init-output.log'}");
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return path;
	}

	@Override
	public void configureCOMPSsMaster(COMPSsJob job) {
		this.transferContextFile(job);
		this.generateCOMPSsConfig(job);

		if (ConfigurationManager.getConfigurationManager().getProviderName().equals("ONE")) {
			this.transferCredentials(job);
		}
	}

	private void transferCredentials(COMPSsJob job) {
		logger.debug("Transferring credentials...");

		ConfigurationManager cm = ConfigurationManager.getConfigurationManager();
		User user = job.getJobDef().getUser();

		String username = user.getUsername();
		String ip = job.getResource(0).getIp();
		String srcKey = user.getCredentials().get("key");
		String srcPem = user.getCredentials().get("pem");
		String srcCa = cm.getConnectorProperties().get("ca-path");
		String dstCa = "pmes@" + ip + ":" + srcCa;
		String dstKey = username + "@" + ip + ":";

		logger.debug("SCP: " + srcCa + " to " + dstCa);

		ProcessBuilder pb = new ProcessBuilder();
		pb.command("scp", "-r", srcCa, dstCa);

		try {
			pb.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		logger.debug("SCP: " + srcKey + " to " + dstKey);

		pb = new ProcessBuilder();
		pb.command("scp", srcKey, dstKey);

		try {
			pb.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		logger.debug("SCP: " + srcPem + " to " + dstKey);

		pb = new ProcessBuilder();
		pb.command("scp", srcPem, dstKey);

		try {
			pb.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void generateCOMPSsConfig(COMPSsJob job) {
		String jobName = job.getJobDef().getJobName();
		String workspace = ConfigurationManager.getConfigurationManager().getWorkspace();
		Path genDir = Paths.get(workspace, ConfigurationManager.JOB_DIR, jobName);

		logger.debug("Generating COMPSs config files in " + genDir.toString());

		this.generateConfigFiles(job, genDir);

		String ip = job.getResource(0).getIp();
		String username = job.getUser().getUsername();
		String workingDir = "/home/" + username;
		String srcProject = genDir.resolve("project.xml").toString();
		String srcResources = genDir.resolve("resources.xml").toString();

		logger.debug("Transferring project.xml.");

		ProcessBuilder pb = new ProcessBuilder();
		pb.command("scp", srcProject, username + "@" + ip + ":" + workingDir);
		// TODO maybe it is better to capture this with logger
		pb.redirectError();

		try {
			Process proc = pb.start();
			proc.waitFor();
			int exit = proc.exitValue();

			if (exit != 0) {
				logger.warn("Unable to transfer project.xml. COMPSs execution might fail.");
			}
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}

		logger.debug("Transferring resources.xml.");

		pb = new ProcessBuilder();
		pb.command("scp", srcResources, username + "@" + ip + ":" + workingDir);
		// TODO maybe it is better to capture this with logger
		pb.redirectError();

		try {
			Process proc = pb.start();
			proc.waitFor();
			int exit = proc.exitValue();

			if (exit != 0) {
				logger.warn("Unable to transfer resources.xml. COMPSs execution might fail.");
			}
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private void transferContextFile(COMPSsJob job) {
		ConfigurationManager cm = ConfigurationManager.getConfigurationManager();
		Map<String, String> properties = cm.getConnectorProperties();
		String compssWorkingDir = "/home/" + job.getUser().getUsername();
		String ip = job.getResource(0).getIp();
		String username = job.getUser().getUsername();

		String masterKey = this.getPublicKey(username, ip);

		if (masterKey == null) {
			logger.warn("Unable to get public key. Context file will not be transferred.");
			return;
		}
		logger.debug("Public key of " + username + " is " + masterKey);

		Path src = this.createWorkerContextDataFile(job.getJobDef(), properties, masterKey);
		String dst = username + "@" + ip + ":" + compssWorkingDir + "/context.login";

		logger.debug("Transferring context file from " + src.toString() + " to " + dst);

		ProcessBuilder pb = new ProcessBuilder();
		pb.command("scp", src.toString(), dst);
		pb.redirectError();

		try {
			Process proc = pb.start();
			proc.waitFor();

			if (proc.exitValue() != 0) {
				logger.warn("Unable to transfer context file to master. COMPSs workers might not be contextualized.");
			}
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private String getPublicKey(String username, String ip) {
		ProcessBuilder pb = new ProcessBuilder();
		pb.command("ssh", username + "@" + ip, "cat .ssh/id_rsa.pub");
		pb.redirectError();
		Scanner sc = null;

		try {
			Process proc = pb.start();
			sc = new Scanner(proc.getInputStream());
			proc.waitFor();

			if (proc.exitValue() == 0) {
				return sc.nextLine();
			} else {
				logger.warn("Unable to retrieve " + ip + " public key.");
				return null;
			}
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			if (sc != null) {
				sc.close();
			}
		}
	}

	private void generateConfigFiles(COMPSsJob job, Path destination) {
		this.generateProject(job, destination);
		this.generateResources(job, destination);
	}

	private void generateProject(COMPSsJob job, Path destination) {
		logger.debug("Generating project.xml file...");

		JobDefinition jobDef = job.getJobDef();
		ConfigurationManager cm = ConfigurationManager.getConfigurationManager();

		String imgName = jobDef.getImg().getImageName();
		String imgType = jobDef.getImg().getImageType();
		String username = jobDef.getUser().getUsername();
		int cores = jobDef.getCores();
		String compssHome = cm.getCompssHome();
		Path compssWorkingDir = Paths.get("/home", username);
		String providerName = cm.getProviderName();

		// Minus 1 because the master is used as a worker (and is already one VM)
		int maxVMs = jobDef.getMaximumVMs() - 1;
		int minVMs = jobDef.getMinimumVMs() - 1;
		int initialVMs = jobDef.getInitialVMs() - 1;

		ImageType image = ProjectFile.createImage(imgName, compssHome, compssWorkingDir.toString(), cores);
		InstanceTypeType instance = ProjectFile.createInstance(imgType);

		Map<String, String> properties = InfrastructureManager.getInfrastructureManager()
				.configureResource(job.getJobDef());

		Path keyFile = Paths.get(jobDef.getUser().getCredentials().get("key")).getFileName();
		Path pemFile = Paths.get(jobDef.getUser().getCredentials().get("pem")).getFileName();

		// FIXME hardcoded
		properties.put("context", "user_data=\"file://" + compssWorkingDir + "/context.login\"");
		properties.put("user-cred", compssWorkingDir.resolve(pemFile).toString());
		properties.put("password", compssWorkingDir.resolve(keyFile).toString());

		CloudPropertiesType cloudProperties = ProjectFile.createCloudProperties(properties);

		List<ImageType> images = new ArrayList<ImageType>();
		images.add(image);

		List<InstanceTypeType> instances = new ArrayList<InstanceTypeType>();
		instances.add(instance);

		try {
			// Add the master as a compute node
			ProjectFile project = new ProjectFile(logger);
			project.addComputeNode(job.getResource(0).getIp(), compssHome, compssWorkingDir.toString(), username,
					cores);

			// Setup the cloud provider for elasticity
			project.addCloudProvider(providerName, images, instances, maxVMs, cloudProperties);
			project.setCloudProperties(initialVMs, minVMs, maxVMs);

			project.toFile(destination.resolve("project.xml").toFile());
		} catch (InvalidElementException | JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	private void generateResources(COMPSsJob job, Path destination) {
		logger.debug("Generating resources.xml file...");

		JobDefinition jobDef = job.getJobDef();
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
			resources.addComputeNode(job.getResource(0).getIp(), "proc", cores, "es.bsc.compss.nio.master.NIOAdaptor",
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
