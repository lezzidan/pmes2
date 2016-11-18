package es.bsc.pmes.service;

import es.bsc.pmes.managers.InfrastructureManager;
import es.bsc.pmes.managers.JobManager;
import es.bsc.pmes.types.*;

import java.util.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Created by scorella on 8/5/16.
 */

public class PMESservice {
    private JobManager jm;
    private InfrastructureManager im;

    private static final Logger logger = LogManager.getLogger(PMESservice.class);

    public PMESservice() {
        this.startService();
    }

    public void startService(){
        logger.trace("Starting PMESService");
        this.im = InfrastructureManager.getInfrastructureManager();
        this.jm = JobManager.getJobManager();

    }

    public void endService(){
        // TODO: endService
    }

    public ArrayList<String> createActivity(ArrayList<JobDefinition> jobDefinitions) {
        ArrayList<String> jobIds = new ArrayList<>(jobDefinitions.size());
        for (JobDefinition jobDef:jobDefinitions) {
            //** Create new job
            Job newJob = new Job();
            newJob.setUser(jobDef.getUser());
            newJob.setJobDef(jobDef);

            jobIds.add(newJob.getId());
            logger.trace("New Job created with id "+newJob.getId());

            this.jm.enqueueJob(newJob);
        }
        return jobIds;
    }

    public ArrayList<String> terminateActivity(ArrayList<String> jobIds) {
        // TODO: terminateActivity
        ArrayList<String> messages = new ArrayList<>(jobIds.size());
        for (String id:jobIds) {
            logger.trace("Setting status cancelled for job: "+id);
            Job job = this.jm.getJobs().get(id);
            job.setStatus("cancelled");
        }
        return null;
    }

    public ArrayList<JobStatus> getActivityStatus(ArrayList<String> jobids){
        ArrayList<JobStatus> status = new ArrayList<JobStatus>(jobids.size());
        for (String id:jobids) {
            logger.trace("Asking status for job: "+id);
            Job job = this.jm.getJobs().get(id);
            if (job != null) {
                logger.trace("Job Found");
                logger.trace(job.getStatus());
                status.add(JobStatus.valueOf(job.getStatus()));
            }
            else {
                logger.trace("Job not found");
                status.add(JobStatus.valueOf("ALL"));
            }
        }
        logger.trace("Sending list status "+status.toString());
        return status;
    }

    public ArrayList<JobReport> getActivityReport(ArrayList<String> jobids){
        // TODO: getActivityReport
        return null;
    }

    public SystemStatus getSystemStatus(){
        return im.getSystemStatus();
    }

    /** GETTERS AND SETTERS*/
    public JobManager getJm() { return jm; }

    public void setJm(JobManager jm) { this.jm = jm; }

    public InfrastructureManager getIm() { return im; }

    public void setIm(InfrastructureManager im) { this.im = im; }

}
