package server.commands;


import exceptions.WrongArgumentException;
import server.supervisor.Supervisor;
import transfers.Request;
import transfers.Response;
import transfers.ResponseStatus;

import java.sql.SQLException;

public class ClearCommand extends AbstractCommand {
    public ClearCommand(Supervisor supervisor) {
        super("clear", "Очистить коллекцию", supervisor);
    }

    @Override
    public Response execute(Request request) throws WrongArgumentException {
        if (!request.getArgs().isBlank()) throw new WrongArgumentException("");
        try {
            String username = request.getAuthenticationData().getUsername();
            supervisor.getDatabase().clearData(username);
            return new Response(ResponseStatus.OK, "Все ваши записи очищены!");
        }
        catch (SQLException e) {
            return new Response(ResponseStatus.OK, "Ошибка очистки записей!");
        }
    }
}
