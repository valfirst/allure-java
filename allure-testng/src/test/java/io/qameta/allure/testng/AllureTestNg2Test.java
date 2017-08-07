package io.qameta.allure.testng;

import io.qameta.allure.Allure;
import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureTestNg2Test {

    @Test
    public void shouldWork() throws Exception {
        final TestNG testNG = new TestNG(false);
        testNG.addListener((ITestNGListener) new AllureTestNg2(Allure.getLifecycle()));
        runTestNgSuites(testNG, "suites/single-test.xml");
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