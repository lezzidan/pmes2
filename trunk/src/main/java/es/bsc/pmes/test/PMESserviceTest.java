package es.bsc.pmes.test;

import es.bsc.pmes.service.PMESservice;
import es.bsc.pmes.types.App;
import es.bsc.pmes.types.Image;
import es.bsc.pmes.types.JobDefinition;
import es.bsc.pmes.types.User;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by scorella on 8/22/16.
 */
public class PMESserviceTest {
    public static PMESservice pmesService = new PMESservice();

    public static void testCreateActivity(){
        // java -classpath /home/user/compss-connectors-commons.jar:/home/user/compss-connectors-rocci.jar:/home/user/pmes.jar es.bsc.pmes.test.PMESserviceTest
        ArrayList<JobDefinition> jobDefinitions = new ArrayList<>();

        App app = new App("sleep");
        Image img = new Image("uuid_pmestestingocci_68", "small");
        User usr = new User("scorella");
        HashMap<String, String> credentials = new HashMap<>();
        credentials.put("key", "~/certs/test/scorella_test.key");
        credentials.put("pem", "~/certs/test/scorella_test.pem");
        usr.setCredentials(credentials);

        JobDefinition job = new JobDefinition();
        job.setApp(app);
        job.setJobName("test");
        job.setImg(img);
        job.setUser(usr);
        job.setInputPath("");
        job.setOutputPath("");
        job.setWallTime(10);
        job.setNumNodes(1);
        job.setCores(16);
        job.setMemory(1);
        job.setCompss_flags(null);

        jobDefinitions.add(job);
        pmesService.createActivity(jobDefinitions);
    }

    public static void main(String[] args){
        System.out.println("Testing app");

        testCreateActivity();


    }

}
