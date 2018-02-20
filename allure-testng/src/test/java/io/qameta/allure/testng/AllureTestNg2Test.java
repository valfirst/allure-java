package io.qameta.allure.testng;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Lifecycle;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.qameta.allure.aspect.AttachmentsAspects;
import io.qameta.allure.aspect.StepsAspects;
import io.qameta.allure.model3.Attachment;
import io.qameta.allure.model3.Label;
import io.qameta.allure.model3.Link;
import io.qameta.allure.model3.Parameter;
import io.qameta.allure.model3.Stage;
import io.qameta.allure.model3.Status;
import io.qameta.allure.model3.StepResult;
import io.qameta.allure.model3.TestResult;
import io.qameta.allure.test.InMemoryResultsWriter;
import org.assertj.core.api.Condition;
import org.assertj.core.groups.Tuple;
import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
@Epic("TestNG integration")
@Test(description = "Allure TestNG tests")
public class AllureTestNg2Test {

    private TestNG testNg;
    private InMemoryResultsWriter results;
    private Lifecycle lifecycle;

    @BeforeMethod(description = "Configure TestNG engine")
    public void prepare() {
        results = new InMemoryResultsWriter();
        lifecycle = new Lifecycle(results);
        AllureTestNg2 adapter = new AllureTestNg2(lifecycle);
        testNg = new TestNG(false);
        testNg.addListener((ITestNGListener) adapter);

        StepsAspects.setLifecycle(lifecycle);
        AttachmentsAspects.setLifecycle(lifecycle);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        StepsAspects.setLifecycle(Allure.getLifecycle());
        AttachmentsAspects.setLifecycle(Allure.getLifecycle());
    }

    @Feature("Support for parallel test execution")
    @Test(description = "Parallel data provider tests")
    public void parallelDataProvider() {
        runTestNgSuites("suites/parallel-data-provider.xml");
        List<TestResult> testResult = results.getAllTestResults();
        assertThat(testResult).as("Not all testng case results have been written").hasSize(2000);
    }

    @Feature("Basic framework support")
    @Test(description = "Before suites fixtures tests")
    public void beforeSuiteFixtures() {
        final String testOneName = "testOne";
        final String testTwoName = "testTwo";
        final String before1 = "beforeSuiteOne";
        final String before2 = "beforeSuiteTwo";

        runTestNgSuites("suites/fixtures-test.xml");
        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Test case result has not been written").hasSize(2);
        assertBeforeFixtures(before1, before2);

        List<TestResult> setUpResults = results.getAllSetUpResults();
        setUpResults.stream().filter(result -> before1.equals(result.getName()) || before2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(result.getChildren(), testOneName, testTwoName));
    }

    @Feature("Basic framework support")
    @Test(description = "Before test fixtures tests")
    public void beforeTestFixtures() {
        final String testOneName = "testOne";
        final String testTwoName = "testTwo";
        final String before1 = "beforeTestOne";
        final String before2 = "beforeTestTwo";

        runTestNgSuites("suites/fixtures-test.xml");
        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Test case result has not been written").hasSize(2);
        assertBeforeFixtures(before1, before2);

        List<TestResult> setUpResults = results.getAllSetUpResults();
        setUpResults.stream().filter(result -> before1.equals(result.getName()) || before2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(result.getChildren(), testOneName, testTwoName));
    }

    @Feature("Basic framework support")
    @Test(description = "Before class fixtures tests")
    public void beforeClassFixtures() {
        final String testOneName = "testOne";
        final String testTwoName = "testTwo";
        final String before1 = "beforeClassOne";
        final String before2 = "beforeClassTwo";

        runTestNgSuites("suites/fixtures-test.xml");
        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Test case result has not been written").hasSize(2);
        assertBeforeFixtures(before1, before2);

        List<TestResult> setUpResults = results.getAllSetUpResults();
        setUpResults.stream().filter(result -> before1.equals(result.getName()) || before2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(result.getChildren(), testOneName, testTwoName));
    }

    @Feature("Basic framework support")
    @Test(description = "Before group fixtures tests")
    public void beforeGroupFixtures() {
        final String testOneName = "testOne";
        final String testTwoName = "testTwo";
        final String before1 = "beforeGroupOne";
        final String before2 = "beforeGroupTwo";

        runTestNgSuites("suites/fixtures-test.xml");
        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Test case result has not been written").hasSize(2);
        assertBeforeFixtures(before1, before2);

        List<TestResult> setUpResults = results.getAllSetUpResults();
        setUpResults.stream().filter(result -> before1.equals(result.getName()) || before2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(result.getChildren(), testOneName, testTwoName));
    }

