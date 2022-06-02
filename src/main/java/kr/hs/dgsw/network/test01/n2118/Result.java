package kr.hs.dgsw.network.test01.n2118;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Result {
    private final String statement;
    private final List<String> arguments;

    public Result(String statement) {
        this.statement = statement;
        this.arguments = new ArrayList<>();
    }

    public Result addArgument(String argument) {
        arguments.add(argument);
        return this;
    }

    public String getStatement() {
        return statement;
    }

    public String build() {
        return getString(statement, arguments);
    }

    public static String getString(String statement, List<String> arguments) {
        StringBuilder sb = new StringBuilder();

        sb.append(statement).append("\n");
        sb.append(arguments.size()).append("\n");

        for (String argument : arguments) {
            sb.append(argument).append("\n");
        }

        return sb.toString();
    }

    public boolean isSuccess() {
        return statement.equals("SUCCESS");
    }

    public boolean isFail() {
        return !isSuccess();
    }

    public List<String> getArguments() {
        return arguments;
    }

    public static Result receive(BufferedReader reader) throws IOException {
        Result result = new Result(reader.readLine());
        int n = Integer.parseInt(reader.readLine());

        for (int i = 0; i < n; i++) {
            result.addArgument(reader.readLine());
        }

        return result;
    }
}
