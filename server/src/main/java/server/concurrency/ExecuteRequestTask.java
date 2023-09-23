package server.concurrency;

import exceptions.WrongArgumentException;
import exceptions.WrongCommandException;
import exceptions.WrongValuesException;
import server.commands.Command;
import server.supervisor.Supervisor;
import transfers.Request;
import transfers.Response;
import transfers.ResponseStatus;

import java.nio.channels.SocketChannel;
import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class ExecuteRequestTask extends RecursiveAction {
    private Supervisor supervisor;
    private Request request;
    private SocketChannel sc;
    private ConcurrentLinkedQueue<Response> responsesQueue;

    public ExecuteRequestTask(Supervisor supervisor, SocketChannel sc, Request request,
                              ConcurrentLinkedQueue<Response> responsesQueue) {
        this.responsesQueue = responsesQueue;
        this.supervisor = supervisor;
        this.request = request;
        this.sc = sc;
    }

    @Override
    protected void compute() {
        Response response;
        try {
            if (!request.getCommandName().equals("log_in") && !request.getCommandName().equals("sign_up")
                    && !supervisor.checkCredentials(request.getAuthenticationData())) {

                response = new Response(ResponseStatus.AUTH_ERROR, "Ошибка авторизации!");
            }
            else {
                Collections.sort(supervisor.getCollection());
                Command command = supervisor.getCommandMap().get(request.getCommandName());
                if (command == null) throw new WrongCommandException();
                response = command.execute(request);
            }

            System.out.println("Запрос '" + request.getCommandName() + "' успешно обработан");

        } catch (WrongArgumentException e) {
            response = new Response(ResponseStatus.ERROR, "Введены неправильные аргументы команды.");
        } catch (WrongValuesException e) {
            response = new Response(ResponseStatus.ERROR, "Ошибка при исполнении программы");
        } catch (WrongCommandException e) {
            response = new Response(ResponseStatus.ERROR, "Такой команды не существует.");
        }
        catch (SQLException e) {
            response = new Response(ResponseStatus.ERROR, "Ошибка при работе с базой данных.");
        }

        BuildResponseTask responseTask = new BuildResponseTask(sc, response, responsesQueue);
        ForkJoinPool forkJoinPool = ThreadPoolFactory.getForkJoinPool();
        forkJoinPool.execute(responseTask);
    }
}