package io.qameta.allure;

import java.io.Serializable;

/**
 * @author charlie (Dmitry Baev).
 */
public enum SeverityLevel implements Serializable {

    BLOCKER("blocker"),
    CRITICAL("critical"),
    NORMAL("normal"),
    MINOR("minor"),
    TRIVIAL("trivial");

    private static final long serialVersionUID = 1L;

    private final String value;

    SeverityLevel(final String v) {
        value = v;
    }

    public String value() {
        return value;
    }
}
