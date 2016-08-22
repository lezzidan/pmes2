package es.bsc.pmes.types;

import java.util.HashMap;

/**
 * Created by scorella on 8/5/16.
 */
public class User {
    private String username;
    private HashMap<String, String> credentials;

    public User(String name){
        this.username = name;
        this.credentials = new HashMap<>();
    }

    /** GETTERS AND SETTERS*/
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public HashMap<String, String> getCredentials() {
        return credentials;
    }

    public void setCredentials(HashMap<String, String> credentials) {
        this.credentials = credentials;
    }
}
