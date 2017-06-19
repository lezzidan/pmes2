package es.bsc.pmes.managers;

import es.bsc.pmes.types.Job;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

/**
 * JOB MANAGER Class.
 *
 * This class is aimed at managing the data within the infrastructure.
 *
 * - Singleton class.
 *
 * @author scorella on 8/5/16.
 */
public class JobManager {

    /* Main attributes */
    private static JobManager jobManager = new JobManager(InfrastructureManager.getInfrastructureManager());
    private InfrastructureManager im;
    private DataManager dm;
    private SchedulerThread scheduler;

    /* Jobs hashmap */
    private HashMap<String, Job> jobs;

    /* Logger */
    private static final Logger logger = LogManager.getLogger(InfrastructureManager.class.getName());

    /**
     * Constructor
     *
     * @param im Infrastructure manager
     */
    private JobManager(InfrastructureManager im) {
        this.jobs = new HashMap<>();
        this.im = im;
        this.dm = DataManager.getDataManager();
        this.scheduler = SchedulerThread.getScheduler();
        startScheduler();
    }

    /**
     * ************************************************************************
     * GETTERS AND SETTERS.
     * ************************************************************************
     */
    /**
     * Job manager getter
     *
     * @return the static job manager instance
     */
    public static JobManager getJobManager() {
        return jobManager;
    }

    /**
     * Job manager setter
     *
     * @param jm the static job manager instance
     */
    public void setJobManager(JobManager jm) {
        JobManager.jobManager = jm;
    }

    /**
     * Jobs getter
     *
     * @return the jobs hashmap
     */
    public HashMap<String, Job> getJobs() {
        return jobs;
    }

    /**
     * Jobs setter
     *
     * @param jobs the jobs hashmap to be set
     */
    public void setJobs(HashMap<String, Job> jobs) {
        this.jobs = jobs;
    }

    /**
     * Infrastructure manager getter
     *
     * @return the infrastructure manager instance
     */
    public InfrastructureManager getIm() {
        return im;
    }

    /**
     * Infrastructure manager setter
     *
     * @param im the infrastructure manager instance to be set
     */
    public void setIm(InfrastructureManager im) {
        this.im = im;
    }

    /**
     * Data manager getter
     *
     * @return the data manager instance
     */
    public DataManager getDm() {
        return dm;
    }

    /**
     * Data manager setter
     *
     * @param dm the data manager instance to be set
     */
    public void setDm(DataManager dm) {
        this.dm = dm;
    }

    /**
     * ***********************************************************************
     * JOB MANAGER FUNCTIONS.
     * ***********************************************************************
     */
    /**
     * PMES Scheduler starter
     */
    public void startScheduler() {
        this.scheduler.start();
    }

    /**
     * PMES Scheduler stopper
     */
    public void endScheduler() {
        this.scheduler.setStop(Boolean.TRUE);
        // TODO: Set all pending jobs as "CANCELLLED"
    }
    
    /**
     * Enqueue a job in PMES scheduler.
     * @param newJob the job to enqueue
     */
    public void enqueueJob(Job newJob) {
        this.jobs.put(newJob.getId(), newJob);
        newJob.setStatus("PENDING");
        this.scheduler.addJob(newJob);
    }

    /**
     * Delete a job from the PMES scheduler.
     * Checks if the job is running or pending befor deleting.
     * @param job the job to be removed from the scheduler
     */
    public void deleteJob(Job job) {
        if (job.getStatus().equals("PENDING")) {
            this.scheduler.deleteJob(job);
        } else {
            this.scheduler.stopJob(job);
        }
    }
}
