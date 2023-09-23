package server.commands;

import exceptions.WrongArgumentException;
import exceptions.WrongValuesException;
import server.supervisor.Supervisor;
import transfers.Request;
import transfers.Response;
import transfers.ResponseStatus;

import java.sql.SQLException;

public class LogInCommand extends AbstractCommand {
    public LogInCommand(Supervisor supervisor) {
        super("log_in", "Авторизоваться", supervisor);
    }

    @Override
    public Response execute(Request request) throws WrongValuesException, WrongArgumentException, SQLException {
        String username = request.getAuthenticationData().getUsername();

        if (supervisor.checkCredentials(request.getAuthenticationData())) {
            System.out.println("Пользователь " + username + " успешно авторизовался!");
            return new Response(ResponseStatus.OK, "Вы успешно авторизованы!");
        }
        else {
            return new Response(ResponseStatus.AUTH_ERROR, "Неправильное имя пользователя или пароль");
        }
    }
}
