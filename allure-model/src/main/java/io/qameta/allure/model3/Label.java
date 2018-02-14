package io.qameta.allure.model3;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author charlie (Dmitry Baev).
 */
@Data
@Accessors(chain = true)
public class Label implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected String value;

}
