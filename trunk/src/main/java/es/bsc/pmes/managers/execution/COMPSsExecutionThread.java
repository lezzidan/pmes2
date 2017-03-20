package es.bsc.pmes.managers.execution;

import es.bsc.conn.exceptions.ConnectorException;
import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.pmes.managers.InfrastructureManager;
import es.bsc.pmes.types.COMPSsJob;
import es.bsc.pmes.types.Job;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created by scorella on 8/23/16.
 */
public class COMPSsExecutionThread extends AbstractExecutionThread{
    private COMPSsJob job;
    private static final Logger logger = LogManager.getLogger(COMPSsExecutionThread.class);

    public COMPSsExecutionThread(COMPSsJob job){
        this.job = job;
    }

    public void run() {
        executeJob();
    }

    @Override
    protected Job getJob() {
        return job;
    }

    @Override
    public void executeJob(){
        // Create Resource
        if (this.stopExecution("-1", Boolean.TRUE)){
            return;
        }
        String Id = createResource();
        logger.trace("Resource created with Id: "+ Id);

        //Configure execution
        if (this.stopExecution(Id, Boolean.TRUE)){
            return;
        }

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
        cmd.add("bash");
        cmd.add("-ic"); //interactive session IMPORTANT!
        String runcompss = "\"runcompss";
        //TODO: test compss flags
        for (Object o: job.getJobDef().getCompss_flags().entrySet()){
            Map.Entry pair = (Map.Entry) o;
            runcompss += (String) pair.getValue();
        }
        if (job.getJobDef().getApp().getSource().endsWith(".py")){
            runcompss += " --lang=python";
            runcompss += " --pythonpath="+target;
        } else {
            runcompss += " --classpath="+target;
        }
        runcompss += " "+target+"/"+source;
        for (Object o : args.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            runcompss += " "+(String) pair.getValue();
        }
        runcompss += "\"";
        cmd.add(runcompss);

        /*
        // Test redirect output to a file
        cmd.add(">>");
        cmd.add("result.out");
        */


        String[] command = new String[cmd.size()];
        job.setCmd(cmd.toArray(command));
        logger.trace(Arrays.toString(command));

        //Wait until vm is ready at login stage
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //StageIn
        if (this.stopExecution(Id, Boolean.TRUE)){
            return;
        }
        logger.trace("Stage in");
        stageIn();

        //Run job
        if (this.stopExecution(Id, Boolean.TRUE)){
            return;
        }

        logger.trace("runnning");
        long startTime = System.currentTimeMillis();
        Integer exitValue = executeCommand(command);
        long endTime = System.currentTimeMillis()-startTime;
        job.getReport().setElapsedTime(String.valueOf(endTime));

        logger.trace("Execution Time: "+String.valueOf(endTime));
        logger.trace("exit code"+ exitValue);
        if (exitValue > 0){
            if (exitValue == 143){
                job.setStatus("CANCELLED");
                // TODO: Si cancelan un job cuando ya se ha ejecutado, traemos datos? salida de compss
            } else {
                job.setStatus("FAILED");
            }
        } else {
            //StageOut
            logger.trace("Stage out");
            stageOut();
            job.setStatus("FINISHED");
        }

        //Destroy Resource
        logger.trace("Destroy resource");
        destroyResource(Id);

        //Create Report
        this.job.createReport();
    }





}
