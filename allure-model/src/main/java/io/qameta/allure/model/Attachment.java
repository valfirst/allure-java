package io.qameta.allure.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author charlie (Dmitry Baev).
 */
@Data
@Accessors(chain = true)
public class Attachment implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String uuid;
    protected String name;
    protected String source;
    protected String contentType;
    protected Long timestamp;

}
