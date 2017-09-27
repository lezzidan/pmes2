package es.bsc.pmes.managers.execution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.bsc.pmes.types.Job;
import es.bsc.pmes.types.SingleJob;

/**
 * SINGLE EXECUTION THREAD Class.
 *
 * This class contains the single execution thread. Objective: execute non
 * COMPSs applications
 *
 * @author scorella on 9/19/16.
 */
public class SingleExecutionThread extends AbstractExecutionThread {

	/* Main attributes */
	private SingleJob job;

	/* Logger */
	private static final Logger logger = LogManager.getLogger(SingleExecutionThread.class);

	/**
	 * Constructor
	 *
	 * @param job
	 *            Job to be executed
	 */
	public SingleExecutionThread(SingleJob job) {
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
		logger.debug("Single job execution requested: " + this.job.getId());

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
		cmd.add("-i"); // beware with this. It is needed in order to load .bashrc
		// cmd.add("-c"); // not needed in this case
		String commandToRun = "";
		commandToRun += target + "/./" + source;

		for (Entry<String, String> e : args.entrySet()) {
			commandToRun += " --" + e.getKey() + " " + e.getValue();
		}
		commandToRun += "";
		cmd.add(commandToRun);

		String[] command = new String[cmd.size()];
		job.setCmd(cmd.toArray(command));
		logger.debug(Arrays.toString(command));

		this.waitForResource(address);			
		
		if (this.stopExecution(Id, Boolean.TRUE)) {
			return;
		}

		// StageIn
		logger.debug("Stage In");
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
			// StageOut
			logger.debug("Stage Out");
			stageOut();
			job.setStatus("FINISHED");
		}

		// Destroy Resource
		logger.debug("Destroy resource");
		destroyResource(Id);

		// Create Report
		this.job.createReport();
	}
}
