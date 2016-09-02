package es.bsc.pmes.test

import es.bsc.pmes.types.Job

/**
 * Created by scorella on 9/2/16.
 */
class JobTest {
    public static void testGenerateProjects() {
        Job jobTest = new Job();
        jobTest.generateProjects();
    }

    public static void  testGenerateResources() {
        Job jobTest = new Job();
        jobTest.generateResources();
    }

    public static void main(String[] args){
        JobTest.testGenerateProjects();

        JobTest.testGenerateResources();
    }
}
