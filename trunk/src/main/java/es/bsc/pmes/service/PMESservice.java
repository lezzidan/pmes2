package es.bsc.pmes.service;

import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.pmes.managers.InfrastructureManager;
import es.bsc.pmes.managers.JobManager;
import es.bsc.pmes.types.Job;
import es.bsc.pmes.types.JobDefinition;
import es.bsc.pmes.types.JobReport;
import es.bsc.pmes.types.JobStatus;

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
        // TODO: createActivity
        ArrayList<String> jobIds = new ArrayList<>(jobDefinitions.size());
        for (JobDefinition jobDef:jobDefinitions) {

            /** Create new job */
            logger.trace("Creating new Job");
            Job newJob = new Job();
            newJob.setUser(jobDef.getUser());
            jobIds.add(newJob.getId());

            /** configure Resource Petition*/
            logger.trace("Configuring Job " + newJob.getId());
            // Configuring Hardware
            HardwareDescription hd = new HardwareDescription();
            hd.setMemorySize(jobDef.getMemory());
            hd.setTotalComputingUnits(jobDef.getCores()*jobDef.getNumNodes());

            // Configure software
            SoftwareDescription sd = new SoftwareDescription();
            sd.setImageType(jobDef.getImg().getImageType());
            sd.setImageName(jobDef.getImg().getImageName());

            // Configure properties
            HashMap<String, String> prop = this.im.configureResource(jobDef);

            /** create resource */
            try {
                logger.trace("Creating new Resource");
                //String Id = this.im.createResource(hd, sd, prop);
                //newJob.setResource(this.im.getActiveResources().get(Id));

                /** run */
                logger.trace("enqueuing new job");
                this.jm.enqueueJob(newJob);

                /** loop getActivityStatus */

                /** destroy resource */
                logger.trace("Deleting Resource");
                newJob.setStatus("FINISHED");
                //this.im.destroyResource(Id);

            } catch (Exception e){
                logger.error("Error creating resource " + e);
                newJob.setStatus("FAILED");
            }

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

    /** GETTERS AND SETTERS*/
    public JobManager getJm() { return jm; }

    public void setJm(JobManager jm) { this.jm = jm; }

    public InfrastructureManager getIm() { return im; }

    public void setIm(InfrastructureManager im) { this.im = im; }

}
