package es.bsc.pmes.types;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by scorella on 8/5/16.
 */

public class App {
    private String id;
    private String name;
    private HashMap<String, String> args;
    private String target;
    private String source;

    public App(String name){
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.args = new HashMap<>();
        this.target = "/home/user/apps/";
        this.source = "";
    }
    /** GETTERS AND SETTERS*/
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashMap<String, String> getArgs() {
        return args;
    }

    public void setArgs(HashMap<String, String> args) {
        this.args = args;
    }

    public String getTarget() { return target; }

    public void setTarget(String path) { this.target = path; }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
