package es.bsc.pmes.managers.execution;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.bsc.pmes.managers.ConfigurationManager;
import es.bsc.pmes.types.COMPSsJob;
import es.bsc.pmes.types.Job;
import es.bsc.pmes.types.User;

/**
 * SINGLE EXECUTION THREAD Class.
 *
 * This class contains the single execution thread. Objective: execute non
 * COMPSs applications
 *
 * @author scorella on 8/23/16.
 */
public class COMPSsExecutionThread extends AbstractExecutionThread {

	/* Main attributes */
	private COMPSsJob job;

	/* Logger */
	private static final Logger logger = LogManager.getLogger(COMPSsExecutionThread.class);

	/**
	 * Constructor
	 * 
	 * @param job
	 *            COMPSs job to be executed
	 */
	public COMPSsExecutionThread(COMPSsJob job) {
		this.job = job;
	}

	/**
	 * Job getter
	 * 
	 * @return The job
	 */
	@Override
	protected Job getJob() {
		return job;
	}

	/**
	 * Job executor.
	 *
	 * Performs all necessary actions to: - create the resource - do the stage in -
	 * execute the job - do the stage out - destroy the resource - create the job
	 * report
	 */
	@Override
	public void executeJob() {
		logger.debug("COMPSs job execution requested: " + this.job.getId());

		// Check if the user want to stop execution
		// The stop check is done between each stage of the execution
		if (this.stopExecution("", Boolean.FALSE)) {
			logger.debug("Terminate JOB");
			return;
		}
		// Create Resource
		String Id = createResource();
		logger.debug("Resource created with Id: " + Id);

		if (this.stopExecution(Id, Boolean.TRUE)) {
			return;
		}

		// Configure execution
		String resourceAddress = job.getResource(0).getIp(); // get master IP
		String user = job.getUser().getUsername();
		String address = user + "@" + resourceAddress;
		String source = job.getJobDef().getApp().getSource();
		String target = job.getJobDef().getApp().getTarget();
		HashMap<String, String> args = job.getJobDef().getApp().getArgs();

		// Create command to execute
		ArrayList<String> cmd = new ArrayList<>();
		cmd.add("ssh");
		cmd.add(address);
		cmd.add("bash");
		cmd.add("-i"); // interactive session because we want to source environment variables.
		cmd.add("-c"); // needed due to the " over the runcompss command
		String runcompss = "\"runcompss";

		// TODO: test compss flags from request json
		for (String v : job.getJobDef().getCompss_flags().values()) {
			runcompss += v;
		}
		if (job.getJobDef().getApp().getSource().endsWith(".py")) {
			runcompss += " --lang=python";
			runcompss += " --pythonpath=" + target;
		} else {
			runcompss += " --classpath=" + target;
		}
		ConfigurationManager cm = ConfigurationManager.getConfigurationManager();
		String workingDir = cm.getCompssWorkingDir();

		String resources = workingDir + "/resources.xml";
		String project = workingDir + "/project.xml";

		runcompss += " --conn=es.bsc.compss.connectors.DefaultNoSSHConnector";

		runcompss += " --resources=" + resources + " --project=" + project;

		runcompss += " " + target + "/" + source;

		for (Entry<String, String> e : args.entrySet()) {
			runcompss += " --" + e.getKey() + " " + e.getValue();
		}
		runcompss += "\"";

		cmd.add(runcompss);

		/*
		 * // Test redirect output to a file cmd.add(">>"); cmd.add("result.out");
		 */

		String[] command = new String[cmd.size()];
		job.setCmd(cmd.toArray(command));
		logger.debug(Arrays.toString(command));

		// Wait for SSH connectivity
		this.waitForResource(address);

		// Generate and transfer project and resources files
		this.generateCOMPSsConfig();

		this.transferContextFile();

		// FIXME hardcoded
		if (cm.getProviderName().equals("ONE")) {
			this.transferCredentials();
		}

		if (this.stopExecution(Id, Boolean.TRUE)) {
			return;
		}

		// Stage In
		logger.debug("Stage in");
		stageIn();

		if (this.stopExecution(Id, Boolean.TRUE)) {
			return;
		}

		// Run job
		logger.debug("Runnning");

		long startTime = System.currentTimeMillis();

		Integer exitValue = executeCommand(command);

		long endTime = System.currentTimeMillis() - startTime;

		job.getReport().setElapsedTime(String.valueOf(endTime));

		logger.debug("Execution Time: " + String.valueOf(endTime));
		logger.debug("Exit code" + exitValue);

		if (exitValue > 0) {
			if (exitValue == 143) {
				job.setStatus("CANCELLED");
			} else {
				job.setStatus("FAILED");
			}
		} else {
			// Stage Out
			logger.debug("Stage out");
			stageOut();
			job.setStatus("FINISHED");
		}

		// Destroy Resource
		logger.debug("Destroy resource");
		destroyResource(Id);

		// Create Report
		this.job.createReport();
	}

