package io.qameta.allure.junit5;

import io.qameta.allure.Lifecycle;
import io.qameta.allure.aspect.AttachmentsAspects;
import io.qameta.allure.aspect.StepsAspects;
import io.qameta.allure.junit5.features.*;
import io.qameta.allure.model.*;
import io.qameta.allure.test.InMemoryResultsWriter;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static io.qameta.allure.junit5.features.TaggedTests.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureJunit5Test {

    private InMemoryResultsWriter results;

    private Lifecycle lifecycle;

    @BeforeEach
    void setUp() {
        this.results = new InMemoryResultsWriter();
        this.lifecycle = new Lifecycle(results);
        StepsAspects.setLifecycle(lifecycle);
        AttachmentsAspects.setLifecycle(lifecycle);
    }

    @Test
    void shouldProcessPassedTests() {
        runClasses(PassedTests.class);

        final List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(3)
                .filteredOn(testResult -> Status.PASSED.equals(testResult.getStatus()))
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder("first()", "second()", "third()");
    }

    @Test
    void shouldProcessFailedTests() {
        runClasses(FailedTests.class);

        final List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .hasFieldOrPropertyWithValue("name", "failedTest()")
                .hasFieldOrPropertyWithValue("status", Status.FAILED);

        assertThat(testResults)
                .extracting(TestResult::getStatusMessage)
                .containsExactly("Make the test failed");
    }

    @Test
    void shouldProcessBrokenTests() {
        runClasses(BrokenTests.class);

        final List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "brokenTest()")
                .hasFieldOrPropertyWithValue("status", Status.BROKEN);

        assertThat(testResults)
                .extracting(TestResult::getStatusMessage)
                .containsExactly("Make the test broken");
    }

    @Test
    void shouldProcessSkippedTests() {
        runClasses(SkippedTests.class);

        final List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "skippedTest()")
                .hasFieldOrPropertyWithValue("status", Status.SKIPPED);

        assertThat(testResults)
                .extracting(TestResult::getStatusMessage)
                .containsExactly("Assumption failed: Make the test skipped");
    }

    @Test
    void shouldProcessDisplayName() {
        runClasses(TestsWithDisplayName.class);

        final List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .hasFieldOrPropertyWithValue("name", "Some test with changed name");
    }

    @Test
    void shouldSetStartAndStopTimes() {
        runClasses(FailedTests.class);

        final List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .hasFieldOrProperty("start")
                .hasFieldOrProperty("stop");
    }

    @Test
    void shouldSetFinishedStage() {
        runClasses(FailedTests.class);

        final List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED);
    }

    @Test
    void shouldProcessDynamicTests() {
        runClasses(DynamicTests.class);

        final List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(3)
                .filteredOn(testResult -> Status.PASSED.equals(testResult.getStatus()))
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder("testA", "testB", "testC");
    }

    @Test
    void shouldProcessParametrisedTests() {
        runClasses(ParameterisedTests.class);

        final List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(2)
                .filteredOn(testResult -> Status.PASSED.equals(testResult.getStatus()))
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder("[1] Hello", "[2] World");
    }

    @Test
    void shouldAddSteps() {
        runClasses(TestsWithSteps.class);

        final List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(1);

        final TestResult testResult = testResults.get(0);
        assertThat(testResult.getSteps())
                .hasSize(3)
                .flatExtracting(StepResult::getName)
                .containsExactly("first", "second", "third");

    }

    @Test
    void shouldAddTags() {
        runClasses(TaggedTests.class);

        final List<TestResult> testResults = results.getAllTestResults();

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(testResults)
                .hasSize(1);

        softly.assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "tag".equals(label.getName()))
                .flatExtracting(Label::getValue)
                .containsExactlyInAnyOrder(CLASS_TAG, METHOD_TAG);

        softly.assertAll();
    }

    @Test
    void shouldProcessTestClassDisplayNameByAnnotation() {
        runClasses(TestsClassWithDisplayNameAnnotation.class);

        final List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(1);

        final Set<Label> testResultLabels = testResults.get(0).getLabels();
        assertThat(testResultLabels)
                .filteredOn(label -> "suite".equals(label.getName()))
                .hasSize(1)
                .flatExtracting(Label::getValue)
                .contains("Display name of test class");
    }

    @Test
    void shouldProcessDefaultTestClassDisplayName() {
        runClasses(TestsClassWithoutDisplayNameAnnotation.class);

        final List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(1);

        final Set<Label> testResultLabels = testResults.get(0).getLabels();
        assertThat(testResultLabels)
                .filteredOn(label -> "suite".equals(label.getName()))
                .hasSize(1)
                .flatExtracting(Label::getValue)
                .contains("io.qameta.allure.junit5.features.TestsClassWithoutDisplayNameAnnotation");
    }

    @Test
    void shouldProcessJunit5Description() {
        runClasses(TestsWithDescriptions.class);

        final List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getDescription)
                .contains("Test description");
    }

    private void runClasses(Class<?>... classes) {
        final ClassSelector[] classSelectors = Stream.of(classes)
                .map(DiscoverySelectors::selectClass)
                .toArray(ClassSelector[]::new);
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(classSelectors)
                .build();

        final Launcher launcher = LauncherFactory.create();
        launcher.execute(request, new AllureJunit5(lifecycle));
    }
}
