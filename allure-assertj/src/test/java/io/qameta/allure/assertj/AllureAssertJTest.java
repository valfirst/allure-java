package io.qameta.allure.assertj;

import io.qameta.allure.Lifecycle;
import io.qameta.allure.model3.StepResult;
import io.qameta.allure.model3.TestResult;
import io.qameta.allure.test.InMemoryResultsWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
class AllureAssertJTest {

    private InMemoryResultsWriter results;

    private Lifecycle lifecycle;

    @BeforeEach
    public void initLifecycle() {
        results = new InMemoryResultsWriter();
        lifecycle = new Lifecycle(results);
        AllureAssertJ.setLifecycle(lifecycle);
    }

    @Test
    public void shouldCreateStepsForAsserts() throws Exception {
        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().setUuid(uuid);

        lifecycle.startTest(result);

        assertThat("Data")
                .hasSize(4);

        lifecycle.stopTest();
        lifecycle.writeTest(uuid);

        assertThat(results.getAllTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("assertThat 'Data'", "hasSize '4'");
    }
    
    @Test
    public void shouldHandleNullableObject() {
        assertThat((Object) null).as("Nullable object").isNull();
    }
}