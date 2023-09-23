package server.commands;


import exceptions.WrongArgumentException;
import exceptions.WrongValuesException;
import model.SpaceMarine;
import server.supervisor.Supervisor;
import transfers.Request;
import transfers.Response;
import transfers.ResponseStatus;

import java.sql.SQLException;
import java.util.Objects;

public class AddCommand extends AbstractCommand {
    public AddCommand(Supervisor supervisor) {
        super("add", "Добавить новый элемент в коллекцию", supervisor);
    }

    @Override
    public Response execute(Request request) throws WrongValuesException, WrongArgumentException, SQLException {
        if (!request.getArgs().isBlank()) throw new WrongArgumentException("");
        if (Objects.isNull(request.getObject())) {
            return new Response(ResponseStatus.ERROR, "Для команды " + this.getName() +
                    " требуется объект.");
        } else {
            String username = request.getAuthenticationData().getUsername();
            supervisor.getDatabase().addItem(username, request.getObject());
            return new Response(ResponseStatus.OK, "Элемент успешно добавлен");
        }
    }
}

