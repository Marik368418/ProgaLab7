package server.supervisor;

import model.SpaceMarine;
import org.apache.commons.codec.digest.DigestUtils;
import server.util.CollectionWrapper;
import server.commands.*;
import server.util.DatabaseProcessor;
import transfers.AuthenticationData;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Supervisor implements Supervising {
    private static Scanner scanner;
    private static List<Path> stack = new LinkedList<>();
    private final CollectionWrapper collectionWrapper;
    private final HashMap<String, Command> commandMap;
    private boolean active;
    private DatabaseProcessor dbProcessor;

    public Supervisor(DatabaseProcessor dbProcessor, String dbCredentialsName) throws Exception {
        System.out.println("Подключение к базе данных... ");
        this.dbProcessor = dbProcessor;
        connectToDatabase(dbProcessor, dbCredentialsName);
        this.collectionWrapper = new CollectionWrapper(dbProcessor);

        scanner = new Scanner(System.in);
        this.commandMap = new HashMap<>();
        commandMap.put("add", new AddCommand(this));
        commandMap.put("add_if_max", new AddIfMaxCommand(this));
        commandMap.put("add_if_min", new AddIfMinCommand(this));
        commandMap.put("average_of_heart_count", new AverageOfHeartCountCommand(this));
        commandMap.put("clear", new ClearCommand(this));
        commandMap.put("execute_script", new ExecuteScriptCommand(this));
        commandMap.put("exit", new ExitCommand(this));
        commandMap.put("group_counting_by_name", new GroupCountingByNameCommand(this));
        commandMap.put("help", new HelpCommand(this));
        commandMap.put("info", new InfoCommand(this));
        commandMap.put("min_by_id", new MinByIdCommand(this));
        commandMap.put("remove_by_id", new RemoveByIdCommand(this));
        commandMap.put("remove_first", new RemoveFirstCommand(this));
        commandMap.put("show", new ShowCommand(this));
        commandMap.put("update", new UpdateCommand(this));

        commandMap.put("log_in", new LogInCommand(this));
        commandMap.put("sign_up", new SignUpCommand(this));
    }

    public synchronized Connection getDbConnection() {
        return collectionWrapper.getConnection();
    }

    public synchronized void connectToDatabase(DatabaseProcessor dbProcessor,
                                  String credentials) throws SQLException, IOException {
        dbProcessor.setConnection(credentials);
        dbProcessor.checkDatabase();
    }

    public synchronized CopyOnWriteArrayList<SpaceMarine> getCollection() throws SQLException {
        return collectionWrapper.getData();
    }

    public synchronized CollectionWrapper getDatabase() {
        return collectionWrapper;
    }

    public synchronized Command getCommandByName(String name) {
        return commandMap.get(name);
    }

    @Override
    public synchronized void stop() {
        this.active = false;
    }

    public HashMap<String, Command> getCommandMap() {
        return commandMap;
    }

    public boolean checkCredentials(AuthenticationData inputData) throws SQLException {
        inputData.setPassword(getSHA512Hash(inputData.getPassword()));

        AuthenticationData storedData = dbProcessor.getUserCredentials(inputData.getUsername());
        if (storedData == null) return false;
        return storedData.equals(inputData);
    }

    public boolean registerUser(AuthenticationData inputData) throws SQLException {
        return dbProcessor.addNewUser(inputData);
    }

    private String getSHA512Hash(String s) {
        return DigestUtils.sha512Hex(s);
    }
}