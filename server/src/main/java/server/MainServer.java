package server;

import server.concurrency.ServerCommandController;
import server.exceptions.ServerLaunchException;
import server.supervisor.Supervisor;
import server.util.DatabaseProcessor;

import java.io.FileNotFoundException;
import java.sql.SQLException;

public class MainServer {


    public static void main(String[] args) throws Exception {
        try {
            // Personal computer
            var supervisor = new Supervisor(new DatabaseProcessor(), ".credentials");
            var server = new Server(supervisor, 49160);

            Thread controllingServerThread = new Thread(new ServerCommandController());
            controllingServerThread.setDaemon(true);
            controllingServerThread.start();

            server.run();
        }
        catch (SQLException e) {
            throw new ServerLaunchException("Ошибка подключения к базе данных!!!");
        }
        catch (FileNotFoundException e) {
            System.out.println("Не найден файл .credentials с данными авторизации в базе данных!");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("_");
        }
    }
}