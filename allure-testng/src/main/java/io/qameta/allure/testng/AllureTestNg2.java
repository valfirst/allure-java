package io.qameta.allure.testng;

import io.qameta.allure.Lifecycle;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultType;
import io.qameta.allure.util.ResultsUtils;
import org.testng.IAttributes;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener2;
import org.testng.ISuite;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.qameta.allure.util.ResultsUtils.firstNonEmpty;
import static io.qameta.allure.util.ResultsUtils.getStatus;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureTestNg2 implements IInvokedMethodListener2 {

    private static final String ALLURE_UUID = "ALLURE_UUID";

    private final Map<ISuite, Set<String>> suiteConfigurations = new ConcurrentHashMap<>();
    private final Map<ISuite, Set<String>> suiteTests = new ConcurrentHashMap<>();

    private final Map<ISuite, Set<String>> classConfigurations = new ConcurrentHashMap<>();
    private final Map<ISuite, Set<String>> classTests = new ConcurrentHashMap<>();

    private final Map<ITestContext, String> testConfigurations = new ConcurrentHashMap<>();
    private final Map<String, String> groupConfigurations = new ConcurrentHashMap<>();
    private final Map<String, String> methodConfigurations = new ConcurrentHashMap<>();

    private final Lifecycle lifecycle;

    public AllureTestNg2(final Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void beforeInvocation(final IInvokedMethod method,
                                 final ITestResult testResult, final ITestContext context) {
        final ITestNGMethod testMethod = method.getTestMethod();
        final TestResult result = getResult(testMethod);
        if (testMethod.isBeforeSuiteConfiguration() || testMethod.isAfterSuiteConfiguration()) {
            suiteConfigurations.put(context.getSuite(), result);
        }
        if (testMethod.isBeforeTestConfiguration() || testMethod.isAfterTestConfiguration()) {
            testConfigurations.put(context, result);
        }
        if (testMethod.isBeforeClassConfiguration() || testMethod.isAfterClassConfiguration()) {
            classConfigurations.put(testMethod.getTestClass(), result);
        }
        if (testMethod.isBeforeGroupsConfiguration() || testMethod.isAfterGroupsConfiguration()) {
            Arrays.stream(testMethod.getGroups())
                    .forEach(group -> groupConfigurations.put(group, result));
        }

        if (testMethod.isTest()) {
            Arrays.stream(testMethod.getGroups());
        }
        lifecycle.startTest(result);
    }

    @Override
    public void beforeInvocation(final IInvokedMethod method, final ITestResult testResult) {
        //do nothing
    }

    @Override
    public void afterInvocation(final IInvokedMethod method, final ITestResult testResult,
                                final ITestContext context) {
        lifecycle.updateTest(r -> r.setStage(Stage.FINISHED));
        if (testResult.isSuccess()) {
            lifecycle.updateTest(r -> r.setStatus(Status.PASSED));
        } else {
            lifecycle.updateTest(r -> r.setStatus(getStatus(testResult.getThrowable()).orElse(Status.BROKEN)));
        }
        Optional.ofNullable(testResult.getThrowable()).ifPresent(throwable -> lifecycle.updateTest(r -> {
            r.setStatusMessage(throwable.getMessage());
            r.setStatusTrace(ResultsUtils.getStackTraceAsString(throwable));
        }));
        lifecycle.currentTest().ifPresent(current -> {
            lifecycle.stopTest();
            lifecycle.writeTest(current.getUuid());
        });
    }

    @Override
    public void afterInvocation(final IInvokedMethod method, final ITestResult testResult) {
        //do nothing
    }

    private TestResult getResult(final ITestNGMethod method) {
        final TestResult result = new TestResult()
                .setUuid(UUID.randomUUID().toString())
                .setName(getMethodName(method))
                .setDescription(method.getDescription());
        if (method.isTest()) {
            result.setType(TestResultType.TEST);
        }
        if (isSetUpConfiguration(method)) {
            result.setType(TestResultType.SET_UP);
        }
        if (isTearDownConfiguration(method)) {
            result.setType(TestResultType.TEAR_DOWN);
        }

        return result;
    }

    private boolean isTearDownConfiguration(final ITestNGMethod method) {
        return method.isAfterSuiteConfiguration()
                || method.isAfterTestConfiguration()
                || method.isAfterClassConfiguration()
                || method.isAfterGroupsConfiguration()
                || method.isAfterMethodConfiguration();
    }

    private boolean isSetUpConfiguration(final ITestNGMethod method) {
        return method.isBeforeSuiteConfiguration()
                || method.isBeforeTestConfiguration()
                || method.isBeforeClassConfiguration()
                || method.isBeforeGroupsConfiguration()
                || method.isBeforeMethodConfiguration();
    }

    private String getMethodName(final ITestNGMethod method) {
        return firstNonEmpty(
                method.getDescription(),
                method.getMethodName(),
                getQualifiedName(method)).orElse("Unknown");
    }

    private String getQualifiedName(final ITestNGMethod method) {
        return method.getRealClass().getName() + "." + method.getMethodName();
    }

    private String getUniqueUuid(final IAttributes suite) {
        if (Objects.isNull(suite.getAttribute(ALLURE_UUID))) {
            suite.setAttribute(ALLURE_UUID, UUID.randomUUID().toString());
        }
        return Objects.toString(suite.getAttribute(ALLURE_UUID));
    }
}
