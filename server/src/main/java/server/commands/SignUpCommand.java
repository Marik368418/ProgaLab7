package server.commands;

import exceptions.WrongArgumentException;
import exceptions.WrongValuesException;
import server.supervisor.Supervisor;
import transfers.*;

import java.sql.SQLException;

public class SignUpCommand extends AbstractCommand {
    public SignUpCommand(Supervisor supervisor) {
        super("sign_up", "Зарегистрироваться", supervisor);
    }

    @Override
    public Response execute(Request request) throws WrongValuesException, WrongArgumentException, SQLException {
        String username = request.getAuthenticationData().getUsername();

        if (supervisor.registerUser(request.getAuthenticationData())) {
            System.out.println("Пользователь " + username + " успешно зарегистрирован!");
            return new Response(ResponseStatus.OK, "Вы успешно зарегистрированы!");
        }
        else {
            return new Response(ResponseStatus.AUTH_ERROR, "Пользователь с таким именем уже существует!");
        }
    }
}
