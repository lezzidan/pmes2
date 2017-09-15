package es.bsc.pmes.service;

import es.bsc.pmes.managers.InfrastructureManager;
import es.bsc.pmes.managers.JobManager;
import es.bsc.pmes.types.*;

import java.util.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * PMES SERVICE Class
 *
 * Main PMES service class
 *
 * @author scorella on 8/5/16.
 */
public class PMESservice {

    /* Main attributes */
    private JobManager jm;
    private InfrastructureManager im;

    /* Logger */
    private static final Logger logger = LogManager.getLogger(PMESservice.class);

    /**
     * Default constructor
     */
    public PMESservice() {
        this.startService();
    }

    /**
     * Start the PMES service: Initializes the infrastructure and job managers
     */
    public void startService() {
        logger.trace("Starting PMESService");
        this.im = InfrastructureManager.getInfrastructureManager();
        this.jm = JobManager.getJobManager();
    }

    /**
     * Finishes the PMES service
     */
    public void endService() {
        // Any cleanup?
        logger.trace("Finishing PMESService");
    }

    /**
     * ***********************************************************************
     * PMES BACKEND API
     * ***********************************************************************
     */
    /**
     * CREATE ACTIVITY.
     *
     * This method is used to submit a job/s request to PMES. Requires a set of
     * JobDefinitions.
     *
     * @param jobDefinitions Job definitions of the requested jobs
     * @return A list of the corresponding job ids.
     */
    public ArrayList<String> createActivity(ArrayList<JobDefinition> jobDefinitions) {
        ArrayList<String> jobIds = new ArrayList<>(jobDefinitions.size());
        for (JobDefinition jobDef : jobDefinitions) {
            Job newJob;
            logger.trace("Job Type: " + jobDef.getApp().getType());
            if (jobDef.getApp().getType().equals("COMPSs")) {
                newJob = new COMPSsJob();
            } else {
                newJob = new SingleJob();
            }
            // Create new job
            newJob.setUser(jobDef.getUser());
            newJob.setJobDef(jobDef);
            newJob.setDataIn(jobDef.getInputPaths());
            newJob.setDataOut(jobDef.getOutputPaths());
            logger.trace("JobDef: " + jobDef.getInputPaths().toString() + " " + jobDef.getOutputPaths().toString());
            logger.trace("newJob: " + newJob.getDataIn().toString() + " " + newJob.getDataOut().toString());
            logger.trace("user: " + newJob.getUser().getCredentials().toString());
            jobIds.add(newJob.getId());

            String type = newJob instanceof COMPSsJob ? "COMPSs" : "Single";
            logger.trace("New " + type + " Job created with id " + newJob.getId());
            this.jm.enqueueJob(newJob);
        }
        return jobIds;
    }

    /**
     * TERMINATE ACTIVITY.
     *
     * Stops/Cancels the requested activities of the given job ids.
     *
     * @param jobIds Job ids to be terminated
     * @return A list with the messages of each job termination
     */
    public ArrayList<String> terminateActivity(ArrayList<String> jobIds) {
        ArrayList<String> messages = new ArrayList<>(jobIds.size());
        for (String id : jobIds) {
            String message = "";
            Job job = this.jm.getJobs().get(id);
            if (job != null) {
                job.setTerminate(Boolean.TRUE);

                if (job.getStatus().equals("FAILED")) {
                    message += "Job with id "
                            + id
                            + " cannot be cancelled, the job has been finished in Failed state.";
                } else if (job.getStatus().equals("FINISHED")) {
                    message += "Job with id "
                            + id
                            + " cannot be cancelled, the job has been finished.";
                } else {
                    this.jm.deleteJob(job);
                    message += "Job with id "
                            + id
                            + " will be cancelled.";
                }
            } else {
                message += "Job not found";
            }
            messages.add(message);
        }
        return messages;
    }

    /**
     * GET ACTIVITY STATUS.
     *
     * Retrieve the status of a set of jobs from their job ids.
     *
     * @param jobids Job ids to consult
     * @return List of JobStatus corresponding to each job id
     */
    public ArrayList<JobStatus> getActivityStatus(ArrayList<String> jobids) {
        ArrayList<JobStatus> status = new ArrayList<>(jobids.size());
        for (String id : jobids) {
            logger.trace("Asking status for job: " + id);
            Job job = this.jm.getJobs().get(id);
            if (job != null) {
                logger.trace("Job Found");
                logger.trace(job.getStatus());
                status.add(JobStatus.valueOf(job.getStatus()));
            } else {
                logger.trace("Job not found");
                status.add(JobStatus.valueOf("FAILED"));
            }
        }
        logger.trace("Sending list status " + status.toString());
        return status;
    }

    /**
     * GET ACTIVITY REPORT.
     *
     * Retrieve the report of a set of jobs from their job ids.
     *
     * @param jobids Job ids to consult
     * @return List of JobReport corresponding to each job id
     */
    public ArrayList<JobReport> getActivityReport(ArrayList<String> jobids) {
        ArrayList<JobReport> reports = new ArrayList<>(jobids.size());
        for (String id : jobids) {
            logger.trace("Asking Activity for job: " + id);
            Job job = this.jm.getJobs().get(id);
            if (job != null) {
                logger.trace("Job Found");
                JobReport jr = job.getReport();
                reports.add(jr);
            } else {
                logger.trace("Job not found");
                reports.add(new JobReport());
            }
        }
        return reports;
    }

    /**
     * GET SYSTEM STATUS.
     *
     * Retrieve the status of the whole system where PMES is deployed
     *
     * @return The system status
     */
    public SystemStatus getSystemStatus() {
        logger.trace(im.getSystemStatus().print());
        return im.getSystemStatus();
    }

    /**
     *************************************************************************
     * GETTERS AND SETTERS.
     * ************************************************************************
     */
    /**
     * Job manager getter
     *
     * @return the static job manager instance
     */
    public JobManager getJm() {
        return jm;
    }

    /**
     * Job manager setter
     *
     * @param jm the static job manager instance
     */
    public void setJm(JobManager jm) {
        this.jm = jm;
    }

    /**
     * Infrastructure manager getter
     *
     * @return the static infrastructure manager instance
     */
    public InfrastructureManager getIm() {
        return im;
    }

    /**
     * Infrastructure manager setter
     *
     * @param im the static infrastructure manager instance
     */
    public void setIm(InfrastructureManager im) {
        this.im = im;
    }

}
