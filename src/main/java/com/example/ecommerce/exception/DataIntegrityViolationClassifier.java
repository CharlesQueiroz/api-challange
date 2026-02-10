package com.example.ecommerce.exception;

import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Locale.ROOT;

enum DataIntegrityViolationClassifier {
    UNIQUE_VIOLATION("Duplicate value violates uniqueness constraint", context -> "23505".equals(context.sqlState())),
    FOREIGN_KEY_VIOLATION("Referenced resource not found or invalid", context -> "23503".equals(context.sqlState())),
    CHECK_VIOLATION("Value violates check constraint", context -> "23514".equals(context.sqlState())),
    GENERIC_VIOLATION("Data integrity violation", context -> true);

    private final String detail;
    private final Predicate<Context> strategy;

    DataIntegrityViolationClassifier(String detail, Predicate<Context> strategy) {
        this.detail = detail;
        this.strategy = strategy;
    }

    static String resolveDetail(DataIntegrityViolationException ex) {
        var context = Context.from(ex);
        return Arrays.stream(values())
                .filter(rule -> rule.strategy.test(context))
                .findFirst()
                .orElse(GENERIC_VIOLATION)
                .detail;
    }

    private record Context(String sqlState, String message) {
        static Context from(DataIntegrityViolationException ex) {
            var message = Optional.of(ex.getMostSpecificCause())
                    .map(Throwable::getMessage)
                    .or(() -> Optional.ofNullable(ex.getMessage()))
                    .map(it -> it.toLowerCase(ROOT))
                    .orElse("");

            var sqlState = findSqlState(ex).orElse("");
            return new Context(sqlState, message);
        }

        private static Optional<String> findSqlState(Throwable throwable) {
            var current = throwable;
            while (current != null) {
                if (current instanceof SQLException sqlException && sqlException.getSQLState() != null) {
                    return Optional.of(sqlException.getSQLState());
                }
                current = current.getCause();
            }
            return Optional.empty();
        }
    }
}
