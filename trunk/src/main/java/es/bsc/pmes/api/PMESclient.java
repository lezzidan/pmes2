package es.bsc.pmes.api;

import es.bsc.pmes.service.PMESservice;
import es.bsc.pmes.types.*;

import java.util.ArrayList;

import com.sun.net.httpserver.HttpServer;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * PMES API
 */

@Path("/pmes")
public class PMESclient {
    private String address;
    public static PMESservice pmesService;
    private static final Logger logger = LogManager.getLogger(PMESclient.class);

    /**
     * Instantiates a new PMES client.
     * @param address
     */
    public PMESclient(String address){
        this.pmesService = new PMESservice();
        this.address = address;
        logger.trace("New PMESclient created with address "+this.address);
    }

    /**
     * Instantiates a new PMES client.
     */
    public PMESclient(){
        this.pmesService = new PMESservice();
        this.address = "";
        logger.trace("New PMESclient created");
    }

    /**
     * Start pmes service
     */
    @GET
    @Path("/startService/{id}")
    @Produces("text/plain")
    public String startService(@PathParam("id")String address){
        logger.trace("Starting PMES client at "+this.address);
        PMESclient client = new PMESclient(address);
        return address;
    }

    /**
     * Stop pmes Service
     */
    @GET
    @Path("/stopService")
    @Produces("text/plain")
    public String stopService(){
        logger.trace("Stopping PMES client at "+this.address);
        // TODO
        return this.address;
    }

    /**
     * Submits a job to the PMES service.
     * @param jobDefinitions
     * @return list of jobIds
     */
    @POST
    @Path("/createActivity")
    @Consumes("application/json")
    @Produces("application/json")
    public ArrayList<String> createActivity(ArrayList<JobDefinition> jobDefinitions){
        ArrayList<String> jobIds = this.pmesService.createActivity(jobDefinitions);
        logger.trace("Jobs created: "+jobIds.toString());
        return jobIds;
    }

    /**
     * Retrieves the JobStatus object for a set of sumbitted jobs with a certain status
     * (all | pending | running | finished | cancelled | failed).
     * @param jobids
     * @return
     */

    @POST
    @Path("/getActivityStatus")
    @Consumes("application/json")
    @Produces("application/json")
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

    @POST
    @Path("/getActivityReport")
    @Consumes("application/json")
    @Produces("application/json")
    public ArrayList<JobReport> getActivityReport(ArrayList<String> jobids){
        ArrayList<JobReport> jobReports = this.pmesService.getActivityReport(jobids);
        return jobReports;
    }

    /**
     * Terminates a set of submitted jobs.
     * @param jobIds
     * @return
     */
    @POST
    @Path("/terminateActivity")
    @Consumes("application/json")
    @Produces("application/json")
    public ArrayList<String> terminateActivity(ArrayList<String> jobIds){
        ArrayList<String> terminateMessages = this.pmesService.terminateActivity(jobIds);
        return terminateMessages;
    }

    /**
     * Provides information about the resources consumption of the system.
     * @return
     */
    @GET
    @Path("/getSystemStatus")
    @Produces("application/json")
    public SystemStatus getSystemStatus(){
        SystemStatus systemStatus = this.pmesService.getSystemStatus();
        return systemStatus;
    }


    // Test purposes
    @GET
    @Path("/message")
    @Produces("text/plain")
    public static String getClichedMessage() {
        // Return some cliched textual content
        //http://localhost:8081/trunk_war_exploded/pmes/message
        return "pmes message";
    }

    // Test purposes
    @GET
    @Path("/createActivity/{id}")
    @Produces("text/plain")
    public static String getAttribute(@PathParam("id") String name) {
        // Return some cliched textual content
        //http://localhost:8081/trunk_war_exploded/pmes/message
        System.out.println(name);
        return name;
    }

    // Test purposes
    @GET
    @Path("/get")
    @Produces(MediaType.APPLICATION_JSON)
    public static MyObj getAttribute() {
        // Return some cliched textual content
        //http://localhost:8081/trunk_war_exploded/pmes/message

        MyObj obj = new MyObj("test");
        return obj;
    }

    @POST
    @Path("/post")
    @Consumes("text/plain")
    @Produces("text/plain")
    public static String setAttribute(String name) {
        // Return some cliched textual content
        //http://localhost:8081/trunk_war_exploded/pmes/message
        System.out.println(name);
        return name;
    }

    @POST
    @Path("/post2")
    @Consumes("application/json")
    @Produces("application/json")
    public static MyObj setAttributeJSON(MyObj obj) {
        // Return some cliched textual content
        //http://localhost:8081/trunk_war_exploded/pmes/message
        System.out.println(obj.name);
        obj.name += "ret";
        return obj;
    }

    @POST
    @Path("/testArray")
    @Consumes("application/json")
    @Produces("application/json")
    public static ArrayList<MyObj> setAttributeJSON(ArrayList<MyObj> objs) {
        // Return some cliched textual content
        //http://localhost:8081/trunk_war_exploded/pmes/message
        System.out.println(objs.get(0).name);
        objs.get(0).name += "ret";
        return objs;
    }

    @GET
    @Path("/pending")
    @Produces("text/plain")
    public String pending(){
        logger.trace("Starting PMES client at "+this.address);
        Integer size = this.pmesService.getJm().getJobs().size();
        return size.toString();
    }

}
