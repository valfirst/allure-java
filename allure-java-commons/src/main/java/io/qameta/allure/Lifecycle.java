package io.qameta.allure;

import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Executable;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.writer.AttachmentContentWriter;
import io.qameta.allure.writer.DummyAttachmentContentWriter;
import io.qameta.allure.writer.ResultsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static io.qameta.allure.util.ResultsUtils.getStackTraceAsString;
import static io.qameta.allure.util.ResultsUtils.getStatus;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
public class Lifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(Lifecycle.class);

    private final ExecutableStorage storage = new ExecutableStorage();

    private final ResultsWriter writer;

    private final Map<String, TestResult> results = new ConcurrentHashMap<>();

    public Lifecycle(final ResultsWriter writer) {
        this.writer = writer;
    }

    public synchronized void startTest(final TestResult result) {
        if (Objects.isNull(result.getUuid())) {
            result.setUuid(UUID.randomUUID().toString());
        }
        result.setStart(System.currentTimeMillis());
        result.setStage(Stage.RUNNING);
        storage.remove();
        storage.get().add(result);
        results.put(result.getUuid(), result);
    }

    public synchronized void updateTest(final Consumer<TestResult> updateFunction) {
        final Optional<TestResult> current = currentTest();
        if (current.isPresent()) {
            updateFunction.accept(current.get());
        } else {
            LOGGER.error("Could not update test: there is no test run at the moment");
        }
    }

    public synchronized void updateTest(final String uuid, final Consumer<TestResult> updateFunction) {
        final TestResult result = results.get(uuid);
        if (!Objects.isNull(result)) {
            updateFunction.accept(result);
        } else {
            LOGGER.error("Could not update test: there is no test run at the moment");
        }
    }

    public synchronized void stopTest() {
        final Optional<TestResult> current = currentTest();
        if (current.isPresent()) {
            final TestResult result = current.get();
            result.setStop(System.currentTimeMillis());
        } else {
            LOGGER.error("Could not stop test: there is no test run at the moment");
        }
        storage.remove();
    }

    public synchronized void writeTest(final String uuid) {
        Objects.requireNonNull(uuid, "Uuid should not be null value");
        Optional.ofNullable(results.remove(uuid)).ifPresent(result -> {
            System.out.println("writer: " + result);
            writer.writeResult(result);
        });
    }

    public synchronized void startStep(final StepResult result) {
        final Optional<Executable> current = currentStepOrTest();
        if (current.isPresent()) {
            final Executable executable = current.get();
            executable.getSteps().add(result);
            storage.get().add(result);
        } else {
            LOGGER.error("Could not start step: there is not test run at the moment");
        }
    }

    public synchronized void updateStep(final Consumer<StepResult> updateFunction) {
        final Optional<StepResult> current = currentStep();
        if (current.isPresent()) {
            updateFunction.accept(current.get());
        } else {
            LOGGER.error("Could not update step: there is no step run at the moment");
        }
    }

    public synchronized void stopStep() {
        final Optional<StepResult> current = currentStep();
        if (current.isPresent()) {
            storage.get().pollLast();
        } else {
            LOGGER.error("Could not stop step: there is not step run at the moment");
        }
    }

    public synchronized AttachmentContentWriter addAttachment(final String name,
                                                              final String contentType, final String extension) {
        final Optional<Executable> executable = currentStepOrTest();
        if (executable.isPresent()) {
            final String uuid = UUID.randomUUID().toString();
            final String source = String.format("%s.%s", uuid, extension);
            final Attachment attachment = new Attachment()
                    .setName(name)
                    .setContentType(contentType)
                    .setUuid(uuid)
                    .setTimestamp(System.currentTimeMillis())
                    .setSource(source);
            executable.get().getAttachments().add(attachment);
            return writer.writeAttachment(source);
        } else {
            LOGGER.error("Could not add attachment: there is no test run at the moment");
        }
        return new DummyAttachmentContentWriter();
    }

    public Optional<TestResult> currentTest() {
        final Deque<Executable> deque = storage.get();
        return Optional.ofNullable(deque.peekFirst())
                .filter(TestResult.class::isInstance)
                .map(TestResult.class::cast);
    }

    public Optional<Executable> currentStepOrTest() {
        final Deque<Executable> deque = storage.get();
        return Optional.ofNullable(deque.peekLast());
    }

    public Optional<StepResult> currentStep() {
        return currentStepOrTest()
                .filter(StepResult.class::isInstance)
                .map(StepResult.class::cast);
    }

    public static Consumer<StepResult> stepPassed() {
        return stepResult -> stepResult.setStatus(Status.PASSED);
    }

    public static Consumer<StepResult> stepFailed(final Throwable throwable) {
        return stepResult -> stepResult
                .setStatus(getStatus(throwable).orElse(Status.BROKEN))
                .setStatusMessage(throwable.getMessage())
                .setStatusTrace(getStackTraceAsString(throwable));
    }

    public static Consumer<TestResult> dependsOn(final String uuid) {
        return testResult -> testResult.getChildren().add(uuid);
    }

    /**
     * Class for InheritableThreadLocal.
     */
    protected static class ExecutableStorage extends InheritableThreadLocal<Deque<Executable>> {

        @Override
        protected Deque<Executable> initialValue() {
            return new LinkedList<>();
        }

        @Override
        protected Deque<Executable> childValue(final Deque<Executable> parentValue) {
            final Deque<Executable> queue = new LinkedList<>();
            if (!parentValue.isEmpty()) {
                queue.add(parentValue.getFirst());
            }
            return queue;
        }
    }
}
