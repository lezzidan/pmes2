package es.bsc.pmes.managers;

/**
 * DATA MANAGER Class.
 *
 * This class is aimed at managing the data within the infrastructure.
 *
 * - Singleton class. - Currently this class is not being used since the stageIn
 * and stageOut are done within the Infrastructure manager directly
 *
 * @author scorella on 8/5/16.
 */
public class DataManager {

    /* Main attributes */
    private static DataManager dataManager = new DataManager();
    private String protocol;

    /**
     * Default constructor
     */
    private DataManager() {
        // TODO
    }

    /**
     * ************************************************************************
     * GETTERS AND SETTERS.
     * ************************************************************************
     */
    /**
     * Data manager getter
     *
     * @return the static data manager instance
     */
    public static DataManager getDataManager() {
        return dataManager;
    }

    /**
     * Data manager setter
     *
     * @param dm the static data manager instance
     */
    public void getDataManager(DataManager dm) {
        DataManager.dataManager = dm;
    }

    /**
     * Data manager protocol getter
     *
     * @return the data manager protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Data manager protocol setter
     *
     * @param protocol the data manager protocol to be set
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * ***********************************************************************
     * DATA MANAGER FUNCTIONS.
     * ***********************************************************************
     */
    /**
     * Stage In
     */
    public void stageIn() {
        // TODO
    }

    /**
     * Stage Out
     */
    public void stageOut() {
        // TODO
    }
}
