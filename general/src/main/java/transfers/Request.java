package transfers;

import model.SpaceMarine;

import java.io.Serializable;

public class Request implements Serializable {
    private AuthenticationData authenticationData;

    private String commandName;
    private String args = "";
    private SpaceMarine object = null;

    public Request(){

    }

    public Request(String commandName) {
        this.commandName = commandName.trim();
    }

    public Request(String commandName, AuthenticationData data) {
        this.commandName = commandName.trim();
        this.authenticationData = data;
    }

    public Request(String commandName, String args) {
        this.commandName = commandName.trim();
        this.args = args;
    }

    public Request(String commandName, SpaceMarine object) {
        this.commandName = commandName.trim();
        this.object = object;
    }

    public Request(String commandName, String args, SpaceMarine object) {
        this.commandName = commandName.trim();
        this.args = args.trim();
        this.object = object;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getArgs() {
        return args;
    }

    public SpaceMarine getObject() {
        return object;
    }

    public AuthenticationData getAuthenticationData() {
        return authenticationData;
    }

    public void setAuthenticationData(AuthenticationData authenticationData) {
        this.authenticationData = authenticationData;
    }

    public boolean isEmpty() {
        if(commandName == null || args == null){
            return true;
        }
        else {
            return commandName.isEmpty() && args.isEmpty() && object == null;
        }
    }

    @Override
    public String toString() {
        return "Request: " + "\n" +
                "commandName = " + commandName + "\n" +
                "args = " + (args.isEmpty()
                ? ""
                : args) + "\n" +
                "object = " + ((object == null)
                ? ""
                : object) + "\n";
    }
}