    @Feature("Basic framework support")
    @Test(description = "Before method fixtures tests")
    public void beforeMethodFixtures() {
        final String before1 = "beforeMethodOne";
        final String before2 = "beforeMethodTwo";

        runTestNgSuites("suites/fixtures-test.xml");
        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Test case result has not been written").hasSize(2);
        assertBeforeFixtures(before1, before2);

        Set<String> uuids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toSet());
        assertContainersPerMethod(before1, results.getAllSetUpResults(), uuids);
        assertContainersPerMethod(before2, results.getAllSetUpResults(), uuids);
    }

    @Feature("Basic framework support")
    @Test(description = "After suites fixtures tests")
    public void afterSuiteFixtures() {
        final String testOneName = "testOne";
        final String testTwoName = "testTwo";
        final String after1 = "afterSuiteOne";
        final String after2 = "afterSuiteTwo";

        runTestNgSuites("suites/fixtures-test.xml");
        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Test case result has not been written").hasSize(2);
        assertAfterFixtures(after1, after2);

        List<TestResult> tearDownResults = results.getAllTearDownResults();
        tearDownResults.stream().filter(result -> after1.equals(result.getName()) || after2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(result.getChildren(), testOneName, testTwoName));
    }

    @Feature("Basic framework support")
    @Test(description = "After test fixtures tests")
    public void afterTestFixtures() {
        final String testOneName = "testOne";
        final String testTwoName = "testTwo";
        final String after1 = "afterTestOne";
        final String after2 = "afterTestTwo";

        runTestNgSuites("suites/fixtures-test.xml");
        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Test case result has not been written").hasSize(2);
        assertAfterFixtures(after1, after2);

        List<TestResult> tearDownResults = results.getAllTearDownResults();
        tearDownResults.stream().filter(result -> after1.equals(result.getName()) || after2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(result.getChildren(), testOneName, testTwoName));
    }

    @Feature("Basic framework support")
    @Test(description = "After class fixtures tests")
    public void afterClassFixtures() {
        final String testOneName = "testOne";
        final String testTwoName = "testTwo";
        final String after1 = "afterClassOne";
        final String after2 = "afterClassTwo";

        runTestNgSuites("suites/fixtures-test.xml");
        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Test case result has not been written").hasSize(2);
        assertAfterFixtures(after1, after2);

        List<TestResult> tearDownResults = results.getAllTearDownResults();
        tearDownResults.stream().filter(result -> after1.equals(result.getName()) || after2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(result.getChildren(), testOneName, testTwoName));
    }

    @Feature("Basic framework support")
    @Test(description = "After group fixtures tests")
    public void afterGroupFixtures() {
        final String testOneName = "testOne";
        final String testTwoName = "testTwo";
        final String after1 = "afterGroupOne";
        final String after2 = "afterGroupTwo";

        runTestNgSuites("suites/fixtures-test.xml");
        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Test case result has not been written").hasSize(2);
        assertAfterFixtures(after1, after2);

        List<TestResult> tearDownResults = results.getAllTearDownResults();
        tearDownResults.stream().filter(result -> after1.equals(result.getName()) || after2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(result.getChildren(), testOneName, testTwoName));
    }

    @Feature("Basic framework support")
    @Test(description = "After method fixtures tests")
    public void afterMethodFixtures() {
        final String after1 = "afterMethodOne";
        final String after2 = "afterMethodTwo";

        runTestNgSuites("suites/fixtures-test.xml");
        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Test case result has not been written").hasSize(2);
        assertAfterFixtures(after1, after2);

        Set<String> uuids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toSet());
        assertContainersPerMethod(after1, results.getAllTearDownResults(), uuids);
        assertContainersPerMethod(after2, results.getAllTearDownResults(), uuids);
    }

    @Feature("Basic framework support")
    @Test(description = "Singe testng")
    public void singleTest() {
        final String testName = "testWithOneStep";
        runTestNgSuites("suites/single-test.xml");
        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Test case result has not been written").hasSize(1);
        assertThat(testResults.get(0)).as("Unexpected passed testng property")
                .hasFieldOrPropertyWithValue("status", Status.PASSED)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED)
                .hasFieldOrPropertyWithValue("name", testName);
        assertThat(testResults)
                .flatExtracting(TestResult::getSteps)
                .hasSize(1)
                .flatExtracting(StepResult::getStatus)
                .contains(Status.PASSED);
    }

    @Feature("Descriptions")
    @Test(description = "Javadoc descriptions of tests")
    public void descriptionsTest() {
        final String testDescription = "Sample test description";
        runTestNgSuites("suites/descriptions-test.xml");
        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Test case result has not been written")
                .hasSize(2).first()
                .extracting(TestResult::getDescription)
                .as("Javadoc description of test case has not been processed")
                .contains(testDescription);
    }

    @Feature("Failed tests")
    @Story("Failed")
    @Test(description = "Test failing by assertion")
    public void failingByAssertion() {
        String testName = "failingByAssertion";
        runTestNgSuites("suites/failing-by-assertion.xml");
        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Test case result has not been written").hasSize(1);
        assertThat(testResults.get(0)).as("Unexpected failed testng property")
                .hasFieldOrPropertyWithValue("status", Status.FAILED)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED)
                .hasFieldOrPropertyWithValue("name", testName);
        assertThat(testResults)
                .flatExtracting(TestResult::getSteps)
                .hasSize(2)
                .flatExtracting(StepResult::getStatus)
                .contains(Status.PASSED, Status.FAILED);
    }

    @Feature("Failed tests")
    @Story("Broken")
    @Test(description = "Broken testng")
    public void brokenTest() {
        String testName = "brokenTest";
        runTestNgSuites("suites/broken.xml");
        List<TestResult> testResult = results.getAllTestResults();

        assertThat(testResult).as("Test case result has not been written").hasSize(1);
        assertThat(testResult.get(0)).as("Unexpected broken testng property")
                .hasFieldOrPropertyWithValue("status", Status.BROKEN)
                .hasFieldOrPropertyWithValue("stage", Stage.FINISHED)
                .hasFieldOrPropertyWithValue("name", testName);
        assertThat(testResult)
                .flatExtracting(TestResult::getSteps)
                .hasSize(2)
                .flatExtracting(StepResult::getStatus)
                .contains(Status.PASSED, Status.BROKEN);
    }

    @Feature("Test fixtures")
    @Story("Suite")
    @Test(description = "Suite fixtures")
    public void perSuiteFixtures() {
        String suiteName = "Test suite 12";
        String testTagName = "Test tag 12";
        String before1 = "beforeSuite1";
        String before2 = "beforeSuite2";
        String after1 = "afterSuite1";
        String after2 = "afterSuite2";

        runTestNgSuites("suites/per-suite-fixtures-combination.xml");

        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Unexpected quantity of testng case results has been written").hasSize(1);
        assertLabelContents(testResults, "suite", suiteName);
        assertLabelContents(testResults, "tag", testTagName);
        assertBeforeFixtures(before1, before2);
        assertAfterFixtures(after1, after2);
    }

    @Feature("Test fixtures")
    @Story("Class")
    @Test(description = "Class fixtures")
    public void perClassFixtures() {
        runTestNgSuites("suites/per-class-fixtures-combination.xml");

        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults)
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("test1", "test2");

        assertBeforeFixtures( "beforeClass");
        assertAfterFixtures( "afterClass");
    }

    @Feature("Test fixtures")
    @Story("Method")
    @Test(description = "Method fixtures")
    public void perMethodFixtures() {
        String suiteName = "Test suite 11";
        String testTagName = "Test tag 11";
        String before1 = "beforeMethod1";
        String before2 = "beforeMethod2";
        String after1 = "afterMethod1";
        String after2 = "afterMethod2";

        runTestNgSuites("suites/per-method-fixtures-combination.xml");

        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Unexpected quantity of testng case results has been written").hasSize(2);
        assertLabelContents(testResults, "suite", suiteName);
        assertLabelContents(testResults, "tag", testTagName);
        assertBeforeFixtures(before1, before2);
        assertAfterFixtures(after1, after2);

        Set<String> uuids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toSet());
        assertContainersPerMethod(before1, results.getAllSetUpResults(), uuids);
        assertContainersPerMethod(before2, results.getAllSetUpResults(), uuids);
        assertContainersPerMethod(after1, results.getAllTearDownResults(), uuids);
        assertContainersPerMethod(after2, results.getAllTearDownResults(), uuids);
    }

    @Feature("Test fixtures")
    @Story("Suite")
    @Story("Test")
    @Story("Class")
    @Story("Method")
    @Test(description = "Test fixtures")
    public void perTestTagFixtures() {
        String suiteName = "Test suite 13";
        String testTagName = "Test tag 13";
        String before1 = "beforeTest1";
        String before2 = "beforeTest2";
        String after1 = "afterTest1";
        String after2 = "afterTest2";

        runTestNgSuites("suites/per-test-tag-fixtures-combination.xml");

        List<TestResult> testResult = results.getAllTestResults();

        assertThat(testResult).as("Unexpected quantity of testng case results has been written").hasSize(1);
        List<String> testName = singletonList(testResult.get(0).getName());

        List<TestResult> setUpResultsForSuiteAndTag = results.getAllSetUpResults().stream().filter(
                result -> result.getLabels().stream().anyMatch(
                        label -> label.getName().equals("tag") && label.getValue().equals(testTagName)
                ) && result.getLabels().stream().anyMatch(
                        label -> label.getName().equals("suite") && label.getValue().equals(suiteName)
                )
        ).collect(Collectors.toList());
        setUpResultsForSuiteAndTag.forEach(result -> assertChildrensFixtures(result.getChildren(), testName.toArray(new String[]{})));

        List<TestResult> tearDownResultsForSuiteAndTag = results.getAllTearDownResults().stream().filter(
            result -> result.getLabels().stream().anyMatch(
                label -> label.getName().equals("tag") && label.getValue().equals(testTagName)
            ) && result.getLabels().stream().anyMatch(
                    label -> label.getName().equals("suite") && label.getValue().equals(suiteName)
            )
        ).collect(Collectors.toList());
        tearDownResultsForSuiteAndTag.forEach(result -> assertChildrensFixtures(result.getChildren(), testName.toArray(new String[]{})));

        assertFixtures(setUpResultsForSuiteAndTag, before1, before2);
        assertFixtures(tearDownResultsForSuiteAndTag, after1, after2);
    }

    @Feature("Failed tests")
    @Story("Skipped")
    @Test(description = "Skipped suite")
    public void skippedSuiteTest() {
        final Condition<StepResult> skipReason = new Condition<>(step ->
                step.getStatusTrace().startsWith("java.lang.RuntimeException: Skip all"),
                "Suite should be skipped because of an exception in before suite");

        runTestNgSuites("suites/skipped-suite.xml");
        List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults).as("Unexpected quantity of testng case results has been written")
                .hasSize(2)
                .flatExtracting(TestResult::getStatus).contains(Status.SKIPPED, Status.SKIPPED);

        List<TestResult> setUpResultsForSuite = results.getAllSetUpResults().stream().filter(
                result -> result.getLabels().stream().anyMatch(
                        label -> label.getName().equals("suite") && label.getValue().equals("Test suite 8")
                )
        ).collect(Collectors.toList());

        assertThat(setUpResultsForSuite)
                .as("Unexpected quantity of before suite fixtures has been written")
                .hasSize(1)
                .flatExtracting(TestResult::getSteps)
                .as("Before suite container should have a before method with one step")
                .hasSize(1).first()
                .hasFieldOrPropertyWithValue("status", Status.BROKEN)
                .has(skipReason);
    }

    @Feature("Support for multi suites")
    @Test(description = "Multi suites")
    public void multipleSuites() {
        String firstSuiteName = "Test suite 6";
        String secondSuiteName = "Test suite 7";

        runTestNgSuites("suites/parameterized-test.xml", "suites/single-test.xml");

        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Unexpected quantity of testng case results has been written")
                .hasSize(3);
        assertLabelContents(testResults.stream().filter(result -> result.getName().equals("parameterizedTest")).collect(Collectors.toList()), "suite", firstSuiteName);
        assertLabelContents(testResults.stream().filter(result -> result.getName().equals("testWithOneStep")).collect(Collectors.toList()), "suite", secondSuiteName);

        List<TestResult> setUpResultsForSuiteFirst = results.getAllSetUpResults().stream().filter(
                result -> result.getLabels().stream().anyMatch(
                        label -> label.getName().equals("suite") && label.getValue().equals(firstSuiteName)
                )
        ).collect(Collectors.toList());
        List<String> testUuidsForSuiteFirst = results.getAllTestResults().stream().filter(
                result -> result.getLabels().stream().anyMatch(
                        label -> label.getName().equals("suite") && label.getValue().equals(firstSuiteName)
                )
        ).map(TestResult::getUuid).collect(Collectors.toList());
        assertThat(setUpResultsForSuiteFirst.stream().flatMap(results -> results.getChildren().stream()).collect(Collectors.toSet()))
                .hasSize(2)
                .containsExactlyInAnyOrder(testUuidsForSuiteFirst.toArray(new String[]{}));

        List<TestResult> setUpResultsForSuiteSecond = results.getAllSetUpResults().stream().filter(
                result -> result.getLabels().stream().anyMatch(
                        label -> label.getName().equals("suite") && label.getValue().equals(secondSuiteName)
                )
        ).collect(Collectors.toList());
        List<String> testUuidsForSuiteSecound = results.getAllTestResults().stream().filter(
                result -> result.getLabels().stream().anyMatch(
                        label -> label.getName().equals("suite") && label.getValue().equals(secondSuiteName)
                )
        ).map(TestResult::getUuid).collect(Collectors.toList());
        assertThat(setUpResultsForSuiteSecond.stream().flatMap(results -> results.getChildren().stream()).collect(Collectors.toSet()))
                .hasSize(1)
                .containsExactlyInAnyOrder(testUuidsForSuiteSecound.toArray(new String[]{}));
    }

    @Feature("Parameters")
    @Story("Suite parameter")
    @Test(description = "Before Suite Parameter")
    public void testBeforeSuiteParameter() {
        runTestNgSuites("suites/parameterized-suite1.xml", "suites/parameterized-suite2.xml");
        List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(2)
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getName, Parameter::getValue)
                .containsExactly(
                        tuple("param", "first"),
                        tuple("parameter", "first"),
                        tuple("param", "second"),
                        tuple("parameter", "second")
                );
    }

    @Feature("Support for parallel test execution")
    @Test(description = "Parallel methods")
    public void parallelMethods() {
        String before1 = "io.qameta.allure.testng.samples.ParallelMethods.beforeMethod";
        String before2 = "io.qameta.allure.testng.samples.ParallelMethods.beforeMethod2";
        String after = "io.qameta.allure.testng.samples.ParallelMethods.afterMethod";

        runTestNgSuites("suites/parallel-methods.xml");
        List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults).as("Unexpected quantity of testng case results has been written")
                .hasSize(2001);
        List<TestResult> setUpForBefore1 = results.getAllSetUpResults().stream().filter(result -> before1.equals(result.getFullName())).collect(Collectors.toList());
        assertThat(setUpForBefore1)
                .hasSize(2001);
        List<TestResult> setUpForBefore2 = results.getAllSetUpResults().stream().filter(result -> before2.equals(result.getFullName())).collect(Collectors.toList());
        assertThat(setUpForBefore2)
                .hasSize(2001);
        List<TestResult> tearDownForAfter = results.getAllTearDownResults().stream().filter(result -> after.equals(result.getFullName())).collect(Collectors.toList());
        assertThat(tearDownForAfter)
                .hasSize(2001);
    }

    @Feature("Basic framework support")
    @Test(description = "Nested steps")
    public void nestedSteps() {
        String beforeMethod = "io.qameta.allure.testng.samples.NestedSteps.beforeMethod";
        String nestedStep = "nestedStep";
        String stepInBefore = "stepTwo";
        String stepInTest = "stepThree";
        final Condition<StepResult> substep = new Condition<>(step ->
                step.getSteps().get(0).getName().equals(nestedStep),
                "Given step should have a substep with name " + nestedStep);

        runTestNgSuites("suites/nested-steps.xml");
        List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults).as("Unexpected quantity of testng case results has been written")
                .hasSize(1);

        assertThat(results.getAllSetUpResults())
                .filteredOn("fullName", beforeMethod)
                .flatExtracting(TestResult::getSteps)
                .as("Before method should have a step")
                .hasSize(1).first()
                .hasFieldOrPropertyWithValue("name", stepInBefore)
                .has(substep);

        assertThat(testResults)
                .flatExtracting(TestResult::getSteps)
                .hasSize(1).first()
                .hasFieldOrPropertyWithValue("name", stepInTest)
                .has(substep);
    }

    @Feature("Test markers")
    @Story("Flaky")
    @Test(description = "Flaky tests")
    public void flakyTests() throws Exception {
        runTestNgSuites("suites/flaky.xml");

        List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(9)
                .filteredOn(flakyPredicate())
                .extracting(TestResult::getFullName)
                .hasSize(7)
                .containsExactly(
                        "io.qameta.allure.testng.samples.FlakyMethods.flakyTest",
                        "io.qameta.allure.testng.samples.FlakyMethods.flakyTest",
                        "io.qameta.allure.testng.samples.FlakyTestClass.flakyAsWell",
                        "io.qameta.allure.testng.samples.FlakyTestClass.flakyTest",
                        "io.qameta.allure.testng.samples.FlakyTestClass.flakyAsWell",
                        "io.qameta.allure.testng.samples.FlakyTestClass.flakyTest",
                        "io.qameta.allure.testng.samples.FlakyTestClassInherited.flakyInherited"
                );
    }

    @Feature("Test markers")
    @Story("Muted")
    @Test(description = "Muted tests")
    public void mutedTests() throws Exception {
        runTestNgSuites("suites/muted.xml");

        List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(9)
                .filteredOn(mutedPredicate())
                .extracting(TestResult::getFullName)
                .hasSize(7)
                .containsExactly(
                        "io.qameta.allure.testng.samples.MutedMethods.mutedTest",
                        "io.qameta.allure.testng.samples.MutedMethods.mutedTest",
                        "io.qameta.allure.testng.samples.MutedTestClass.mutedAsWell",
                        "io.qameta.allure.testng.samples.MutedTestClass.mutedTest",
                        "io.qameta.allure.testng.samples.MutedTestClass.mutedAsWell",
                        "io.qameta.allure.testng.samples.MutedTestClass.mutedTest",
                        "io.qameta.allure.testng.samples.MutedTestClassInherited.mutedInherited"
                );
    }

    @Feature("Test markers")
    @Story("Links")
    @Test(description = "Tests with links")
    public void linksTest() throws Exception {
        runTestNgSuites("suites/links.xml");

        List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(4)
                .filteredOn(hasLinks())
                .hasSize(4)
                .flatExtracting(TestResult::getLinks)
                .extracting(Link::getName)
                .containsExactlyInAnyOrder("testClass", "a", "b", "c", "testClassIssue", "testClassTmsLink",
                        "testClass", "nested1", "nested2", "nested3", "testClassIssue", "issue1", "issue2", "issue3",
                        "testClassTmsLink", "tms1", "tms2", "tms3", "testClass", "a", "b", "c", "testClassIssue",
                        "testClassTmsLink", "testClass", "inheritedLink1", "inheritedLink2", "testClassIssue",
                        "inheritedIssue", "testClassTmsLink", "inheritedTmsLink"
                );
    }

    @Feature("Test markers")
    @Story("Bdd annotations")
    @Test(description = "BDD annotations")
    public void bddAnnotationsTest() throws Exception {
        runTestNgSuites("suites/bdd-annotations.xml");

        final List<String> bddLabels = Arrays.asList("epic", "feature", "story");
        List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(2)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> bddLabels.contains(label.getName()))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder(
                        "epic1",
                        "epic2",
                        "feature1",
                        "feature2",
                        "story1",
                        "story2",
                        "epic-inherited",
                        "class-feature1",
                        "class-feature2",
                        "story-inherited"
                );
    }

    @Feature("TestNG retries")
    @Test(description = "Should support TestNG retries")
    public void retryTest() throws Exception {
        runTestNgSuites("suites/retry.xml");
        List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(2);
    }

    @Feature("Test markers")
    @Story("Severity")
    @Test(description = "Should add severity for tests")
    public void severityTest() throws Exception {
        runTestNgSuites("suites/severity.xml");
        List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(8)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "severity".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder("critical", "critical", "minor", "blocker", "minor", "blocker");
    }

    @Feature("Test markers")
    @Story("Owner")
    @Test(description = "Should add owner to tests")
    public void ownerTest() throws Exception {
        runTestNgSuites("suites/owner.xml");
        List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(8)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "owner".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder("charlie", "charlie", "other-guy", "eroshenkoam", "other-guy", "eroshenkoam");
    }

    @Feature("Basic framework support")
    @Story("Attachments")
    @Test(description = "Should add attachments to tests")
    public void attachmentsTest() throws Exception {
        runTestNgSuites("suites/attachments.xml");
        List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(1)
                .flatExtracting(TestResult::getAttachments)
                .hasSize(1)
                .flatExtracting(Attachment::getName)
                .containsExactly("String attachment");
    }

    @Feature("Test markers")
    @Story("Flaky")
    @Issue("42")
    @Test(description = "Should process flaky for failed tests")
    public void shouldAddFlakyToFailedTests() throws Exception {
        runTestNgSuites("suites/gh-42.xml");

        List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .hasSize(1)
                .filteredOn(flakyPredicate())
                .extracting(TestResult::getFullName)
                .hasSize(1)
                .containsExactly(
                        "io.qameta.allure.testng.samples.FailedFlakyTest.flakyWithFailure");
    }

    @Feature("History")
    @Story("Parameters")
    @Test(description = "Should use parameters for history id")
    public void shouldUseParametersForHistoryIdGeneration() throws Exception {
        runTestNgSuites("suites/history-id-parameters.xml");

        final List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .extracting(TestResult::getHistoryId)
                .doesNotHaveDuplicates();
    }

    @Feature("History")
    @Story("Base history support")
    @Test(description = "Should generate the same history id for the same tests")
    public void shouldGenerateSameHistoryIdForTheSameTests() throws Exception {
        runTestNgSuites("suites/history-id-the-same.xml");

        final List<TestResult> testResults = results.getAllTestResults();
        assertThat(testResults)
                .extracting(TestResult::getHistoryId)
                .containsExactlyInAnyOrder("45e3e2818aabf660b03908be12ba64f7", "45e3e2818aabf660b03908be12ba64f7");
    }

    @Feature("Test fixtures")
    @Story("Suite")
    @Story("Test")
    @Story("Class")
    @Story("Method")
    @Issue("67")
    @Test(description = "Should set correct status for fixtures")
    public void shouldSetCorrectStatusesForFixtures() throws Exception {
        runTestNgSuites(
                "suites/per-suite-fixtures-combination.xml",
                "suites/per-method-fixtures-combination.xml",
                "suites/per-class-fixtures-combination.xml",
                "suites/per-test-tag-fixtures-combination.xml",
                "suites/failed-test-passed-fixture.xml"
        );


        assertThat(results.getAllSetUpResults())
                .hasSize(10)
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("beforeSuite1", Status.PASSED),
                        Tuple.tuple("beforeSuite2", Status.PASSED),
                        Tuple.tuple("beforeMethod1", Status.PASSED),
                        Tuple.tuple("beforeMethod2", Status.PASSED),
                        Tuple.tuple("beforeMethod1", Status.PASSED),
                        Tuple.tuple("beforeMethod2", Status.PASSED),
                        Tuple.tuple("beforeClass", Status.PASSED),
                        Tuple.tuple("beforeTest1", Status.PASSED),
                        Tuple.tuple("beforeTest2", Status.PASSED),
                        Tuple.tuple("beforeTestPassed", Status.PASSED)
                );

        assertThat(results.getAllTearDownResults())
                .hasSize(9)
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("afterSuite1", Status.PASSED),
                        Tuple.tuple("afterSuite2", Status.PASSED),
                        Tuple.tuple("afterMethod1", Status.PASSED),
                        Tuple.tuple("afterMethod2", Status.PASSED),
                        Tuple.tuple("afterMethod1", Status.PASSED),
                        Tuple.tuple("afterMethod2", Status.PASSED),
                        Tuple.tuple("afterClass", Status.PASSED),
                        Tuple.tuple("afterTest1", Status.PASSED),
                        Tuple.tuple("afterTest2", Status.PASSED)
                );
    }

    @Feature("Test fixtures")
    @Story("Suite")
    @Story("Test")
    @Story("Method")
    @Issue("67")
    @Test(description = "Should set correct status for failed before fixtures")
    public void shouldSetCorrectStatusForFailedBeforeFixtures() throws Exception {
        runTestNgSuites(
                "suites/failed-before-suite-fixture.xml",
                "suites/failed-before-test-fixture.xml",
                "suites/failed-before-method-fixture.xml"
        );

        assertThat(results.getAllSetUpResults())
                .hasSize(3)
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("beforeSuite", Status.BROKEN),
                        Tuple.tuple("beforeTest", Status.BROKEN),
                        Tuple.tuple("beforeMethod", Status.BROKEN)
                );
    }

    @Feature("Test fixtures")
    @Story("Suite")
    @Story("Test")
    @Story("Method")
    @Issue("67")
    @Test(description = "Should set correct status for failed after fixtures")
    public void shouldSetCorrectStatusForFailedAfterFixtures() throws Exception {
        runTestNgSuites(
                "suites/failed-after-suite-fixture.xml",
                "suites/failed-after-test-fixture.xml",
                "suites/failed-after-method-fixture.xml"
        );

        assertThat(results.getAllTearDownResults())
                .hasSize(3)
                .extracting(TestResult::getName, TestResult::getStatus)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("afterSuite", Status.BROKEN),
                        Tuple.tuple("afterTest", Status.BROKEN),
                        Tuple.tuple("afterMethod", Status.BROKEN)
                );
    }

    @Feature("Parameters")
    @Issue("97")
    @Test(description = "Should process varargs test parameters")
    public void shouldProcessVarargsParameters() throws Exception {
        runTestNgSuites("suites/gh-97.xml");

        assertThat(results.getAllTestResults())
                .hasSize(1)
                .flatExtracting(TestResult::getParameters)
                .extracting(Parameter::getValue)
                .containsExactlyInAnyOrder(
                        "[a, b, c]"
                );
    }

    @Feature("Test fixtures")
    @Story("Class")
    @Issue("99")
    @Test(description = "Should attach class fixtures correctly")
    public void shouldAttachClassFixturesCorrectly() throws Exception {
        runTestNgSuites("suites/gh-99.xml");

        assertThat(results.getAllTestResults())
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder(
                        "classFixtures1", "classFixtures2",
                        "classFixtures3", "classFixturesInParent"
                );

        assertThat(results.getAllTestResults())
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> "package".equals(label.getName()))
                .extracting(Label::getValue)
                .containsExactlyInAnyOrder(
                        "io.qameta.allure.testng.samples.ClassFixtures1",
                        "io.qameta.allure.testng.samples.ClassFixtures2",
                        "io.qameta.allure.testng.samples.ClassFixtures3",
                        "io.qameta.allure.testng.samples.ClassFixturesInParent"
                );

        final TestResult test1 = findTestResultByName("classFixtures1");
        final TestResult classBeforeFixture1 = findBeforeTestFixtureByName("beforeClass1");
        final TestResult classAfterFixture1 = findAfterTestFixtureByName("afterClass1");
        assertThat(classBeforeFixture1.getChildren()).containsOnly(test1.getUuid());
        assertThat(classAfterFixture1.getChildren()).containsOnly(test1.getUuid());

        final TestResult test2 = findTestResultByName("classFixtures2");
        final TestResult classBeforeFixture2 = findBeforeTestFixtureByName("beforeClass2");
        final TestResult classAfterFixture2 = findAfterTestFixtureByName("afterClass2");
        assertThat(classBeforeFixture2.getChildren()).containsOnly(test2.getUuid());
        assertThat(classAfterFixture2.getChildren()).containsOnly(test2.getUuid());

        final TestResult test3 = findTestResultByName("classFixtures3");
        final TestResult classBeforeFixture3 = findBeforeTestFixtureByName("beforeClass3");
        final TestResult classAfterFixture3 = findAfterTestFixtureByName("afterClass3");
        assertThat(classBeforeFixture3.getChildren()).containsOnly(test3.getUuid());
        assertThat(classAfterFixture3.getChildren()).containsOnly(test3.getUuid());

        final TestResult testInParent = findTestResultByName("classFixturesInParent");
        final TestResult classBeforeFixtureInParent = findBeforeTestFixtureByName("beforeInInherited");
        assertThat(classBeforeFixtureInParent.getChildren()).containsOnly(testInParent.getUuid());
    }

    @Feature("History")
    @Story("Inherited tests")
    @Issue("102")
    @Test(description = "Should generate different history id for inherited tests")
    public void shouldGenerateDifferentHistoryIdForInheritedTests() throws Exception {
        runTestNgSuites("suites/gh-102.xml");

        assertThat(results.getAllTestResults())
                .extracting(TestResult::getHistoryId)
                .doesNotHaveDuplicates();
    }

    @Feature("Test fixtures")
    @Story("Descriptions")
    @Issue("101")
    @Test(description = "Should use fixture descriptions")
    public void shouldUseFixtureDescriptions() throws Exception {
        runTestNgSuites("suites/gh-101.xml");

        assertThat(results.getAllSetUpResults())
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("Set up method with description");
    }

    @Feature("Basic framework support")
    @Story("Descriptions")
    @Issue("106")
    @Test
    public void shouldProcessCyrillicDescriptions() throws Exception {
        runTestNgSuites("suites/gh-106.xml");

        assertThat(results.getAllTestResults())
                .extracting(TestResult::getName)
                .containsExactlyInAnyOrder("Тест с описанием на русском языке");
    }

    @Step("Check containers per method")
    private static void assertContainersPerMethod(String name, List<TestResult> fixturesList,
                                                  Set<String> uids) {
        final Condition<List<? extends TestResult>> singlyMapped = new Condition<>(results ->
                results.stream().allMatch(c -> c.getChildren().size() == 1),
                format("All for per-method fixture %s should be linked to only one testng result", name));

        assertThat(fixturesList)
                .filteredOn("name", name)
                .is(singlyMapped)
                .flatExtracting(TestResult::getChildren)
                .as("Unexpected children for per-method fixtures " + name)
                .containsOnlyElementsOf(uids);
    }

    @Step("Check label contents")
    private static void assertLabelContents(final List<TestResult> testResults, String name, String value) {
        assertThat(testResults)
                .flatExtracting(TestResult::getLabels)
                .filteredOn(label -> name.equals(label.getName()))
                .extracting(Label::getValue)
                .containsOnly(value);
    }

    @Step("Check before fixtures")
    private void assertChildrensFixtures(final List<String> childrens,
                                                final String... testName) {
        assertThat(results.getAllTestResults())
                .filteredOn(result -> childrens.contains(result.getUuid()))
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder(testName);
    }

    @Step("Check fixtures")
    private static void assertFixtures(final List<TestResult> results, final String... fixturesNames) {
        assertThat(results)
                .flatExtracting(TestResult::getName)
                .contains(fixturesNames);
    }

    @Step("Check before fixtures")
    private void assertBeforeFixtures(final String... befores) {
        assertThat(results.getAllSetUpResults())
                .flatExtracting(TestResult::getName)
                .contains(befores);
    }

    @Step("Check after fixtures")
    private void assertAfterFixtures(final String... afters) {
        assertThat(results.getAllTearDownResults())
                .flatExtracting(TestResult::getName)
                .contains(afters);
    }

    @Step("Find flaky")
    private Predicate<TestResult> flakyPredicate() {
        return TestResult::isFlaky;
    }

    @Step("Find muted")
    private Predicate<TestResult> mutedPredicate() {
        return TestResult::isMuted;
    }

    @Step("Has links")
    private Predicate<TestResult> hasLinks() {
        return testResult -> !testResult.getLinks().isEmpty();
    }

    @Step("Find resutls by name")
    private TestResult findTestResultByName(final String name) {
        return results.getAllTestResults().stream()
                .filter(testResult -> name.equals(testResult.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("can not find result by name " + name));
    }

    @Step("Find before fixture by name")
    private TestResult findBeforeTestFixtureByName(final String name) {
        return results.getAllSetUpResults().stream()
                .filter(result -> name.equalsIgnoreCase(result.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("can not find container by name " + name));
    }

    @Step("Find after fixture by name")
    private TestResult findAfterTestFixtureByName(final String name) {
        return results.getAllTearDownResults().stream()
                .filter(result -> name.equalsIgnoreCase(result.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("can not find container by name " + name));
    }

    @Step("Run testng suites")
    private void runTestNgSuites(String... suites) {
        final ClassLoader classLoader = getClass().getClassLoader();
        List<String> suiteFiles = Arrays.stream(suites)
                .map(classLoader::getResource)
                .map(URL::getFile)
                .collect(Collectors.toList());
        testNg.setTestSuites(suiteFiles);
        testNg.run();
    }
}