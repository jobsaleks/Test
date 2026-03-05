package ru.alphabank.uvs.validation.api;

import ru.alphabank.uvs.validation.dto.ValidationResponseDto;

import java.util.concurrent.atomic.AtomicReference;

public final class ValidationResponseWrapper {
    public enum Decision {UNDECIDED, COMMIT, ROLLBACK}

    private final ValidationResponseDto response;
    private final AtomicReference<Decision> decision = new AtomicReference<>(Decision.UNDECIDED);

    public ValidationResponseWrapper(ValidationResponseDto response) {
        this.response = response;
    }

    public ValidationResponseDto response() {
        return response;
    }

    public void commit() {
        decision.set(Decision.COMMIT);
    }

    public void rollback() {
        decision.set(Decision.ROLLBACK);
    }

    public Decision decisionOrDefaultCommit() {
        Decision value = decision.get();
        return value == Decision.UNDECIDED ? Decision.COMMIT : value;
    }
}
