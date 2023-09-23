package server.concurrency;
import managers.SerializationManager;
import server.supervisor.Supervisor;
import transfers.Request;
import transfers.Response;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;

public class RequestReader implements Runnable {
    private Supervisor supervisor;
    private Selector selector;

    private ForkJoinPool forkJoinPool = ThreadPoolFactory.getForkJoinPool();
    private ConcurrentLinkedQueue<Response> responsesQueue = new ConcurrentLinkedQueue<>();
    private long lastResponse = System.currentTimeMillis();

    public RequestReader(Selector selector, Supervisor supervisor){
        this.supervisor = supervisor;
        this.selector = selector;
    }

    @Override
    public void run() {
        try {
            while (true) {
                SelectionKey key = null;
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    key = iterator.next();
                    iterator.remove();
                }

                if (key.isReadable()) {
                    ByteBuffer bb = ByteBuffer.allocate(10000);
                    SocketChannel sc = (SocketChannel) key.channel();
                    sc.read(bb);
                    Request userRequest = (Request) SerializationManager.deserialize(bb);
                    ExecuteRequestTask executeRequestTask
                            = new ExecuteRequestTask(supervisor, sc, userRequest, responsesQueue);
                    forkJoinPool.execute(executeRequestTask);

                }
                else if (key.isWritable() && !responsesQueue.isEmpty()
                        && System.currentTimeMillis() - lastResponse > 50) {

                    Response response = responsesQueue.poll();

                    SocketChannel sc = (SocketChannel) key.channel();
                    SendResponseTask responseTask = new SendResponseTask(sc, response);
                    forkJoinPool.execute(responseTask);
                    lastResponse = System.currentTimeMillis();
                }
            }
        }
        catch (IOException e) {
            System.out.println("Ошибка клиента! Соединение разорвано");
        } catch (ClassNotFoundException e) {
            System.out.println("Ошибка чтения запроса! Соединение разорвано");
        }
    }
}