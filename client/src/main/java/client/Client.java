package client;

import exceptions.WrongValuesException;
import managers.SerializationManager;
import transfers.AuthenticationData;
import transfers.Request;
import transfers.Response;
import transfers.ResponseStatus;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client {
    private String username;
    private String password;

    private final int port;
    private final int reconnectionTimeout;
    private final int maxReconnectionAttempts;
    private final Manager manager;
    private SocketChannel sc;

    public Client(int port, int reconnectionTimeout, int maxReconnectionAttempts, Manager manager) {
        this.port = port;
        this.reconnectionTimeout = reconnectionTimeout;
        this.maxReconnectionAttempts = maxReconnectionAttempts;
        this.manager = manager;
    }

    public void run() {
        try {
            connect();
        }
        catch (InterruptedException | WrongValuesException e) {
            return;
        }

       if (!processAuthentication())
           return;

        boolean processingStatus = true;
        while (processingStatus) {
            processingStatus = processRequestToServer();
        }
    }

    private boolean connect() throws WrongValuesException, InterruptedException {
        System.out.println("Подключение к серверу...");
        int reconnectionAttempts = 0;

        while (reconnectionAttempts <= maxReconnectionAttempts) {

            if (reconnectionAttempts > 0) {
                System.out.println("Производится повторное подключение");
                Thread.sleep(reconnectionTimeout);
            }
            try {
                sc = SocketChannel.open();
                sc.connect(new InetSocketAddress(port));

                System.out.println("Соединение с сервером успешно установлено.");

                System.out.println("Ожидание разрешения на обмен данными.");

                System.out.println("Разрешение на обмен данными получено.");
                System.out.println("Добро пожаловать!");

                reconnectionAttempts = 0;
                return true;

            } catch (IllegalArgumentException e) {
                System.out.println("Адрес сервера введен некорректно");
                throw new WrongValuesException("");
            } catch (IOException e) {
                System.out.println("Произошла ошибка соединения с сервером.");
                reconnectionAttempts++;
            }
        }
        System.out.println("Превышено количество попыток подключения");
        return false;
    }

    private boolean processRequestToServer() {
        Request requestToServer = null;
        Response serverResponse = null;

        do {
            try {
                requestToServer = serverResponse != null ?
                        manager.handle(serverResponse.getStatus()) :
                        manager.handle(null);

                if (requestToServer.isEmpty()) continue;

                requestToServer.setAuthenticationData(new AuthenticationData(username, password));
                sc.write(SerializationManager.serialize(requestToServer));

                ByteBuffer bb = ByteBuffer.allocate(10000);
                sc.read(bb);

                serverResponse = (Response) SerializationManager.deserialize(bb);
                int responseLen = 0;

                try {
                    responseLen = Integer.parseInt(serverResponse.getResponseBody());
                }
                catch (NumberFormatException ignored){};

                if (responseLen == 0) {
                    System.out.println(serverResponse.getResponseBody());
                }
                else {
                    while (responseLen != 0) {

                        ByteBuffer bb2 = ByteBuffer.allocate(10000);
                        sc.read(bb2);

                        serverResponse = (Response) SerializationManager.deserialize(bb2);;
                        System.out.println(serverResponse.getResponseBody());
                        responseLen--;
                    }
                }
            } catch (InvalidClassException | NotSerializableException e) {
                System.out.println("Произошла ошибка при отправке данных на сервер");
            } catch (ClassNotFoundException e) {
                System.out.println("Произошла ошибка при чтении полученных данных");
            } catch (IOException e) {
                System.out.println("Соединение с сервером разорвано");

                try {
                    boolean shouldExit = connect();
                    if (!shouldExit) return false;

                } catch (WrongValuesException | InterruptedException reconnectionEx) {
                    if (requestToServer.getCommandName().equals("exit"))
                        System.out.println("Команда не зарегистрирована на сервере");
                    else System.out.println("Попробуйте повторить команду позже");
                }
            }
        } while (!requestToServer.getCommandName().equals("exit"));
        return false;
    }

    private boolean processAuthentication() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String authWay;
        String username;
        String password;
        Request requestToServer;

        try {
            while (true) {
                System.out.print("\nАвторизуйтесь (1 - вход, 2 - создать новую учетную запись): ");
                authWay = reader.readLine();

                if (authWay.equals("1") || authWay.equals("2")) {
                    System.out.print("Введите имя пользователя: ");
                    username = reader.readLine();
                    System.out.print("Введите пароль: ");
                    password = reader.readLine();

                    if (!checkAuthData(username, password)) {
                        System.out.println("Имя пользователя и пароль должны быть от 5 до 15 символов. " +
                                "Повторите попытку.");
                        continue;
                    }

                    if (authWay.equals("1"))
                        requestToServer = new Request("log_in");
                    else
                        requestToServer = new Request("sign_up");

                    requestToServer.setAuthenticationData(new AuthenticationData(username, password));

                    sc.write(SerializationManager.serialize(requestToServer));

                    ByteBuffer bb = ByteBuffer.allocate(10000);
                    sc.read(bb);
                    Response response = (Response) SerializationManager.deserialize(bb);

                    System.out.println(response.getResponseBody());
                    if (response.getStatus() == ResponseStatus.OK) {
                        this.username = username;
                        this.password = password;
                        return true;
                    }
                }
                else {
                    System.out.println("Неверный ввод. Попробуйте ещё раз.\n");
                }
            }
        }
        catch (IOException | ClassNotFoundException e) {
            System.out.println("Ошибка сервера!");
            return false;
        }
    }

    private boolean checkAuthData(String username, String password) {
        if (username.length() > 15 || username.length() < 5 || password.length() > 15 || password.length() < 5)
            return false;
        else
            return true;
    }
}