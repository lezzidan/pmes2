package es.bsc.pmes.managers.execution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.bsc.pmes.types.COMPSsJob;
import es.bsc.pmes.types.Job;

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
		logger.trace("COMPSs job execution requested: " + this.job.getId());

		// Check if the user want to stop execution
		// The stop check is done between each stage of the execution
		if (this.stopExecution("", Boolean.FALSE)) {
			logger.trace("Terminate JOB");
			return;
		}
		// Create Resource
		String Id = createResource();
		logger.trace("Resource created with Id: " + Id);

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
		logger.trace(Arrays.toString(command));
		
		this.waitForResource(address);

		if (this.stopExecution(Id, Boolean.TRUE)) {
			return;
		}

		// Stage In
		logger.trace("Stage in");
		stageIn();

		if (this.stopExecution(Id, Boolean.TRUE)) {
			return;
		}

		// Run job
		logger.trace("Runnning");
		long startTime = System.currentTimeMillis();
		Integer exitValue = executeCommand(command);
		long endTime = System.currentTimeMillis() - startTime;
		job.getReport().setElapsedTime(String.valueOf(endTime));
		logger.trace("Execution Time: " + String.valueOf(endTime));
		logger.trace("Exit code" + exitValue);
		if (exitValue > 0) {
			if (exitValue == 143) {
				job.setStatus("CANCELLED");
			} else {
				job.setStatus("FAILED");
			}
		} else {
			// Stage Out
			logger.trace("Stage out");
			stageOut();
			job.setStatus("FINISHED");
		}

		// Destroy Resource
		logger.trace("Destroy resource");
		destroyResource(Id);

		// Create Report
		this.job.createReport();
	}	
}
