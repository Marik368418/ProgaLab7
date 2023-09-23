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

public class AddIfMaxCommand extends AbstractCommand {
    public AddIfMaxCommand(Supervisor supervisor) {
        super("add_if_max", "Добавить новый элемент в коллекцию, если его значение превышает значение наибольшего элемента этой коллекции", supervisor);
    }

    @Override
    public Response execute(Request request) throws WrongValuesException, WrongArgumentException, SQLException {
        if (!request.getArgs().isBlank()) throw new WrongArgumentException("");
        if (Objects.isNull(request.getObject())) {
            return new Response(ResponseStatus.ERROR, "Для команды " + this.getName() + " требуется объект.");
        }
        SpaceMarine element = request.getObject();
        if (element.compareTo(Objects.requireNonNull(supervisor.getCollection().stream()
                .filter(Objects::nonNull)
                .max(SpaceMarine::compareTo)
                .orElse(null))) >= 1) {

            String username = request.getAuthenticationData().getUsername();
            supervisor.getDatabase().addItem(username, element);
            return new Response(ResponseStatus.OK, "Элемент успешно добавлен");
        }
        else {
            return new Response(ResponseStatus.ERROR, "Элемент не соответствует условиям команды.");
        }
    }
}
