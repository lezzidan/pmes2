package es.bsc.pmes.managers.execution;

/**
 * Created by scorella on 9/8/16.
 */
public interface ExecutionThread {
    public void run();

    public void cancel() throws Exception;

    public void start();
}
