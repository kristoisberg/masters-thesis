package xyz.kristoi.jooq;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CliApplication {

    static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: java JooqAnalyzer.java <project_path>");
            return;
        }

        StatementCounter statementCounter = new StatementCounter();
        Path projectPath = Paths.get(args[0]);
        System.out.println(statementCounter.count(projectPath));
    }
}
