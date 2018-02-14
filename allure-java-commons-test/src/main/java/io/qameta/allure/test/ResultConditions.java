package io.qameta.allure.test;

import io.qameta.allure.model3.Executable;
import io.qameta.allure.model3.Stage;
import io.qameta.allure.model3.TestResult;
import org.assertj.core.api.Condition;

import java.util.List;

/**
 * @author charlie (Dmitry Baev).
 */
public final class ResultConditions {

    public static final Condition<List<? extends TestResult>> ALL_FINISHED = new Condition<>(items ->
            items.stream().allMatch(item -> item.getStage() == Stage.FINISHED),
            "All items should have be in a finished stage");

    private ResultConditions() {
        throw new IllegalStateException("Do not instance");
    }

    public static Condition<List<? extends Executable>> hasStepsCount(final long count) {
        return new Condition<>(
            executables -> executables.stream().allMatch(executable -> executable.getSteps().size() == 1),
                String.format("All items should have %d steps", count)
        );
    }
}
