package io.qameta.allure.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * @author charlie (Dmitry Baev).
 */
public enum Stage implements Serializable {

    NOT_IMPLEMENTED("not-implemented"),
    SCHEDULED("scheduled"),
    RUNNING("running"),
    FINISHED("finished"),
    PENDING("pending"),
    INTERRUPTED("interrupted");

    private static final long serialVersionUID = 1L;

    private final String value;

    Stage(final String v) {
        value = v;
    }

    @JsonValue
    public String value() {
        return value;
    }

}