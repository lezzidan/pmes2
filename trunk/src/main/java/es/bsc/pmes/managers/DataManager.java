package es.bsc.pmes.managers;

/**
 * Created by scorella on 8/5/16.
 * Singleton Class
 */
public class DataManager {
    private static DataManager dataManager = new DataManager();
    private String protocol;

    private DataManager() {
        // TODO
    }

    public static DataManager getDataManager(){
        return dataManager;
    }

    public void stageIn(){
        // TODO
    }

    public void stageOut(){
        // TODO
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
