package server.concurrency;

import server.Server;
import transfers.Response;
import transfers.ResponseStatus;

import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RecursiveAction;

public class BuildResponseTask extends RecursiveAction {
    private SocketChannel sc;
    private Response response;
    private ConcurrentLinkedQueue<Response> responsesQueue;

    public BuildResponseTask(SocketChannel channel, Response response, ConcurrentLinkedQueue<Response> responsesQueue) {
        this.responsesQueue = responsesQueue;
        this.sc = channel;
        this.response = response;
    }

    @Override
    protected void compute() {
        String body = response.getResponseBody();

        int responseBodyLength = body.getBytes(StandardCharsets.UTF_8).length;

        if (responseBodyLength > 5000) {
            int amountOfPackages = responseBodyLength / 4000 + (responseBodyLength % 4000 != 0 ? 1 : 0);

            responsesQueue.offer(new Response(ResponseStatus.OK, String.valueOf(amountOfPackages)));
            String[] responseParts = divideOnSameParts(body, 4000);

            for (String val : responseParts) {
                Response resp = new Response(ResponseStatus.OK, val);
                responsesQueue.offer(resp);
            }
        }
        else {
            responsesQueue.offer(response);
        }
    }

    public static String[] divideOnSameParts(String src, int len) {
        String[] result = new String[(int)Math.ceil((double)src.length()/(double)len)];
        for (int i=0; i<result.length; i++)
            result[i] = src.substring(i*len, Math.min(src.length(), (i+1)*len));
        return result;
    }
}
