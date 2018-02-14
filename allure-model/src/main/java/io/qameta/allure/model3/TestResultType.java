package io.qameta.allure.model3;

import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * @author charlie (Dmitry Baev).
 */
public enum TestResultType implements Serializable {

    TEST("test"),
    SET_UP("setUp"),
    TEAR_DOWN("tearDown"),
    DATA_GENERATOR("dataGenerator");

    private static final long serialVersionUID = 1L;

    private final String value;

    TestResultType(final String v) {
        value = v;
    }

    @JsonValue
    public String value() {
        return value;
    }

}
