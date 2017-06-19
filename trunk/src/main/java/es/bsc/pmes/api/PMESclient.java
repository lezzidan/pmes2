package es.bsc.pmes.api;

import es.bsc.pmes.service.PMESservice;
import es.bsc.pmes.types.*;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;

/**
 * PMES API
 *
 * @author scorella
 */
@Path("/pmes")
public class PMESclient {

    /* Main attributes */
    private String address;
    public static PMESservice pmesService;

    /* Logger */
    private static final Logger logger = LogManager.getLogger(PMESclient.class);

    /**
     * Instantiates a new PMES client.
     */
    public PMESclient() {
        pmesService = new PMESservice();
        this.address = "";
        logger.trace("New PMESclient created without address");
    }

    /**
     * Instantiates a new PMES client with a given address.
     *
     * @param address Address string
     */
    public PMESclient(String address) {
        pmesService = new PMESservice();
        this.address = address;
        logger.trace("New PMESclient created with address " + this.address);
    }

    /**
     * Start PMES service
     *
     * @param address PMES address where to be started
     * @return the address where PMES is started
     */
    @GET
    @Path("/startService/{id}")
    @Produces("text/plain")
    public String startService(@PathParam("id") String address) {
        logger.trace("Starting PMES client at " + this.address);
        PMESclient client = new PMESclient(address);
        return address;
    }

    /**
     * Stop PMES Service
     *
     * @return the address where PMES was running
     */
    @GET
    @Path("/stopService")
    @Produces("text/plain")
    public String stopService() {
        logger.trace("Stopping PMES client at " + this.address);
        // TODO: stop service - Currently done by stopping tomcat
        return this.address;
    }

    /**
     * Submits a job to the PMES service.
     *
     * @param jobDefinitions Jobs to be executed by PMES
     * @return list of jobIds
     */
    @POST
    @Path("/createActivity")
    @Consumes("application/json")
    @Produces("application/json")
    public ArrayList<String> createActivity(ArrayList<JobDefinition> jobDefinitions) {
        ArrayList<String> jobIds = pmesService.createActivity(jobDefinitions);
        logger.trace("Jobs created: " + jobIds.toString());
        return jobIds;
    }

    /**
     * Retrieves the JobStatus object for a set of sumbitted jobs with a certain
     * status (all | pending | running | finished | cancelled | failed).
     *
     * @param jobIds List of job ids to be consulted
     * @return The job status of the given job ids
     */
    @POST
    @Path("/getActivityStatus")
    @Consumes("application/json")
    @Produces("application/json")
    public ArrayList<JobStatus> getActivityStatus(ArrayList<String> jobIds) {
        ArrayList<JobStatus> jobStatuses = pmesService.getActivityStatus(jobIds);
        return jobStatuses;
    }

    /**
     * Gets the activity documents of a set of jobs giving the: JSDLs, jobs
     * status, execution progress, elapsed time and error messages.
     *
     * @param jobIds List of job ids to be consulted
     * @return The job report of the given job ids
     */
    @POST
    @Path("/getActivityReport")
    @Consumes("application/json")
    @Produces("application/json")
    public ArrayList<JobReport> getActivityReport(ArrayList<String> jobIds) {
        ArrayList<JobReport> jobReports = pmesService.getActivityReport(jobIds);
        return jobReports;
    }

    /**
     * Terminates a set of submitted jobs.
     *
     * @param jobIds List of job ids to be finished
     * @return The termination message of the given job ids
     */
    @POST
    @Path("/terminateActivity")
    @Consumes("application/json")
    @Produces("application/json")
    public ArrayList<String> terminateActivity(ArrayList<String> jobIds) {
        ArrayList<String> terminateMessages = pmesService.terminateActivity(jobIds);
        return terminateMessages;
    }

    /**
     * Provides information about the resources consumption of the system.
     *
     * @return The system status
     */
    @GET
    @Path("/getSystemStatus")
    @Produces("application/json")
    public SystemStatus getSystemStatus() {
        SystemStatus systemStatus = pmesService.getSystemStatus();
        return systemStatus;
    }

    // Test purposes
    /*@GET
    @Path("/createActivity/{id}")
    @Produces("text/plain")
    public static String getAttribute(@PathParam("id") String name) {
        // Return some cliched textual content
        //http://localhost:8081/trunk_war_exploded/pmes/message
        System.out.println(name);
        return name;
    }

    @GET
    @Path("/pending")
    @Produces("text/plain")
    public String pending(){
        logger.trace("Starting PMES client at "+this.address);
        Integer size = pmesService.getJm().getJobs().size();
        return size.toString();
    }*/
}
