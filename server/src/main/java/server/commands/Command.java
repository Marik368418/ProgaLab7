package server.commands;

import exceptions.WrongArgumentException;
import exceptions.WrongValuesException;
import transfers.Request;
import transfers.Response;

import java.sql.SQLException;

public interface Command {
    public Response execute(Request request) throws WrongValuesException, WrongArgumentException, SQLException;
}
