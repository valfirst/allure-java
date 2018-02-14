package io.qameta.allure.model3;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author charlie (Dmitry Baev).
 */
@Data
@Accessors(chain = true)
public class StepResult implements Serializable, Executable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected Long start;
    protected Long stop;
    protected Status status;
    protected String statusMessage;
    protected String statusTrace;

    protected List<StepResult> steps = new ArrayList<>();
    protected List<Attachment> attachments = new ArrayList<>();
    protected Set<Parameter> parameters = new HashSet<>();

}
