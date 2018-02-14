package io.qameta.allure;

import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import static io.qameta.allure.Lifecycle.dependsOn;
import static io.qameta.allure.testdata.TestData.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


import io.qameta.allure.test.InMemoryResultsWriter;

import java.util.List;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureLifecycleTest {

    private InMemoryResultsWriter writer;
    private Lifecycle lifecycle;

    @Before
    public void setUp() throws Exception {
        writer = Mockito.mock(InMemoryResultsWriter.class);
        lifecycle = new Lifecycle(writer);
    }

    @Test
    public void shouldCreateTest() throws Exception {
        final String uuid = randomString();
        final String name = randomString();
        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setName(name)
                .setType(TestResultType.TEST);
        lifecycle.startTest(result);
        lifecycle.stopTest();
        lifecycle.writeTest(uuid);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).writeResult(captor.capture());

        assertThat(captor.getValue())
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", name);
    }

    @Test
    public void shouldAddStepsToTests() throws Exception {
        final String uuid = randomString();
        final String name = randomString();
        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setName(name)
                .setType(TestResultType.TEST);
        lifecycle.startTest(result);

        final String firstStepName = randomStep();
        final String secondStepName = randomStep();

        lifecycle.stopTest();
        lifecycle.writeTest(uuid);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).writeResult(captor.capture());

        final TestResult actual = captor.getValue();
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", name);

        assertThat(actual.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly(firstStepName, secondStepName);
    }

    @Test
    public void shouldUpdateTest() throws Exception {
        final String uuid = randomString();
        final String name = randomString();

        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setName(name)
                .setType(TestResultType.TEST);
        lifecycle.startTest(result);

        final String stepName = randomString();

        final StepResult step = new StepResult().setName(stepName);
        lifecycle.startStep(step);

        final String description = randomString();
        final String fullName = randomString();

        lifecycle.updateTest(uuid, testResult -> testResult.setDescription(description));
        lifecycle.updateTest(testResult -> testResult.setFullName(fullName));

        lifecycle.stopStep();

        lifecycle.stopTest();
        lifecycle.writeTest(uuid);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).writeResult(captor.capture());

        final TestResult actual = captor.getValue();
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("description", description)
                .hasFieldOrPropertyWithValue("name", name)
                .hasFieldOrPropertyWithValue("fullName", fullName);

        assertThat(actual.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly(stepName);
    }

    @Test
    public void shouldCreateTestFixture() throws Exception {
        final String setUpUuid = randomString();
        final String setUpName = randomString();

        final TestResult setUp = new TestResult()
                .setUuid(setUpUuid)
                .setName(setUpName)
                .setType(TestResultType.SET_UP);

        lifecycle.startTest(setUp);

        final String setUpStepName = randomStep();
        final String setUpSecondStepName = randomStep();

        lifecycle.stopTest();

        final String testUuid = randomString();
        final String testName = randomString();

        final TestResult test = new TestResult()
                .setUuid(testUuid)
                .setName(testName)
                .setType(TestResultType.TEST);

        lifecycle.startTest(test);

        final String testStepName = randomStep();
        final String testSecondStepName = randomStep();

        lifecycle.stopTest();
        lifecycle.writeTest(testUuid);

        final String tearDownUuid = randomString();
        final String tearDownName = randomString();

        final TestResult tearDown = new TestResult()
                .setUuid(tearDownUuid)
                .setName(tearDownName)
                .setType(TestResultType.TEAR_DOWN);

        lifecycle.startTest(tearDown);

        final String tearDownStepName = randomStep();
        final String tearDownSecondStepName = randomStep();

        lifecycle.updateTest(dependsOn(testUuid));
        lifecycle.stopTest();
        lifecycle.writeTest(tearDownUuid);

        lifecycle.updateTest(setUpUuid, dependsOn(testUuid));
        lifecycle.writeTest(setUpUuid);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(3)).writeResult(captor.capture());

        final List<TestResult> results = captor.getAllValues();
        assertThat(results)
                .hasSize(3);

        TestResult setUpResult = results.stream()
                .filter(result -> TestResultType.SET_UP.equals(result.getType())).findFirst().get();
        assertThat(setUpResult)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", setUpName);
        assertThat(setUpResult.getChildren()).containsOnly(testUuid);
        assertThat(setUpResult.getSteps())
                .hasSize(2)
                .flatExtracting(StepResult::getName)
                .containsExactly(setUpStepName, setUpSecondStepName);

        TestResult testResult = results.stream()
                .filter(result -> TestResultType.TEST.equals(result.getType())).findFirst().get();
        assertThat(testResult)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", testName);
        assertThat(testResult.getSteps())
                .hasSize(2)
                .flatExtracting(StepResult::getName)
                .containsExactly(testStepName, testSecondStepName);

        TestResult tearDownResult = results.stream()
                .filter(result -> TestResultType.TEAR_DOWN.equals(result.getType())).findFirst().get();
        assertThat(tearDownResult)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", tearDownName);
        assertThat(tearDownResult.getChildren()).containsOnly(testUuid);
        assertThat(tearDownResult.getSteps())
                .hasSize(2)
                .flatExtracting(StepResult::getName)
                .containsExactly(tearDownStepName, tearDownSecondStepName);
    }

    private String randomStep() {
        final String name = randomString();
        final StepResult step = new StepResult().setName(name);
        lifecycle.startStep(step);
        lifecycle.stopStep();
        return name;
    }
}