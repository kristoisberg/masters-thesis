package xyz.kristoi.jooq;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

public class StatementCounter {

    // Sinks represent the "end" of a chain where execution happens
    private static final Set<String> EXECUTION_SINKS =
            Set.of(
                    // Standard DSL sinks
                    "fetch", "fetchOne", "fetchAny", "fetchMany", "fetchMap",
                    "fetchArray", "fetchInto", "fetchAsync", "execute",
                    "executeAsync", "stream", "batch",
                    // DAO specific sinks (generated DAOs)
                    "insert", "update", "delete", "deleteById", "exists",
                    "existsById", "count", "findAll", "findById"
            );

    // Markers help identify if a chain belongs to jOOQ
    private static final Set<String> JOOQ_MARKERS =
            Set.of(
                    "select", "selectFrom", "insertInto", "update", "delete",
                    "deleteFrom", "where", "join", "innerJoin", "leftJoin",
                    "values", "set", "onDuplicateKeyUpdate", "returning",
                    "fetchNext", "forUpdate", "union", "intersect", "with",
                    "condition", "and", "or", "asTable"
            );

    private static final ParserConfiguration parserConfiguration =
            new ParserConfiguration().setLanguageLevel(
                    ParserConfiguration.LanguageLevel.BLEEDING_EDGE
            );

    public long count(Path projectPath) throws IOException {
        return Files.walk(projectPath)
                .filter(this::isRelevantFile)
                .parallel()
                .map(this::parseFile)
                .flatMap(Optional::stream)
                .map(this::analyzeFile)
                .mapToLong(Long::longValue)
                .sum();
    }

    private boolean isRelevantFile(Path path) {
        String str = path.toString();
        // Ignore generated DAOs themselves to avoid double counting definitions
        return str.endsWith(".java") && !str.contains("/generated/");
    }

    private Optional<CompilationUnit> parseFile(Path path) {
        try {
            JavaParser localParser = new JavaParser(parserConfiguration);
            ParseResult<CompilationUnit> result = localParser.parse(path);

            if (result.isSuccessful() && result.getResult().isPresent()) {
                return result.getResult();
            }
        } catch (IOException e) {
            System.err.println("Failed to read " + path + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    private long analyzeFile(CompilationUnit compilationUnit) {
        return compilationUnit
                .findAll(MethodCallExpr.class)
                .stream()
                .filter(method -> EXECUTION_SINKS.contains(method.getNameAsString()))
                .filter(this::isLikelyJooq)
                .count();
    }

    private boolean isLikelyJooq(MethodCallExpr method) {
        // 1. Identify the chain root (the very first call or object)
        Optional<com.github.javaparser.ast.expr.Expression> scope = method.getScope();

        if (scope.isEmpty()) return false;

        // 2. Look for strong "DSL" starters in the chain
        boolean hasJooqStarter = method.toString().matches("(?s).*(select|insertInto|deleteFrom|update|selectFrom)\\(.*");
        if (hasJooqStarter) return true;

        // 3. Check for specific DAO-like patterns
        String scopeStr = scope.get().toString();
        boolean isCommonDslVar = scopeStr.matches("(?i).*(dsl|ctx|jooq|create).*");

        // 4. Heuristic: Check if the arguments look like jOOQ Metadata (Upper_Case)
        boolean hasJooqArgs = method.getArguments().stream()
                .anyMatch(arg -> arg.toString().matches("^[A-Z0-9_.]+$"));

        if (isCommonDslVar || hasJooqArgs) return true;

        // 5. Deep recursion to find a JOOQ_MARKER anywhere in the chain
        if (scope.get() instanceof MethodCallExpr parentCall) {
            if (JOOQ_MARKERS.contains(parentCall.getNameAsString())) {
                return true;
            }
            return isLikelyJooq(parentCall);
        }

        return false;
    }
}
