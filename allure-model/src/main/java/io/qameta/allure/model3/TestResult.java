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
@SuppressWarnings("PMD.TooManyFields")
@Data
@Accessors(chain = true)
public class TestResult implements Serializable, Executable {

    private static final long serialVersionUID = 1L;

    protected TestResultType type = TestResultType.TEST;

    protected String uuid;
    protected String name;
    protected String fullName;

    protected String historyId;
    protected String testId;

    protected Long start;
    protected Long stop;

    protected String description;
    protected String descriptionHtml;

    protected Stage stage = Stage.NOT_IMPLEMENTED;

    protected Status status;
    protected String statusMessage;
    protected String statusTrace;

    protected boolean flaky;
    protected String flakyMessage;

    protected boolean muted;
    protected String mutedMessage;

    protected boolean known;

    protected List<StepResult> steps = new ArrayList<>();
    protected List<Attachment> attachments = new ArrayList<>();
    protected Set<Parameter> parameters = new HashSet<>();

    protected Set<Label> labels = new HashSet<>();
    protected Set<Link> links = new HashSet<>();

    protected List<String> children = new ArrayList<>();

}
