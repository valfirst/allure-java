package io.qameta.allure.aspect;

import io.qameta.allure.Lifecycle;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.InMemoryResultsWriter;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class Allure1StepsAspectsTest {

    private InMemoryResultsWriter results;

    private Lifecycle lifecycle;

    @Before
    public void initLifecycle() {
        results = new InMemoryResultsWriter();
        lifecycle = new Lifecycle(results);
        Allure1StepsAspects.setLifecycle(lifecycle);
    }

    @Test
    public void shouldSetupStepTitleFromAnnotation() {
        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().setUuid(uuid);

        lifecycle.startTest(result);

        stepWithTitleAndWithParameter("parameter value");

        lifecycle.stopTest();
        lifecycle.writeTest(uuid);

        assertThat(results.getAllTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("step with title and parameter [parameter value]");

        assertThat(results.getAllTestResults())
                .flatExtracting(TestResult::getSteps)
                .flatExtracting(StepResult::getParameters)
                .extracting("name", "value")
                .containsExactly(tuple("parameter", "parameter value"));
    }

    @Test
    public void shouldSetupStepTitleFromMethodSignature() {
        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().setUuid(uuid);

        lifecycle.startTest(result);

        stepWithoutTitleAndWithParameter("parameter value");

        lifecycle.stopTest();
        lifecycle.writeTest(uuid);

        assertThat(results.getAllTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("stepWithoutTitleAndWithParameter[parameter value]");

        assertThat(results.getAllTestResults())
                .flatExtracting(TestResult::getSteps)
                .flatExtracting(StepResult::getParameters)
                .extracting("name", "value")
                .containsExactly(tuple("parameter", "parameter value"));
    }

    @Step
    public void stepWithoutTitleAndWithParameter(String parameter) {

    }

    @Step("step with title and parameter [{0}]")
    public void stepWithTitleAndWithParameter(String parameter) {
    }

}
