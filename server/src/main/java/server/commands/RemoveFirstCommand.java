package server.commands;


import server.exceptions.AccessException;
import server.supervisor.Supervisor;
import transfers.Request;
import transfers.Response;
import transfers.ResponseStatus;

import java.sql.SQLException;

public class RemoveFirstCommand extends AbstractCommand {
    public RemoveFirstCommand(Supervisor supervisor) {
        super("remove_first", "Удалить первый элемент из коллекции", supervisor);
    }

    @Override
    public Response execute(Request request) throws SQLException {
        if (supervisor.getCollection().isEmpty()) {
            return new Response(ResponseStatus.OK, "Коллекция пуста - нельзя ничего удалить!");
        } else {
            String username = request.getAuthenticationData().getUsername();
            try {
                supervisor.getDatabase().removeFirstItem(username);
                return new Response(ResponseStatus.OK, "Первый элемент коллекции успешно удалён!");
            }
            catch (AccessException e) {
                return new Response(ResponseStatus.OK, e.getMessage());
            }
        }
    }
}
