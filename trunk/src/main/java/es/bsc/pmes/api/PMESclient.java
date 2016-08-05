package es.bsc.pmes.api;

import es.bsc.pmes.types.JobDefinition;
import es.bsc.pmes.types.JobReport;
import es.bsc.pmes.types.JobStatus;
import es.bsc.pmes.types.SystemStatus;

import java.util.ArrayList;

/**
 * PMES API
 */

public class PMESclient {
    private String address;

    /**
     * Instantiates a new PMES client.
     * @param address
     */
    public PMESclient(String address){
        this.address = address;
    }

    /**
     * Submits a job to the PMES service.
     * @param jobDefinitions
     * @return
     */
    public ArrayList<String> createActivity(ArrayList<JobDefinition> jobDefinitions){
        return null;
    }

    /**
     * Retrieves the JobStatus object for a set of sumbitted jobs with a certain status
     * (all | pending | running | finished | cancelled | failed).
     * @param jobids
     * @return
     */
    private ArrayList<JobStatus> getActivityStatus(ArrayList<String> jobids){
        return null;
    }

    /**
     * Gets the activity documents of a set of jobs giving the: JSDLs, jobs status,
     * execution progress, elapsed time and error messages.
     * @param jobids
     * @return
     */
    private ArrayList<JobReport> getActivityReport(ArrayList<String> jobids){
        return null;
    }

    /**
     * Terminates a set of submitted jobs.
     * @param jobIds
     * @return
     */
    public ArrayList<String> terminationMessages(ArrayList<String> jobIds){
        return null;
    }

    /**
     * Provides information about the resources consumption of the system.
     * @return
     */
    public SystemStatus getSystemStatus(){
        return null;
    }
}