	private void transferCredentials() {
		logger.debug("Transferring credentials...");

		ConfigurationManager cm = ConfigurationManager.getConfigurationManager();
		Job currentJob = this.getJob();
		User user = currentJob.getJobDef().getUser();
		
		String compssWorkingDir = cm.getCompssWorkingDir();		
		String username = user.getUsername();
		String ip = currentJob.getResource(0).getIp();		
		String srcKey = user.getCredentials().get("key");
		String srcPem = user.getCredentials().get("pem");
		String srcCa = cm.getConnectorProperties().get("ca-path");
		String dstCa = username + "@" + ip + ":" + compssWorkingDir;		
		String dstKey = username + "@" + ip + ":" + compssWorkingDir;

		logger.debug("SCP: " + srcCa + " to " + dstCa);

		ProcessBuilder pb = new ProcessBuilder();
		pb.command("scp", "-r", srcCa, dstCa);

		try {
			pb.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		logger.debug("SCP: " + srcKey + " to " + dstCa);

		pb = new ProcessBuilder();
		pb.command("scp", srcKey, dstCa);

		try {
			pb.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		logger.debug("SCP: " + srcPem + " to " + dstCa);

		pb = new ProcessBuilder();
		pb.command("scp", srcPem, dstCa);

		try {
			pb.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void transferContextFile() {
		String workspace = ConfigurationManager.getConfigurationManager().getWorkspace();
		String compssWorkingDir = ConfigurationManager.getConfigurationManager().getCompssWorkingDir();
		String ip = job.getResource(0).getIp();
		String username = job.getUser().getUsername();

		String src = workspace + "/jobs/" + this.getJob().getJobDef().getJobName() + "/context.login";
		String dst = username + "@" + ip + ":" + compssWorkingDir;

		logger.debug("Transferring context file.");

		ProcessBuilder pb = new ProcessBuilder();
		pb.command("scp", src, dst);

		try {
			pb.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void generateCOMPSsConfig() {
		String jobName = job.getJobDef().getJobName();
		String workspace = ConfigurationManager.getConfigurationManager().getWorkspace();
		Path genDir = Paths.get(workspace, ConfigurationManager.JOB_DIR, jobName);

		logger.debug("Generating COMPSs config files in " + genDir.toString());

		job.generateConfigFiles(genDir);

		String ip = job.getResource(0).getIp();
		String username = job.getUser().getUsername();

		String workingDir = ConfigurationManager.getConfigurationManager().getCompssWorkingDir();
		String srcProject = genDir.resolve("project.xml").toString();
		String srcResources = genDir.resolve("resources.xml").toString();

		logger.debug("Transferring project.xml.");

		ProcessBuilder pb = new ProcessBuilder();
		pb.command("scp", srcProject, username + "@" + ip + ":" + workingDir);

		try {
			pb.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		logger.debug("Transferring resources.xml to.");

		pb = new ProcessBuilder();
		pb.command("scp", srcResources, username + "@" + ip + ":" + workingDir);

		try {
			pb.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
