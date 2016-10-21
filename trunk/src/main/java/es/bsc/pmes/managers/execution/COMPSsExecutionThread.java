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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
    }

    public void cancel() throws Exception {
        //TODO
    }

    public void executeJob(){
        // Create Resource
        String Id = createResource();
        logger.trace("Resource created with Id: "+ Id);

        //StageIn
        logger.trace("Stage in");
        stageIn();

        //Configure execution
        String resourceAddress = job.getResource(0).getIp(); //get master IP
        String user = job.getUser().getUsername();
        String address = user+"@"+resourceAddress;
        String source = job.getJobDef().getApp().getSource();
        String target = job.getJobDef().getApp().getTarget();
        HashMap<String, String> args = job.getJobDef().getApp().getArgs();

        // Create command to execute
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("ssh");
        cmd.add(address);
        cmd.add(target+"/./"+source);
        for (Object o : args.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            cmd.add((String) pair.getValue());
        }
        String[] command = new String[cmd.size()];
        job.setCmd(cmd.toArray(command));
        logger.trace(Arrays.toString(command));

        //Wait until vm is ready at login stage
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Run job
        logger.trace("runnning");
        Integer exitValue = executeCommand(command);
        logger.trace("exit code"+ exitValue);
        if (exitValue > 0){
            job.setStatus("FAILED");
        }

        //StageOut
        logger.trace("Stage out");
        stageOut();

        //Destroy Resource
        logger.trace("Destroy resource");
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
        hd.setImageType(job.getJobDef().getImg().getImageType());
        hd.setImageName(job.getJobDef().getImg().getImageName());

        // Configure software
        SoftwareDescription sd = new SoftwareDescription();

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
        //job.setStatus("FINISHED");
        this.im.destroyResource(Id);
    }

    public Integer executeCommand(String[] cmd){
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec(cmd);

            BufferedReader in = new BufferedReader(new
                    InputStreamReader(process.getInputStream()));

            BufferedReader err= new BufferedReader(new
                    InputStreamReader(process.getErrorStream()));

            // Output log
            String outStr = "";
            String line = null;
            while ((line = in.readLine()) != null) {
                outStr += line;
            }
            in.close();
            logger.trace("out: " + outStr);

            //Error log
            line = null;
            String errStr = "";
            while ((line = err.readLine()) != null) {
                errStr += line;
            }
            err.close();
            logger.trace("err: " + errStr);

            process.waitFor();
            Integer exitValue = process.exitValue();
            return exitValue;
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 1;
    }
}
