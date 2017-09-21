package es.bsc.pmes.managers.execution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.pmes.managers.InfrastructureManager;
import es.bsc.pmes.types.Job;

/**
 * ABSTRACT EXECUTION THREAD Class.
 *
 * This class contains the abstract execution thread.
 *
 * - Singleton class. - Abstracts the execution thread for COMPSs and Single
 * modes. - COMPSs - Execute an application with COMPSs within a resource. -
 * Single - Execute an application without COMPSs within a resource.
 *
 * @author scorella on 9/8/16.
 */
public abstract class AbstractExecutionThread extends Thread implements ExecutionThread {

	/* Main attributes */
	private InfrastructureManager im = InfrastructureManager.getInfrastructureManager();
	private Process process = null;

	/* Logger */
	private static final Logger logger = LogManager.getLogger(AbstractExecutionThread.class);

	/**
	 * Thread run method.
	 */
	public void run() {
		executeJob();
	}

	/**
	 * Abstract job getter
	 *
	 * @return the job being executed
	 */
	protected abstract Job getJob();

	/**
	 * Abstract execution method.
	 */
	public abstract void executeJob();

	/**
	 * Cancel the current job execution.
	 *
	 * @throws Exception
	 *             Something wrong happened
	 */
	@Override
	public void cancel() throws Exception {
		if (this.process != null) {
			this.process.destroy();
			logger.trace("Job cancelled: Execution stopped");
			getJob().setStatus("CANCELLED");
		}
	}

	/**
	 * Does the stage in.
	 */
	public void stageIn() {
		logger.trace("Staging In");
		Integer failedTransfers = getJob().stage(0);
		logger.trace("Failed Transfers: " + failedTransfers.toString());
	}

	/**
	 * Does the stage out.
	 */
	public void stageOut() {
		logger.trace("Staging Out");
		Integer failedTransfers = getJob().stage(1);
		logger.trace("Failed Transfers: " + failedTransfers.toString());
	}

	/**
	 * Create a resource (eg VM).
	 *
	 * Takes into account the resource petition: - Hardware description - Software
	 * description - Job properties
	 *
	 * @return The new resource id
	 */
	public String createResource() {

		// Configuring Hardware
		HardwareDescription hd = new HardwareDescription();
		hd.setMemorySize(getJob().getJobDef().getMemory());
		hd.setTotalComputingUnits(getJob().getJobDef().getCores() * getJob().getJobDef().getNumNodes());
		hd.setImageType(getJob().getJobDef().getImg().getImageType());
		hd.setImageName(getJob().getJobDef().getImg().getImageName());

		// Configure software
		SoftwareDescription sd = new SoftwareDescription();

		// Configure properties
		HashMap<String, String> prop = this.im.configureResource(getJob().getJobDef());

		// Create resource
		logger.trace("Creating new Resource");
		String Id = this.im.createResource(hd, sd, prop);
		logger.trace("New resource created with Id " + Id);
		getJob().addResource(this.im.getActiveResources().get(Id));
		return Id;
	}

	/**
	 * Destroy a resource (eg VM).
	 *
	 * Deletes an existing resource within the infrastructure manager.
	 *
	 * @param Id
	 *            The resource id to be destroyed
	 */
	public void destroyResource(String Id) {
		logger.trace("Deleting Resource: " + Id);
		this.im.destroyResource(Id);
		logger.trace("Resource deleted: " + Id);
	}

	/**
	 * Command executor.
	 *
	 * @param cmd
	 *            Command to be executed
	 * @return The execution exit value
	 */
	public Integer executeCommand(String[] cmd) {
		// Runtime runtime = Runtime.getRuntime();
		Integer times = 3;
		Integer exitValue = 1;
		Integer i = 0;
		while (i < times && exitValue != 0) {
			logger.trace("Round " + String.valueOf(i));
			logger.trace("Command: " + Arrays.toString(cmd));

			// if (i > 0) {
			// // Wait until vm will be ready
			// try {
			// Thread.sleep(60000);
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
			// }
			try {
				ProcessBuilder pb = new ProcessBuilder(cmd);
				pb.redirectErrorStream(true);
				this.process = pb.start();

				BufferedReader in = new BufferedReader(new InputStreamReader(this.process.getInputStream()));

				// Output log
				String outStr = "";
				String line = null;
				Job job = getJob();

				while ((line = in.readLine()) != null) {
					outStr += line;
					outStr += "\n";
					job.getReport().setJobOutputMessage(outStr);
				}
				in.close();
				logger.trace("out: " + outStr);

				this.process.waitFor();
				exitValue = this.process.exitValue();
				logger.trace("Exit Value " + String.valueOf(exitValue));
				job.getReport().setExitValue(exitValue);

			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			i += 1;
		}
		return exitValue;
	}

	/**
	 * Stop execution.
	 *
	 * @param Id
	 *            Resource id
	 * @param destroyResource
	 *            if the resource has to be destroyed
	 * @return True if successful or False if failed.
	 */
	public Boolean stopExecution(String Id, Boolean destroyResource) {
		if (Id == null || Id.equals("-1")) {
			getJob().setStatus("FAILED");
			getJob().getReport().setJobErrorMessage("OCCI has failed creating the resource");
			return Boolean.TRUE;
		}
		if (getJob().getTerminate()) {
			// Destroy VM if the user cancel the job.
			if (destroyResource) {
				logger.trace("Job cancelled: Destroying resource with Id: " + Id);
				destroyResource(Id);
			}
			getJob().setStatus("CANCELLED");
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	// Maximum waiting time is timeout
	protected void waitForResource(String addr) {
		ProcessBuilder pb = new ProcessBuilder("ssh", addr, "echo");

		// TODO: add timeout and polling interval in config file
		int timeout = this.im.getTimeout();
		int pollingInterval = this.im.getPollingInterval();
		int maxRetries = timeout / pollingInterval;
		int tries = 0;

		logger.trace("Trying to SSH to resource...");

		try {
			while (tries < maxRetries) {
				Process p = pb.start();

				// ignore timeout (connectivity errors are handled later)
				// TODO: set timeout in cfg file
				if (!p.waitFor(timeout, TimeUnit.SECONDS)) {
					logger.warn("SSH timeout after " + timeout + " seconds.");
					return;
				}
				if (p.exitValue() == 0) {
					logger.trace("SSH established.");
					return;
				}
				logger.trace("Connection refused. Waiting " + pollingInterval + " seconds.");
				tries++;
				Thread.sleep(pollingInterval * 1000);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		logger.warn("Could not establish SSH connection in " + timeout + " seconds.");
	}
}
