package es.bsc.pmes.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by scorella on 8/5/16.
 */
public class Job {
	private String id;
	private JobStatus status;
	private ArrayList<Resource> resources;
	private User user;
	private ArrayList<String> dataIn;
	private ArrayList<String> dataOut;
	private String[] cmd;
	private JobDefinition jobDef;
	private Boolean terminate;
	private JobReport report;
	private String dirPath;

	private static final Logger logger = LogManager.getLogger(Job.class.getName());

	public Job() {
		this.id = UUID.randomUUID().toString();
		this.status = JobStatus.PENDING;
		this.resources = new ArrayList<>();
		this.user = null;
		this.dataIn = new ArrayList<>();
		this.dataOut = new ArrayList<>();
		this.cmd = new String[] {};
		this.jobDef = null;
		this.terminate = Boolean.FALSE;
		this.report = new JobReport();
	}

	/** GETTERS AND SETTERS */
	public String getId() {
		return id;
	}

	public String getStatus() {
		return status.toString();
	}

	public void setStatus(String status) {
		this.status = JobStatus.valueOf(status);
		this.report.setJobStatus(this.status);
	}

	public Resource getResource(Integer idx) {
		return resources.get(idx);
	}

	public void addResource(Resource resource) {
		this.resources.add(resource);
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public ArrayList<String> getDataIn() {
		return dataIn;
	}

	public void setDataIn(ArrayList<String> dataIn) {
		this.dataIn = dataIn;
	}

	public ArrayList<String> getDataOut() {
		return dataOut;
	}

	public void setDataOut(ArrayList<String> dataOut) {
		this.dataOut = dataOut;
	}

	public String[] getCmd() {
		return cmd;
	}

	public void setCmd(String[] cmd) {
		this.cmd = cmd;
	}

	public void setJobDef(JobDefinition jobDef) {
		this.jobDef = jobDef;
		this.report.setJobDefinition(jobDef);
	}

	public JobDefinition getJobDef() {
		return jobDef;
	}

	public Boolean getTerminate() {
		return terminate;
	}

	public void setTerminate(Boolean terminate) {
		this.terminate = terminate;
	}

	public JobReport getReport() {
		return report;
	}

	public void setReport(JobReport report) {
		this.report = report;
	}

	public String getDirPath() {
		return dirPath;
	}

	public void setDirPath(String dirPath) {
		this.dirPath = dirPath;
	}

	public void createReport() {
		try (PrintWriter writer = new PrintWriter(this.dirPath + "/report.txt", "UTF-8")) {
			writer.println("Job Report");
			writer.println("---- Infrastructure ----");
			writer.println("Infrastructure: " + this.jobDef.getInfrastructure());
			writer.println("---- Job Definition ----");
			writer.println("Job Name: " + this.jobDef.getJobName());
			writer.println("App: " + this.jobDef.getApp().getName());
			writer.println("- Target: " + this.jobDef.getApp().getTarget());
			writer.println("- Source: " + this.jobDef.getApp().getSource());
			writer.println("- Type: " + this.jobDef.getApp().getType());
			writer.println("Image: " + this.jobDef.getImg().getImageName());
			writer.println("User: " + this.jobDef.getUser().getUsername());
			writer.println("- UID: " + this.jobDef.getUser().getCredentials().get("uid"));
			writer.println("- GID: " + this.jobDef.getUser().getCredentials().get("gid"));
			ArrayList<MountPoint> mountPoints = this.jobDef.getMountPoints();
			for (int i = 0; i < mountPoints.size(); i++) {
				writer.println("Mount Point: " + i);
				MountPoint mp = mountPoints.get(i);
				String target = mp.getTarget();
				String device = mp.getDevice();
				String permissions = mp.getPermissions();
				writer.println("- " + target + " : " + device + " : " + permissions);
			}
			writer.println("Input Files: " + this.jobDef.getInputPaths().toString());
			writer.println("Output Files: " + this.jobDef.getOutputPaths().toString());
			writer.println("Wall Clock time: " + this.jobDef.getWallTime());
			writer.println("Number of Nodes: " + this.jobDef.getNumNodes());
			writer.println("Number of Cores: " + this.jobDef.getCores());
			writer.println("Memory: " + this.jobDef.getMemory());
			writer.println("Disk: " + this.jobDef.getDisk());
			writer.println("COMPSs Flags: " + this.jobDef.getCompss_flags().toString());
			writer.println("InitialVMs: " + this.jobDef.getInitialVMs());
			writer.println("Number of VMs: [" + this.jobDef.getMinimumVMs() + ", " + this.jobDef.getMaximumVMs() + "]");
			writer.println("---- Job Execution -----");
			writer.println("Job Status: " + String.valueOf(this.report.getJobStatus()));
			writer.println("Exit value: " + this.report.getExitValue());
			writer.println("Elapsed Time: " + this.report.getElapsedTime());
			writer.println("---- Job Out/Err -------");
			writer.println(this.report.getJobOutputMessage());
			// writer.println("ERR: "+this.report.getJobErrorMessage());
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public void createExecutionDir() {
		String path = "/home/pmes/pmes/jobs/" + this.getJobDef().getJobName();
		this.dirPath = path;
		File dir = new File(path);
		if (!dir.exists()) {
			boolean result = dir.mkdir();
			if (result) {
				logger.trace("Job execution directory created: " + path);
			}
		}
	}

	public Integer stage(Integer direction) {
		/*
		 * direction = 0 --> Stage In direction = 1 --> Stage out
		 */
		Runtime runtime = Runtime.getRuntime();
		String resourceAddress = this.resources.get(0).getIp(); // get master IP
		String user = this.user.getUsername();
		String address = user + "@" + resourceAddress;
		Integer exitValue = 0;
		ArrayList<String> dataToTransfer;
		if (direction == 1) {
			dataToTransfer = this.dataOut;
		} else {
			dataToTransfer = this.dataIn;
		}
		for (String path : dataToTransfer) {
			logger.trace("Staging file: " + path);
			logger.trace("direction: " + direction.toString());
			ArrayList<String> cmd = new ArrayList<>();
			cmd.add("scp");
			cmd.add("-r");
			if (direction == 0) {
				cmd.add(path);
				cmd.add(address + ":/home/" + this.user.getUsername() + "/");
			} else {
				cmd.add(address + ":" + path);
				cmd.add(this.dirPath);
			}

			logger.trace("Command: " + cmd);

			String[] command = new String[cmd.size()];
			try {
				Integer pExitValue = 0;
				Integer max_retries = 0;
				do {
					Process process = runtime.exec(cmd.toArray(command));

					BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

					BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));

					// Output log
					String outStr = "";
					String line = null;
					while ((line = in.readLine()) != null) {
						outStr += line;
					}
					in.close();
					logger.trace("outMessage: " + outStr);

					// Error log
					line = null;
					String errStr = "";
					while ((line = err.readLine()) != null) {
						errStr += line;
					}
					err.close();
					logger.trace("errMessage: " + errStr);

					process.waitFor();
					pExitValue = process.exitValue();
					logger.trace("exit Code: " + pExitValue);
					exitValue += pExitValue; // How many transfers have been failed
					max_retries += 1;
					if (pExitValue != 0) {
						try {
							Thread.sleep(10000);
						} catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
					}
				} while (pExitValue != 0 && max_retries < 3);

			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}

		}
		return exitValue;
	}
}
