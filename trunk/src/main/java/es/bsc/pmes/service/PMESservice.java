package es.bsc.pmes.service;

import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.pmes.managers.InfrastructureManager;
import es.bsc.pmes.managers.JobManager;
import es.bsc.pmes.types.*;

import java.util.ArrayList;
import java.util.HashMap;

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
            // Test purposes
            newJob.setCmd("touch testFile.txt");

            jobIds.add(newJob.getId());
            logger.trace("New Job created with id "+newJob.getId());

            this.jm.enqueueJob(newJob);
        }
        return jobIds;
    }

    public ArrayList<String> terminateActivity(ArrayList<String> jobIds) {
        // TODO: createActivity
        return null;
    }

    public ArrayList<JobStatus> getActivityStatus(ArrayList<String> jobids){
        // TODO: getActivityStatus
        return null;
    }

    public ArrayList<JobReport> getActivityReport(ArrayList<String> jobids){
        // TODO: getActivityReport
        return null;
    }

    public SystemStatus getSystemStatus(){
        // TODO: getSystemStatus
        return null;
    }

    /** GETTERS AND SETTERS*/
    public JobManager getJm() { return jm; }

    public void setJm(JobManager jm) { this.jm = jm; }

    public InfrastructureManager getIm() { return im; }

    public void setIm(InfrastructureManager im) { this.im = im; }

}
