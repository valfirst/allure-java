package io.qameta.allure.junit5;

import io.qameta.allure.Allure;
import io.qameta.allure.Lifecycle;
import io.qameta.allure.Description;
import io.qameta.allure.model3.Label;
import io.qameta.allure.model3.Stage;
import io.qameta.allure.model3.Status;
import io.qameta.allure.model3.TestResult;
import io.qameta.allure.util.ResultsUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.qameta.allure.model3.Status.FAILED;
import static io.qameta.allure.model3.Status.PASSED;
import static io.qameta.allure.model3.Status.SKIPPED;
import static java.nio.charset.StandardCharsets.UTF_8;

import static io.qameta.allure.util.ResultsUtils.getStackTraceAsString;

/**
 * @author ehborisov
 */
public class AllureJunit5 implements TestExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureJunit5.class);

    private static final String TAG = "tag";


    private final ThreadLocal<String> tests
            = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    private final Lifecycle lifecycle;

    public AllureJunit5(final Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public AllureJunit5() {
        this.lifecycle = Allure.getLifecycle();
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public void executionStarted(final TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            final Optional<MethodSource> methodSource = testIdentifier.getSource()
                    .filter(MethodSource.class::isInstance)
                    .map(MethodSource.class::cast);
            final String uuid = tests.get();
            final TestResult result = new TestResult()
                    .setUuid(uuid)
                    .setName(testIdentifier.getDisplayName())
                    .setLabels(getTags(testIdentifier))
                    .setHistoryId(getHistoryId(testIdentifier))
                    .setStage(Stage.RUNNING);

            methodSource.ifPresent(source -> {
                result.setDescription(getDescription(source));
                result.getLabels().add(new Label().setName("suite").setValue(getSuite(source)));
                result.getLabels().add(new Label().setName("package").setValue(source.getClassName()));
            });
            getLifecycle().startTest(result);
        }
    }

    @Override
    public void executionFinished(final TestIdentifier testIdentifier, final TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest()) {
            final String uuid = tests.get();
            tests.remove();
            getLifecycle().updateTest(result -> {
                result.setStage(Stage.FINISHED);
                switch (testExecutionResult.getStatus()) {
                    case FAILED:
                        testExecutionResult.getThrowable().ifPresent(throwable -> {
                            result.setStatus(getStatus(throwable));
                            result.setStatusMessage(throwable.getMessage());
                            result.setStatusTrace(getStackTraceAsString(throwable));
                        });
                        break;
                    case SUCCESSFUL:
                        result.setStatus(PASSED);
                        break;
                    default:
                        result.setStatus(SKIPPED);
                        testExecutionResult.getThrowable().ifPresent(throwable -> {
                            result.setStatusMessage(throwable.getMessage());
                            result.setStatusTrace(getStackTraceAsString(throwable));
                        });
                        break;
                }
            });
            getLifecycle().stopTest();
            getLifecycle().writeTest(uuid);
        }
    }

    protected Status getStatus(final Throwable throwable) {
        return ResultsUtils.getStatus(throwable).orElse(FAILED);
    }

    private Set<Label> getTags(final TestIdentifier testIdentifier) {
        return testIdentifier.getTags().stream()
                .map(tag -> new Label().setName(TAG).setValue(tag.getName()))
                .collect(Collectors.toSet());
    }

    protected String getHistoryId(final TestIdentifier testIdentifier) {
        return md5(testIdentifier.getUniqueId());
    }

    private String md5(final String source) {
        final byte[] bytes = getMessageDigest().digest(source.getBytes(UTF_8));
        return new BigInteger(1, bytes).toString(16);
    }

    private MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find md5 hashing algorithm", e);
        }
    }

    private String getSuite(final MethodSource source) {
        try {
            final DisplayName displayNameAnnotation =
                    Class.forName(source.getClassName()).getAnnotation(DisplayName.class);
            if (displayNameAnnotation != null && !displayNameAnnotation.value().isEmpty()) {
                return displayNameAnnotation.value();
            }
        } catch (ClassNotFoundException e) {
            LOGGER.trace(e.getMessage(), e);
        }
        return source.getClassName();
    }

    private String getDescription(final MethodSource source) {
        try {
            final Description descriptionAnnotation = Class.forName(source.getClassName())
                    .getDeclaredMethod(source.getMethodName())
                    .getAnnotation(Description.class);
            if (descriptionAnnotation != null) {
                return descriptionAnnotation.value();
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            LOGGER.trace(e.getMessage(), e);
        }
        return null;
    }
}
