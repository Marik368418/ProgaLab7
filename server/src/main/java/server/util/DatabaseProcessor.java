package server.util;

import model.AstartesCategory;
import model.Chapter;
import model.Coordinates;
import model.SpaceMarine;
import org.apache.commons.codec.digest.DigestUtils;
import transfers.AuthenticationData;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

public class DatabaseProcessor {
    private Connection connection;
    private String url;
    private String schema;

    private String host;
    private int port;
    private String user;
    private String password;

    public synchronized void setConnection(String credentialsFileName) throws FileNotFoundException,
            SQLException {

        String dir = System.getProperty("user.dir");
        String filepath = dir + File.separator + credentialsFileName;

        File f = new File(filepath);

        FileReader fileReader = new FileReader(f);
        BufferedReader reader = new BufferedReader(fileReader);
        String property;
        try {
            while ((property = reader.readLine()) != null) {
                String[] tmp = property.split("=");

                if (tmp[0].equals("psqlHost"))
                    host = tmp[1];
                else if (tmp[0].equals("psqlPort"))
                    port = Integer.parseInt(tmp[1]);
                else if (tmp[0].equals("psqlUser"))
                    user = tmp[1];
                else if (tmp[0].equals("psqlPass"))
                    password = tmp[1];
            }
        }
        catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            throw new SQLException();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        url = "jdbc:postgresql://" + host + ":" + port + "/";
        String dbName = "studs";
        url = String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);

