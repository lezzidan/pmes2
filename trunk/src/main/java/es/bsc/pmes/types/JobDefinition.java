package es.bsc.pmes.types;

import java.util.HashMap;

/**
 * Created by bscuser on 8/5/16.
 */
public class JobDefinition {
    private String id;
    private String jobName;
    private App app;
    private Image img;
    private String inputPath;
    private String outputPath;
    private Integer wallTime;
    private Integer numNodes;
    private Integer cores;
    private Integer memory;
    private HashMap<String, String> compss_flags;
}
