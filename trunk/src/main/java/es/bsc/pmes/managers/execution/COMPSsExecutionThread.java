package es.bsc.pmes.managers.execution;

import es.bsc.conn.exceptions.ConnectorException;
import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.pmes.managers.InfrastructureManager;
import es.bsc.pmes.types.Job;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Created by scorella on 8/23/16.
 */
public class COMPSsExecutionThread extends Thread implements ExecutionThread{
    private Job job;
    private InfrastructureManager im = InfrastructureManager.getInfrastructureManager();
    private static final Logger logger = LogManager.getLogger(COMPSsExecutionThread.class);

    public COMPSsExecutionThread(Job job){
        this.job = job;
    }

    public void run() {
        executeJob();
        /*String resourceAddress = job.getResource(0).getIp(); //get master IP
        System.out.println(resourceAddress);

        String user = job.getUser().getUsername();
        String address = user+"@"+resourceAddress;

        String cmd = address + " " + job.getCmd();
        System.out.println(cmd);

        //Wait until vm is ready at login stage
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Run job - Execute
        job.setStatus("RUNNING");
        System.out.println("Executing...");
        Integer exitValue = executeCommand(cmd);

        //stageOut

        //Destroy Resource*/
    }

    public void cancel() throws Exception {
        //TODO
    }

    public void executeJob(){
        // Create Resource
        String Id = createResource();

        //StageIn
        stageIn();

        //Configure execution
        String resourceAddress = job.getResource(0).getIp(); //get master IP
        String user = job.getUser().getUsername();
        String address = user+"@"+resourceAddress;
        String cmd = address + " " + job.getCmd();

        //Wait until vm is ready at login stage
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Run job
        Integer exitValue = executeCommand(cmd);

        //StageOut
        stageOut();

        //Destroy Resource
        destroyResource(Id);
    }

    public String createResource(){
        // Create Resource
        // ** configure Resource Petition
        logger.trace("Configuring Job " + job.getId());
        // Configuring Hardware
        HardwareDescription hd = new HardwareDescription();
        hd.setMemorySize(job.getJobDef().getMemory());
        hd.setTotalComputingUnits(job.getJobDef().getCores()*job.getJobDef().getNumNodes());

        // Configure software
        SoftwareDescription sd = new SoftwareDescription();
        sd.setImageType(job.getJobDef().getImg().getImageType());
        sd.setImageName(job.getJobDef().getImg().getImageName());

        // Configure properties
        HashMap<String, String> prop = this.im.configureResource(job.getJobDef());

        //** create resource
        logger.trace("Creating new Resource");
        String Id = this.im.createResource(hd, sd, prop);
        logger.trace("Resource Id " + Id);
        job.addResource(this.im.getActiveResources().get(Id));
        return Id;
    }

    public void stageIn(){
        logger.trace("Staging in");
        //TODO: stageIN
    }

    public void stageOut(){
        logger.trace("Staging out");
        //TODO: stageOUT
    }

    public void destroyResource(String Id){
        logger.trace("Deleting Resource");
        job.setStatus("FINISHED");
        this.im.destroyResource(Id);
    }

    public Integer executeCommand(String cmd){
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec("ssh "+cmd);
            Integer exitValue = process.waitFor();

            // Output log
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(process.getInputStream()) );
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            in.close();

            //Error log
            BufferedReader err = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()) );
            while ((line = err.readLine()) != null) {
                System.out.println(line);
            }
            err.close();

            return exitValue;
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 1;
    }
}