        if (user == null || password == null) {
            System.out.println("Invalid database user or password.");
            throw new SQLException();
        }

        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);

        connection = DriverManager.getConnection(url, properties);
        schema = connection.getSchema();
    }

    public synchronized void checkDatabase() {
        try {
            System.out.println("Checking tables in database for existence.");
            if (!isCategoriesTableExist()) {
                createCategoriesTable();
                System.out.println("Table \"categories\" didn't exist. It was created.");
            }
            else {
                System.out.println("Table \"categories\" exists.");
            }
            if (!isUsersTableExist()) {
                System.out.println("Table \"users\" didn't exist. It was created.");
                createUsersTable();
            }
            else {
                System.out.println("Table \"users\" exists.");
            }
            if (!isCollectionTableExist()) {
                System.out.println("Table \"collection\" didn't exist. It was created.");
                createCollectionTable();
            }
            else {
                System.out.println("Table \"collection\" exists.");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized AuthenticationData getUserCredentials(String username) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT password_hash FROM users " +
                        " WHERE username = ?;");

        statement.setString(1, username);
        ResultSet set = statement.executeQuery();

        if (!set.isBeforeFirst()) return null;
        set.next();

        String password = set.getString(1);
        return new AuthenticationData(username, password);
    }

    public synchronized boolean addNewUser(AuthenticationData data) throws SQLException {
        String username = data.getUsername();
        PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM users " +
                        " WHERE username = ?;");
        statement.setString(1, username);
        ResultSet rs = statement.executeQuery();

        if (rs.isBeforeFirst()) return false;

        PreparedStatement createStatement = connection.prepareStatement(
                "INSERT INTO users (username, password_hash)" +
                        "VALUES (? ,?);"
        );
        createStatement.setString(1, data.getUsername());
        createStatement.setString(2, getSHA512Hash(data.getPassword()));

        createStatement.execute();
        return true;
    }

    private String getSHA512Hash(String s) {
        return DigestUtils.sha512Hex(s);
    }

    public synchronized boolean isCollectionTableExist() throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT FROM information_schema.tables" +
                        " WHERE table_schema = ? AND" +
                        " table_name = ?;");

        statement.setString(1, schema);
        statement.setString(2, "collection");
        ResultSet set = statement.executeQuery();

        return (set.isBeforeFirst());
    }

    public synchronized boolean isCategoriesTableExist() throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT FROM information_schema.tables" +
                        " WHERE table_schema = ? AND" +
                        " table_name = ?;");

        statement.setString(1, schema);
        statement.setString(2, "categories");
        ResultSet set = statement.executeQuery();

        return (set.isBeforeFirst());
    }

    public synchronized boolean isUsersTableExist() throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT FROM information_schema.tables" +
                        " WHERE table_schema = ? AND" +
                        " table_name = ?;");

        statement.setString(1, schema);
        statement.setString(2, "users");
        ResultSet set = statement.executeQuery();

        return (set.isBeforeFirst());
    }

    public synchronized void createCollectionTable() throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Collection ( " +
                        "id serial PRIMARY KEY, " +
                        "name text, " +
                        "coord_x bigint, " +
                        "coord_y bigint, " +
                        "creation_date timestamp, " +
                        "health int, " +
                        "heart_count int, " +
                        "achievements text, " +
                        "astartes_category int REFERENCES categories, " +
                        "chapter_name text," +
                        "marines_count bigint," +
                        "user_id int REFERENCES users);");
    }

    private synchronized int getCategoryKeyFromTable(String category) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT category_id FROM categories WHERE category_name = ?;");

        statement.setString(1, category);
        ResultSet rs = statement.executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    public synchronized void createCategoriesTable() throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS categories (" +
                        "category_id serial primary key, " +
                        "category_name text );");

        statement.execute(
                "INSERT INTO categories(category_name) " +
                        "values ('SCOUT'), ('ASSAULT'), ('SUPPRESSOR'), ('APOTHECARY');");
    }

    public synchronized void createUsersTable() throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate(
                "CREATE TABLE users ( " +
                        "id serial primary key, " +
                        "username text unique, " +
                        "password_hash text); "
        );
    }

    /**
     * @param username - name of the user who owns the node
     * @param marine - marine to insert
     * @return id of the inserted marine
     * **/
    public synchronized int insertMarine(String username, SpaceMarine marine) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO collection(name, coord_x, coord_y, creation_date, health, heart_count, achievements, " +
                        "astartes_category, chapter_name, marines_count, user_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",

                Statement.RETURN_GENERATED_KEYS);

        statement.setString(1, marine.getName());
        statement.setFloat(2, marine.getCoordinates().getX());
        statement.setDouble(3, marine.getCoordinates().getY());
        statement.setTimestamp(4, Timestamp.valueOf(marine.getCreationDate().toLocalDateTime()));
        statement.setInt(5, marine.getHealth());
        statement.setInt(6, marine.getHeartCount());
        statement.setString(7, marine.getAchievements());
        statement.setInt(8, getCategoryKeyFromTable(marine.getCategory().toString()));
        statement.setString(9, marine.getChapter().getName());
        statement.setLong(10, marine.getChapter().getMarinesCount());
        statement.setInt(11, getUserIdByUsername(username));

        statement.execute();
        ResultSet rs = statement.getGeneratedKeys();
        rs.next();

        return rs.getInt(1);
    }

    private int getUserIdByUsername(String username) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM users WHERE username = ?;"
        );
        statement.setString(1, username);
        ResultSet rs = statement.executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    private String getUserUsernameById(int id) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT username FROM users WHERE id = ?;"
        );
        statement.setInt(1, id);
        ResultSet rs = statement.executeQuery();
        rs.next();
        return rs.getString(1);
    }

    public synchronized void clear(String username) throws SQLException {
        int id = getUserIdByUsername(username);
        PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM collection WHERE user_id = ?;"
        );
        statement.setInt(1, id);
        statement.execute();
    }

    public synchronized void restartSequence() throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate("ALTER SEQUENCE collection_id_seq RESTART WITH 1;");
    }

    public synchronized void remove(int id) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("DELETE FROM collection " +
                "WHERE id = ?;");
        statement.setInt(1, id);
        statement.execute();
    }

    public synchronized void update(SpaceMarine marine) throws SQLException {
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE collection " +
                            "SET name = ?, " +
                            "coord_x = ?, " +
                            "coord_y = ?, " +
                            "creation_date = ?, " +
                            "health = ?, " +
                            "heart_count = ?, " +
                            "achievements = ?, " +
                            "astartes_category = ?, " +
                            "chapter_name = ?, " +
                            "marines_count = ? " +
                            "WHERE id = ?;"
            );

            statement.setString(1, marine.getName());
            statement.setFloat(2, marine.getCoordinates().getX());
            statement.setDouble(3, marine.getCoordinates().getY());
            statement.setTimestamp(4, Timestamp.valueOf(marine.getCreationDate().toLocalDateTime()));
            statement.setInt(5, marine.getHealth());
            statement.setInt(6, marine.getHeartCount());
            statement.setString(7, marine.getAchievements());
            statement.setInt(8, getCategoryKeyFromTable(marine.getCategory().toString()));
            statement.setString(9, marine.getChapter().getName());
            statement.setLong(10, marine.getChapter().getMarinesCount());
            statement.setInt(11, marine.getId());

            statement.execute();
    }

    public synchronized void checkUserRule(int id, String username) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT user_id FROM collection WHERE id = ?;"
        );

        statement.setInt(1, id);
        ResultSet rs = statement.executeQuery();

        rs.next();
    }

    public synchronized CopyOnWriteArrayList<SpaceMarine> getCollection() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet set = statement.executeQuery(
                "SELECT * FROM " +
                        "collection " +
                        "JOIN categories ON collection.astartes_category = categories.category_id;"
        );
        return getResultSetData(set);
    }

    private synchronized CopyOnWriteArrayList<SpaceMarine> getResultSetData(ResultSet rs) throws SQLException {
        CopyOnWriteArrayList<SpaceMarine> result = new CopyOnWriteArrayList<>();

        while (rs.next()) {
            SpaceMarine marine = getMarineFromRow(rs);
            result.add(marine);
        }
        return result;
    }

    private synchronized SpaceMarine getMarineFromRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        Float coord_x = rs.getFloat("coord_x");
        Double coord_y = rs.getDouble("coord_y");

        Timestamp creation_date_ts = rs.getTimestamp("creation_date");
        LocalDateTime localDateTime = creation_date_ts.toLocalDateTime();
        ZonedDateTime creation_date = localDateTime.atZone(ZoneId.systemDefault());

        int health = rs.getInt("health");
        int heart_count = rs.getInt("heart_count");
        String achievements = rs.getString("achievements");

        int astartesCategoryId = Integer.valueOf(rs.getString("astartes_category"));
        AstartesCategory astartes_category = getAstartesCategoryById(astartesCategoryId);

        String chapter_name = rs.getString("chapter_name");
        long marines_count = rs.getLong("marines_count");

        Chapter chapter = new Chapter(chapter_name, marines_count);
        Coordinates coordinates = new Coordinates(coord_x, coord_y);

        String username = getUserUsernameById(rs.getInt("user_id"));

        SpaceMarine marine = new SpaceMarine(
                id,
                name,
                coordinates,
                creation_date,
                health,
                heart_count,
                achievements,
                astartes_category,
                chapter
                );

        marine.setUsername(username);
        return marine;
    }

    private synchronized AstartesCategory getAstartesCategoryById(int id) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT category_name FROM categories WHERE category_id = ?;"
        );
        statement.setInt(1, id);
        ResultSet rs = statement.executeQuery();
        rs.next();
        return AstartesCategory.valueOf(rs.getString(1));
    }

    public synchronized void closeConnection() throws SQLException {
        connection.close();
    }

    public Connection getConnection() {
        return connection;
    }
}