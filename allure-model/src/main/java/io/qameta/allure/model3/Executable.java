package io.qameta.allure.model3;

import java.util.List;
import java.util.Set;

/**
 * @author charlie (Dmitry Baev).
 */
public interface Executable {

    Long getStart();

    Long getStop();

    List<StepResult> getSteps();

    List<Attachment> getAttachments();

    Set<Parameter> getParameters();

}
