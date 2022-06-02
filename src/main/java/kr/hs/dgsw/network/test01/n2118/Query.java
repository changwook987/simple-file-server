package kr.hs.dgsw.network.test01.n2118;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static kr.hs.dgsw.network.test01.n2118.Result.getString;

public class Query {
    private final String command;

    private final List<String> arguments;

    public Query(String command) {
        this.command = command;
        this.arguments = new LinkedList<>();
    }

    public Query addArgument(String argument) {
        arguments.add(argument);
        return this;
    }

    public String build() {
        return getString(command, arguments);
    }

    public String getCommand() {
        return command;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public static Query receive(BufferedReader reader) throws IOException, NumberFormatException {
        Query query = new Query(reader.readLine());

        int n = Integer.parseInt(reader.readLine());

        for (int i = 0; i < n; i++) {
            query.addArgument(reader.readLine());
        }

        return query;
    }
}
