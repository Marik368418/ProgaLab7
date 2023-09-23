package server;

import server.concurrency.RequestReader;
import server.concurrency.ThreadPoolFactory;
import server.exceptions.ServerLaunchException;
import server.supervisor.Supervisor;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class Server {
    private int port;
    private ServerSocketChannel ssc;
    private Selector selector;
    private Supervisor supervisor;

    private ExecutorService fixedThreadPool;

    public Server(Supervisor supervisor, int port) {
        fixedThreadPool = ThreadPoolFactory.getFixedThreadPool();
        this.supervisor = supervisor;
        this.port = port;
    }

    public void open() throws ServerLaunchException {
        try {
            System.out.println("Запуск сервера... ");
            ssc = ServerSocketChannel.open();
            ssc.bind(new InetSocketAddress("localhost", port));
            ssc.configureBlocking(false);
            selector = Selector.open();
            ssc.register(selector, SelectionKey.OP_ACCEPT);

            activateServer();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.out.println("Порт " + port + " недоступен!");
            throw new ServerLaunchException("Сервер не смог запуститься!!!");
        } catch (IOException e) {
            System.out.println("Непредвиденная ошибка при использовании порта " + port + "!");
            throw new ServerLaunchException("Сервер не смог запуститься!!!");
        }
    }

    public void run() throws ServerLaunchException, IOException {
        open();
        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            SelectionKey key = null;

            while (iterator.hasNext()) {
                key = iterator.next();
                iterator.remove();
            }
            try {
                if (key.isAcceptable()) {
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    Selector requestReadingSelector = Selector.open();
                    sc.register(requestReadingSelector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    sc.finishConnect();
                    System.out.println("Соединение с клиентом " + sc.getRemoteAddress() + " установлено!");

                    RequestReader requestReader = new RequestReader(requestReadingSelector, supervisor);
                    fixedThreadPool.execute(requestReader);
                }
            }
            catch (IOException e) {
                key.channel().close();
                System.out.println("Ошибка клиента");
            }
        }
    }

    public void activateServer() {
        System.out.println("Сервер запущен!");
    }
}
