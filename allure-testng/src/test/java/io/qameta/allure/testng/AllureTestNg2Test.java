package io.qameta.allure.testng;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Lifecycle;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.qameta.allure.aspect.AttachmentsAspects;
import io.qameta.allure.aspect.StepsAspects;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.InMemoryResultsWriter;
import org.assertj.core.api.Condition;
import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

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
        assertBeforeFixtures(results, before1, before2);

        List<TestResult> setUpResults = results.getAllSetUpResults();
        setUpResults.stream().filter(result -> before1.equals(result.getName()) || before2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(results, result.getChildren(), testOneName, testTwoName));
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
        assertBeforeFixtures(results, before1, before2);

        List<TestResult> setUpResults = results.getAllSetUpResults();
        setUpResults.stream().filter(result -> before1.equals(result.getName()) || before2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(results, result.getChildren(), testOneName, testTwoName));
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
        assertBeforeFixtures(results, before1, before2);

        List<TestResult> setUpResults = results.getAllSetUpResults();
        setUpResults.stream().filter(result -> before1.equals(result.getName()) || before2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(results, result.getChildren(), testOneName, testTwoName));
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
        assertBeforeFixtures(results, before1, before2);

        List<TestResult> setUpResults = results.getAllSetUpResults();
        setUpResults.stream().filter(result -> before1.equals(result.getName()) || before2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(results, result.getChildren(), testOneName, testTwoName));
    }

    @Feature("Basic framework support")
    @Test(description = "Before method fixtures tests")
    public void beforeMethodFixtures() {
        final String before1 = "beforeMethodOne";
        final String before2 = "beforeMethodTwo";

        runTestNgSuites("suites/fixtures-test.xml");
        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Test case result has not been written").hasSize(2);
        assertBeforeFixtures(results, before1, before2);

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
        assertAfterFixtures(results, after1, after2);

        List<TestResult> tearDownResults = results.getAllTearDownResults();
        tearDownResults.stream().filter(result -> after1.equals(result.getName()) || after2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(results, result.getChildren(), testOneName, testTwoName));
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
        assertAfterFixtures(results, after1, after2);

        List<TestResult> tearDownResults = results.getAllTearDownResults();
        tearDownResults.stream().filter(result -> after1.equals(result.getName()) || after2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(results, result.getChildren(), testOneName, testTwoName));
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
        assertAfterFixtures(results, after1, after2);

        List<TestResult> tearDownResults = results.getAllTearDownResults();
        tearDownResults.stream().filter(result -> after1.equals(result.getName()) || after2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(results, result.getChildren(), testOneName, testTwoName));
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
        assertAfterFixtures(results, after1, after2);

        List<TestResult> tearDownResults = results.getAllTearDownResults();
        tearDownResults.stream().filter(result -> after1.equals(result.getName()) || after2.equals(result.getName()))
                .forEach(result -> assertChildrensFixtures(results, result.getChildren(), testOneName, testTwoName));
    }

    @Feature("Basic framework support")
    @Test(description = "After method fixtures tests")
    public void afterMethodFixtures() {
        final String after1 = "afterMethodOne";
        final String after2 = "afterMethodTwo";

        runTestNgSuites("suites/fixtures-test.xml");
        List<TestResult> testResults = results.getAllTestResults();

        assertThat(testResults).as("Test case result has not been written").hasSize(2);
        assertAfterFixtures(results, after1, after2);

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
                .hasSize(1).first()
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
        assertBeforeFixtures(results, before1, before2);
        assertAfterFixtures(results, after1, after2);
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

        assertBeforeFixtures(results, "beforeClass");
        assertAfterFixtures(results, "afterClass");
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
        assertBeforeFixtures(results, before1, before2);
        assertAfterFixtures(results, after1, after2);

        Set<String> uuids = testResults.stream().map(TestResult::getUuid).collect(Collectors.toSet());
        assertContainersPerMethod(before1, results.getAllSetUpResults(), uuids);
        assertContainersPerMethod(before2, results.getAllSetUpResults(), uuids);
        assertContainersPerMethod(after1, results.getAllTearDownResults(), uuids);
        assertContainersPerMethod(after2, results.getAllTearDownResults(), uuids);
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
    private static void assertChildrensFixtures(final InMemoryResultsWriter results,
                                                final List<String> childrens,
                                                final String... testName) {
        assertThat(results.getAllTestResults())
                .filteredOn(result -> childrens.contains(result.getUuid()))
                .flatExtracting(TestResult::getName)
                .containsExactlyInAnyOrder(testName);
    }

    @Step("Check before fixtures")
    private static void assertBeforeFixtures(final InMemoryResultsWriter results, final String... befores) {
        assertThat(results.getAllSetUpResults())
                .flatExtracting(TestResult::getName)
                .contains(befores);
    }

    @Step("Check after fixtures")
    private static void assertAfterFixtures(final InMemoryResultsWriter results, final String... afters) {
        assertThat(results.getAllTearDownResults())
                .flatExtracting(TestResult::getName)
                .contains(afters);
    }

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