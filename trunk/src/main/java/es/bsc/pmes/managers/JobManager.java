package es.bsc.pmes.managers;

import es.bsc.pmes.types.Job;

import java.util.HashMap;
import java.util.Queue;

/**
 * Created by bscuser on 8/5/16.
 */
public class JobManager {
    private HashMap<String, Job> jobs;
    private Queue<Job> pendingJobs;
    private InfraestructureManager im;
    private DataManager dm;

    public JobManager(InfraestructureManager im){
        this.im = im;
    }
}
