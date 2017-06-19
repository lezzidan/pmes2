package es.bsc.pmes.managers.execution;

/**
 * EXECUTION THREAD Interface.
 *
 * This class contains the execution thread interface that the abstract
 * execution thread implements
 *
 * @author scorella on 9/8/16.
 */
public interface ExecutionThread {

    /* Run execution. */
    public void run();

    /* Cancel execution. @throws Exception */
    public void cancel() throws Exception;

    /* Start thread. */
    public void start();
}
