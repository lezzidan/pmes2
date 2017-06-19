package es.bsc.pmes.managers;

import es.bsc.pmes.managers.execution.COMPSsExecutionThread;
import es.bsc.pmes.managers.execution.ExecutionThread;
import es.bsc.pmes.managers.execution.SingleExecutionThread;
import es.bsc.pmes.types.COMPSsJob;
import es.bsc.pmes.types.Job;
import es.bsc.pmes.types.SingleJob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * SCHEDULER THREAD Class.
 *
 * This class contains the PMES scheduler.
 *
 * - Singleton class. - Always alive Thread
 *
 * @author scorella on 8/5/16.
 */
public class SchedulerThread extends Thread {

    /* Main attributes*/
    private static SchedulerThread scheduler = new SchedulerThread();
    private LinkedList<Job> pendingJobs;
    private Boolean stop = Boolean.FALSE;
    private InfrastructureManager im = InfrastructureManager.getInfrastructureManager();

    /* Execution threads hashmap */
    private HashMap<String, ExecutionThread> tasks = new HashMap<>();

    /* Logger */
    private static final Logger logger = LogManager.getLogger(SchedulerThread.class);

    /**
     * Default constructor
     */
    private SchedulerThread() {
        this.pendingJobs = new LinkedList<>();
    }

    /**
     * Scheduler getter. Retrieves the static scheduler instance
     *
     * @return the static scheduler instance
     */
    public static SchedulerThread getScheduler() {
        return scheduler;
    }

    /**
     * Scheduler thread run function. This function contains the scheduler loop
     * that iterates during the entire PMES alive status. Checks the tasks
     * hashmap every 5 seconds in order to perform the next action.
     */
    public void run() {
        while (!this.stop) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!pendingJobs.isEmpty()) {
                Job nextJob = this.nextJob();
                if (nextJob.getTerminate()) {
                    logger.trace("Job cancelled: Job Stop before execution");
                    nextJob.setStatus("CANCELLED");
                } else {
                    nextJob.setStatus("RUNNING");
                    this.executeJob(nextJob);
                }
            }
        }
    }

    /**
     * Add a new job to the scheduler
     *
     * @param job Job to be added
     */
    public void addJob(Job job) {
        this.pendingJobs.add(job);
    }

    /**
     * Retrieve the next pending job
     *
     * @return the next job
     */
    public Job nextJob() {
        return this.pendingJobs.poll();
    }

    /**
     * Execute job.
     *
     * Instantiates a new execution thread, adds it to the tasks hashmap and
     * starts its execution
     *
     * @param job The job to be executed
     */
    public void executeJob(Job job) {
        //Create execution dir
        job.createExecutionDir();

        //Run job
        ExecutionThread executor = null;
        if (job instanceof COMPSsJob) {
            logger.trace("COMPSs Job");
            executor = new COMPSsExecutionThread((COMPSsJob) job);
            logger.trace("Executor created");
        } else {
            logger.trace("Single Job");
            executor = new SingleExecutionThread((SingleJob) job);
        }
        this.tasks.put(job.getId(), executor);
        logger.trace("Executor Start");
        executor.start();
    }

    /**
     * Stop job.
     *
     * Retrieves the id from the job to be stopped and cancels it
     *
     * @param job Job to be stopped
     */
    public void stopJob(Job job) {
        ExecutionThread executionThread = this.tasks.get(job.getId());
        if (executionThread != null) {
            try {
                executionThread.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Delete job.
     *
     * Checks if the job is pending, and in such case removes it from the
     * pending jobs list. If the job is runnning, stops it.
     *
     * @param job
     */
    public void deleteJob(Job job) {
        Boolean removed = this.pendingJobs.remove(job);
        if (removed) {
            logger.trace("Job cancelled: Job removed from the scheduler");
        } else {
            this.stopJob(job);
        }
    }

    /**
     * ************************************************************************
     * GETTERS AND SETTERS.
     * ************************************************************************
     */
    /**
     * Pending jobs getter
     *
     * @return The list of pending jobs
     */
    public LinkedList<Job> getPendingJobs() {
        return pendingJobs;
    }

    /**
     * Pending jobs setter
     *
     * @param pendingJobs The pending jobs list to be set
     */
    public void setPendingJobs(LinkedList<Job> pendingJobs) {
        this.pendingJobs = pendingJobs;
    }

    /**
     * Scheduler stopper boolean getter
     *
     * @return The value of the stop attribute (is it running?)
     */
    public Boolean getStop() {
        return stop;
    }

    /**
     * Scheduler stopper boolean setter
     *
     * @param stop Boolean value to be set (do you want to stop the scheduler?)
     */
    public void setStop(Boolean stop) {
        this.stop = stop;
    }
}
