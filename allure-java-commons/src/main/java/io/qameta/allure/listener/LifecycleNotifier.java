package io.qameta.allure.listener;

import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;

import java.util.List;

/**
 * @since 2.0
 */
@SuppressWarnings("PMD.TooManyMethods")
public class LifecycleNotifier implements TestLifecycleListener, StepLifecycleListener {

    private final List<TestLifecycleListener> testListeners;

    private final List<StepLifecycleListener> stepListeners;

    public LifecycleNotifier(final List<TestLifecycleListener> testListeners,
                             final List<StepLifecycleListener> stepListeners) {
        this.testListeners = testListeners;
        this.stepListeners = stepListeners;
    }

    @Override
    public void beforeTestSchedule(final TestResult result) {
        testListeners.forEach(listener -> listener.beforeTestSchedule(result));
    }

    @Override
    public void afterTestSchedule(final TestResult result) {
        testListeners.forEach(listener -> listener.afterTestSchedule(result));
    }

    @Override
    public void beforeTestUpdate(final TestResult result) {
        testListeners.forEach(listener -> listener.beforeTestUpdate(result));
    }

    @Override
    public void afterTestUpdate(final TestResult result) {
        testListeners.forEach(listener -> listener.afterTestUpdate(result));
    }

    @Override
    public void beforeTestStart(final TestResult result) {
        testListeners.forEach(listener -> listener.beforeTestStart(result));
    }

    @Override
    public void afterTestStart(final TestResult result) {
        testListeners.forEach(listener -> listener.afterTestStart(result));
    }

    @Override
    public void beforeTestStop(final TestResult result) {
        testListeners.forEach(listener -> listener.beforeTestStop(result));
    }

    @Override
    public void afterTestStop(final TestResult result) {
        testListeners.forEach(listener -> listener.afterTestStop(result));
    }

    @Override
    public void beforeTestWrite(final TestResult result) {
        testListeners.forEach(listener -> listener.beforeTestWrite(result));
    }

    @Override
    public void afterTestWrite(final TestResult result) {
        testListeners.forEach(listener -> listener.afterTestWrite(result));
    }

    @Override
    public void beforeStepStart(final StepResult result) {
        stepListeners.forEach(listener -> listener.beforeStepStart(result));
    }

    @Override
    public void afterStepStart(final StepResult result) {
        stepListeners.forEach(listener -> listener.afterStepStart(result));
    }

    @Override
    public void beforeStepUpdate(final StepResult result) {
        stepListeners.forEach(listener -> listener.beforeStepUpdate(result));
    }

    @Override
    public void afterStepUpdate(final StepResult result) {
        stepListeners.forEach(listener -> listener.afterStepUpdate(result));
    }

    @Override
    public void beforeStepStop(final StepResult result) {
        stepListeners.forEach(listener -> listener.beforeStepStop(result));
    }

    @Override
    public void afterStepStop(final StepResult result) {
        stepListeners.forEach(listener -> listener.afterStepStop(result));
    }
}
