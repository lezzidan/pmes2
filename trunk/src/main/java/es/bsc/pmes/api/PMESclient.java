package es.bsc.pmes.api;

import es.bsc.pmes.service.PMESservice;
import es.bsc.pmes.types.JobDefinition;
import es.bsc.pmes.types.JobReport;
import es.bsc.pmes.types.JobStatus;
import es.bsc.pmes.types.SystemStatus;

import java.util.ArrayList;

import com.sun.net.httpserver.HttpServer;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import java.io.IOException;

import javax.ws.rs.*;

/**
 * PMES API
 */

@Path("/pmes")
public class PMESclient {
    private String address;
    public static PMESservice pmesService;
    private static HttpServer server;

    /**
     * Instantiates a new PMES client.
     * @param address
     */
    public PMESclient(String address){
        this.pmesService = new PMESservice();
        this.address = address;
        /*try {
            this.startService();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    /**
     * Instantiates a new PMES client.
     */
    public PMESclient(){
        this.pmesService = new PMESservice();
    }

    /**
     * Submits a job to the PMES service.
     * @param jobDefinitions
     * @return
     */
    //@POST
    //@Produces("text/plain")
    public ArrayList<String> createActivity(ArrayList<JobDefinition> jobDefinitions){
        ArrayList<String> jobIds = this.pmesService.createActivity(jobDefinitions);
        return jobIds;
    }

    /**
     * Retrieves the JobStatus object for a set of sumbitted jobs with a certain status
     * (all | pending | running | finished | cancelled | failed).
     * @param jobids
     * @return
     */

    //@GET
    //@Produces("text/plain")
    public ArrayList<JobStatus> getActivityStatus(ArrayList<String> jobids){
        ArrayList<JobStatus> jobStatuses = this.pmesService.getActivityStatus(jobids);
        return jobStatuses;
    }

    /**
     * Gets the activity documents of a set of jobs giving the: JSDLs, jobs status,
     * execution progress, elapsed time and error messages.
     * @param jobids
     * @return
     */

    //@GET
    //@Produces("text/plain")
    public ArrayList<JobReport> getActivityReport(ArrayList<String> jobids){
        ArrayList<JobReport> jobReports = this.pmesService.getActivityReport(jobids);
        return jobReports;
    }

    /**
     * Terminates a set of submitted jobs.
     * @param jobIds
     * @return
     */
    //@DELETE
    //@Produces("text/plain")
    public ArrayList<String> terminateActivity(ArrayList<String> jobIds){
        ArrayList<String> terminateMessages = this.pmesService.terminateActivity(jobIds);
        return terminateMessages;
    }

    /**
     * Provides information about the resources consumption of the system.
     * @return
     */
    //@GET
    //@Produces("text/plain")
    public SystemStatus getSystemStatus(){
        SystemStatus systemStatus = this.pmesService.getSystemStatus();
        return systemStatus;
    }

    /**
     * Start pmes service
     * @throws IOException
     */
    private void startService() throws IOException{
        server = HttpServerFactory.create(this.address);
        //server = HttpServerFactory.create("http://localhost:9998/");
        server.start();

        System.out.println("Server running");
        System.out.println("Visit: http://localhost:9998/pmes");
    }

    /**
     * Stop pmes Service
     */
    private void stopService(){
        //System.out.println("Hit return to stop...");
        //System.in.read();
        System.out.println("Stopping server");
        server.stop(0);
        System.out.println("Server stopped");
    }

    // Test purposes
    @GET
    @Path("/message")
    @Produces("text/plain")
    public static String getClichedMessage() {
        // Return some cliched textual content
        return "pmes message";
    }

}
