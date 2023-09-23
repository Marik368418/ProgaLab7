package server.concurrency;

import managers.SerializationManager;
import transfers.Response;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RecursiveAction;

public class SendResponseTask extends RecursiveAction {
    private SocketChannel sc;
    private Response response;

    public SendResponseTask(SocketChannel sc, Response response) {
        this.response = response;
        this.sc = sc;
    }

    @Override
    protected void compute() {
        try {
            sc.write(SerializationManager.serialize(response));
        } catch (IOException e) {
            System.out.println("Ошибка отправки ответа клиенту!");
        }
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}