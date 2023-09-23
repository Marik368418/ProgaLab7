package server.commands;


import exceptions.WrongValuesException;
import server.exceptions.AccessException;
import server.supervisor.Supervisor;
import transfers.Request;
import transfers.Response;
import transfers.ResponseStatus;

import java.sql.SQLException;

public class RemoveByIdCommand extends AbstractCommand {
    public RemoveByIdCommand(Supervisor supervisor) {
        super("remove", "Удалить элемент из коллекции по его id", supervisor);
    }

    @Override
    public Response execute(Request request) throws WrongValuesException, SQLException {
        if (!request.getArgs().matches("-?\\d+")) {
            return new Response(ResponseStatus.ERROR, "id должен быть целочисленным!");
        }
        if (supervisor.getCollection().isEmpty()) {
            return new Response(ResponseStatus.OK, "Коллекция пуста - нельзя ничего удалить!");
        } else {
            String username = request.getAuthenticationData().getUsername();
            int id = Integer.parseInt(request.getArgs());

            try {
                supervisor.getDatabase().removeByIdItem(username, id);
                return new Response(ResponseStatus.OK, "Элемент коллекции с id "
                        + request.getArgs() + " успешно удалён!");
            }
            catch (AccessException e) {
                return new Response(ResponseStatus.OK, e.getMessage());
            }
        }
    }
}
