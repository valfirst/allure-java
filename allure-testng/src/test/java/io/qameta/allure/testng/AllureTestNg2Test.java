package io.qameta.allure.testng;

import io.qameta.allure.Lifecycle;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultType;
import io.qameta.allure.test.InMemoryResultsWriter;
import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureTestNg2Test {

    protected InMemoryResultsWriter writer = new InMemoryResultsWriter();

    @Test
    public void shouldWork() throws Exception {
        final TestNG testNG = new TestNG(false);
        Lifecycle lifecycle = new Lifecycle(writer);

        testNG.addListener((ITestNGListener) new AllureTestNg2(lifecycle));
        runTestNgSuites(testNG, "suites/single-test.xml");

        assertThat(writer.getResults())
                .extracting(TestResult::getType, TestResult::getName)
                .containsExactly(tuple(TestResultType.SET_UP, "beforeMethod"));
    }

    private void runTestNgSuites(TestNG testNg, String... suites) {
        final ClassLoader classLoader = getClass().getClassLoader();
        List<String> suiteFiles = Arrays.stream(suites)
                .map(classLoader::getResource)
                .map(URL::getFile)
                .collect(Collectors.toList());
        testNg.setTestSuites(suiteFiles);
        testNg.run();
    }
}