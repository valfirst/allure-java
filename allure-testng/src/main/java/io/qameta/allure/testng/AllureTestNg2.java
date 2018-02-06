package io.qameta.allure.testng;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Lifecycle;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultType;
import io.qameta.allure.util.ResultsUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.IClass;
import org.testng.IClassListener;
import org.testng.IExecutionListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener2;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.internal.ConstructorOrMethod;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.Lifecycle.dependsOn;
import static io.qameta.allure.util.ResultsUtils.firstNonEmpty;
import static io.qameta.allure.util.ResultsUtils.getHostName;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getThreadName;
import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;
import static java.util.stream.IntStream.range;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.GodClass", "PMD.TooManyMethods", "BooleanExpressionComplexity"})
public class AllureTestNg2 implements ISuiteListener, IClassListener, IInvokedMethodListener2, IExecutionListener {

    private static final String MD_5 = "md5";

    private final Map<ISuite, Set<String>> suiteBeforeConfigurations = new ConcurrentHashMap<>();
    private final Map<ISuite, Set<String>> suiteTestMethods = new ConcurrentHashMap<>();

    private final Map<ITestContext, Set<String>> testBeforeConfigurations = new ConcurrentHashMap<>();
    private final Map<ITestContext, Set<String>> testTestMethods = new ConcurrentHashMap<>();

    private final Map<ITestClass, Set<String>> classBeforeConfigurations = new ConcurrentHashMap<>();
    private final Map<ITestClass, Set<String>> classTestMethods = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> groupBeforeConfigurations = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> groupTestMethods = new ConcurrentHashMap<>();

    @SuppressWarnings("checkstyle:LineLength")
    private final ThreadLocal<String> currentTest = new InheritableThreadLocal<String>() {
        @Override
        public String initialValue() {
            return StringUtils.EMPTY;
        }
    };

    @SuppressWarnings("checkstyle:LineLength")
    private final ThreadLocal<Set<String>> currentBefores = new InheritableThreadLocal<Set<String>>() {
        @Override
        public Set<String> initialValue() {
            return new HashSet<>();
        }
    };

    private final Lifecycle lifecycle;

    public AllureTestNg2(final Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public AllureTestNg2() {
        this.lifecycle = Allure.getLifecycle();
    }

    @Override
    public void onStart(final ISuite suite) {
        suiteBeforeConfigurations.put(suite, new HashSet<>());
        suiteTestMethods.put(suite, new HashSet<>());
    }

    @Override
    public void onFinish(final ISuite suite) {
        writeBeforeSuiteFixture(suite);
        writeBeforeTestFixtures(suite);
    }

    @Override
    public void onBeforeClass(final ITestClass testClass) {
        classBeforeConfigurations.put(testClass, new HashSet<>());
        classTestMethods.put(testClass, new HashSet<>());
    }

    @Override
    public void onAfterClass(final ITestClass testClass) {
        writeBeforeClassFixtures(testClass);
    }

    @Override
    public void onExecutionStart() {
        // do nothing
    }

    @Override
    public void onExecutionFinish() {
        writeBeforeGroupFixtures();
    }

    private void writeBeforeMethodFixture() {
        currentBefores.get().forEach(methodUuid -> {
            lifecycle.updateTest(methodUuid, dependsOn(currentTest.get()));
            lifecycle.writeTest(methodUuid);
        });
        currentBefores.remove();
    }

    private void writeBeforeGroupFixtures() {
        groupBeforeConfigurations.forEach(
            (group, groupUuids) -> {
                groupUuids.forEach(
                    groupUuid -> writeBeforeGroupFixture(group, groupUuid)
                );
                groupBeforeConfigurations.remove(group);
                groupTestMethods.remove(group);
            }
        );
    }

    private void writeBeforeGroupFixture(final String group, final String groupUuid) {
        groupTestMethods.get(group).forEach(
            uuid -> lifecycle.updateTest(groupUuid, dependsOn(uuid))
        );
        lifecycle.writeTest(groupUuid);
    }

    private void writeBeforeClassFixtures(final ITestClass testClass) {
        classBeforeConfigurations.get(testClass).forEach(
            testClassUuid -> writeBeforeClassFixture(testClass, testClassUuid)
        );
        suiteBeforeConfigurations.remove(testClass);
    }

    private void writeBeforeClassFixture(final ITestClass testClass, final String testClassUuid) {
        classTestMethods.get(testClass).forEach(
            uuid -> lifecycle.updateTest(testClassUuid, dependsOn(uuid))
        );
        lifecycle.writeTest(testClassUuid);
    }

    private void writeBeforeTestFixtures(final ISuite suite) {
        testBeforeConfigurations.entrySet().stream()
            .filter(map -> map.getKey().getSuite().equals(suite))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            .forEach((context, testUuids) -> {
                testUuids.forEach(
                    testUuid -> writeBeforeTestFixture(context, testUuid)
                );
                testBeforeConfigurations.remove(context);
            });
    }

    private void writeBeforeTestFixture(final ITestContext context, final String testUuid) {
        testTestMethods.get(context).forEach(
            uuid -> lifecycle.updateTest(testUuid, dependsOn(uuid))
        );
        lifecycle.writeTest(testUuid);
    }

    private void writeBeforeSuiteFixture(final ISuite suite) {
        suiteBeforeConfigurations.get(suite).forEach(suiteUuid -> {
            suiteTestMethods.get(suite).forEach(
                uuid -> lifecycle.updateTest(suiteUuid, dependsOn(uuid))
            );
            lifecycle.writeTest(suiteUuid);
        });
        suiteBeforeConfigurations.remove(suite);
    }

    @Override
    public void beforeInvocation(final IInvokedMethod method, final ITestResult testResult) {
        //do nothing
    }

    @Override
    public void beforeInvocation(final IInvokedMethod method,
                                 final ITestResult testResult,
                                 final ITestContext context) {
        final ITestNGMethod testMethod = method.getTestMethod();
        final Set<Parameter> parameters = getParameters(testResult);
        final TestResult result = getResult(testMethod)
                .setHistoryId(getHistoryId(testMethod, parameters))
                .setFullName(getQualifiedName(testMethod))
                .setParameters(parameters)
                .setLinks(getLinks(testResult))
                .setLabels(getInitialLabels(testResult));

        if (testMethod.isBeforeSuiteConfiguration()) {
            suiteBeforeConfigurations.get(context.getSuite()).add(result.getUuid());
        }
        if (testMethod.isBeforeTestConfiguration()) {
            beforeTestConfiguration(context, result);
        }
        if (testMethod.isBeforeClassConfiguration()) {
            classBeforeConfigurations.get(testMethod.getTestClass()).add(result.getUuid());
        }
        if (testMethod.isBeforeGroupsConfiguration()) {
            beforeGroupsConfiguration(testMethod, result);
        }
        if (testMethod.isBeforeMethodConfiguration()) {
            currentBefores.get().add(result.getUuid());
        }
        if (testMethod.isTest()) {
            beforeTestMethod(context, testMethod, result);
        }
        lifecycle.startTest(result);
    }

    private void beforeTestMethod(final ITestContext context,
                                  final ITestNGMethod testMethod,
                                  final TestResult result) {
        suiteTestMethods.get(context.getSuite()).add(result.getUuid());
        addTestMethodToTestCofiguration(context, result);
        addTestMethodToClassConfiguration(testMethod, result);
        addTestMethodToGroupsConfiguration(testMethod, result);
        currentTest.set(result.getUuid());
        writeBeforeMethodFixture();
    }

    private void addTestMethodToGroupsConfiguration(final ITestNGMethod testMethod, final TestResult result) {
        Arrays.stream(testMethod.getGroups()).forEach(
            group -> addTestMethodToGroupConfiguration(result, group)
        );
    }

    private void addTestMethodToGroupConfiguration(final TestResult result, final String group) {
        if (Objects.isNull(groupTestMethods.get(group))) {
            groupTestMethods.put(group, new HashSet<>());
        }
        groupTestMethods.get(group).add(result.getUuid());
    }

    private void addTestMethodToClassConfiguration(final ITestNGMethod testMethod, final TestResult result) {
        if (Objects.isNull(classTestMethods.get(testMethod.getTestClass()))) {
            classTestMethods.put(testMethod.getTestClass(), new HashSet<>());
        }
        classTestMethods.get(testMethod.getTestClass()).add(result.getUuid());
    }

    private void addTestMethodToTestCofiguration(final ITestContext context, final TestResult result) {
        if (Objects.isNull(testTestMethods.get(context))) {
            testTestMethods.put(context, new HashSet<>());
        }
        testTestMethods.get(context).add(result.getUuid());
    }

    private void beforeGroupsConfiguration(final ITestNGMethod testMethod, final TestResult result) {
        Arrays.stream(testMethod.getGroups()).forEach(
            group -> addGroupToGroupsConfiguration(result, group)
        );
    }

    private void addGroupToGroupsConfiguration(final TestResult result, final String group) {
        if (Objects.isNull(groupBeforeConfigurations.get(group))) {
            groupBeforeConfigurations.put(group, new HashSet<>());
        }
        groupBeforeConfigurations.get(group).add(result.getUuid());
    }

    private void beforeTestConfiguration(final ITestContext context, final TestResult result) {
        if (Objects.isNull(testBeforeConfigurations.get(context))) {
            testBeforeConfigurations.put(context, new HashSet<>());
        }
        testBeforeConfigurations.get(context).add(result.getUuid());
    }

    @Override
    public void afterInvocation(final IInvokedMethod method, final ITestResult testResult) {
        //do nothing
    }

    @Override
    public void afterInvocation(final IInvokedMethod method, final ITestResult testResult,
                                final ITestContext context) {
        updateAfterExecution(testResult);

        final ITestNGMethod testMethod = method.getTestMethod();
        if (isSetUpConfiguration(testMethod)) {
            lifecycle.stopTest();
        } else {
            lifecycle.currentTest().ifPresent(current -> {
                updateAfterFixtures(context, testMethod);
                lifecycle.stopTest();
                lifecycle.writeTest(current.getUuid());
            });
        }
    }

    private void updateAfterFixtures(final ITestContext context, final ITestNGMethod testMethod) {
        if (testMethod.isAfterSuiteConfiguration()) {
            suiteTestMethods.get(context.getSuite()).forEach(
                uuid -> lifecycle.updateTest(dependsOn(uuid))
            );
        } else if (testMethod.isAfterTestConfiguration()) {
            testTestMethods.get(context).forEach(
                uuid -> lifecycle.updateTest(dependsOn(uuid))
            );
        } else if (testMethod.isAfterClassConfiguration()) {
            classTestMethods.get(testMethod.getTestClass()).forEach(
                uuid -> lifecycle.updateTest(dependsOn(uuid))
            );
        } else if (testMethod.isAfterGroupsConfiguration()) {
            Arrays.stream(testMethod.getGroups()).forEach(
                group -> groupTestMethods.get(group).forEach(
                    uuid -> lifecycle.updateTest(dependsOn(uuid))
                )
            );
        } else if (testMethod.isAfterMethodConfiguration()) {
            lifecycle.updateTest(dependsOn(currentTest.get()));
        }
    }

    private void updateAfterExecution(final ITestResult testResult) {
        lifecycle.updateTest(r -> r.setStage(Stage.FINISHED));
        if (testResult.isSuccess()) {
            lifecycle.updateTest(r -> r.setStatus(Status.PASSED));
        } else {
            lifecycle.updateTest(r -> r.setStatus(getStatus(testResult.getThrowable()).orElse(Status.BROKEN)));
        }
        Optional.ofNullable(testResult.getThrowable()).ifPresent(throwable -> lifecycle.updateTest(r -> {
            r.setStatusMessage(throwable.getMessage());
            r.setStatusTrace(ResultsUtils.getStackTraceAsString(throwable));
        }));
    }

    private TestResult getResult(final ITestNGMethod method) {
        final TestResult result = new TestResult()
                .setUuid(UUID.randomUUID().toString())
                .setName(getMethodName(method))
                .setDescription(method.getDescription());
        if (method.isTest()) {
            result.setType(TestResultType.TEST);
        }
        if (isSetUpConfiguration(method)) {
            result.setType(TestResultType.SET_UP);
        }
        if (isTearDownConfiguration(method)) {
            result.setType(TestResultType.TEAR_DOWN);
        }
        return result;
    }

    private boolean isTearDownConfiguration(final ITestNGMethod method) {
        return method.isAfterSuiteConfiguration()
                || method.isAfterTestConfiguration()
                || method.isAfterClassConfiguration()
                || method.isAfterGroupsConfiguration()
                || method.isAfterMethodConfiguration();
    }

    private boolean isSetUpConfiguration(final ITestNGMethod method) {
        return method.isBeforeSuiteConfiguration()
                || method.isBeforeTestConfiguration()
                || method.isBeforeClassConfiguration()
                || method.isBeforeGroupsConfiguration()
                || method.isBeforeMethodConfiguration();
    }

    private String getMethodName(final ITestNGMethod method) {
        return firstNonEmpty(
                method.getDescription(),
                method.getMethodName(),
                getQualifiedName(method)).orElse("Unknown");
    }

    private String getQualifiedName(final ITestNGMethod method) {
        return method.getRealClass().getName() + "." + method.getMethodName();
    }

    private Set<Link> getLinks(final ITestResult result) {
        return Stream.of(
                getAnnotationsOnClass(result, io.qameta.allure.Link.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnMethod(result, io.qameta.allure.Link.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnClass(result, io.qameta.allure.Issue.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnMethod(result, io.qameta.allure.Issue.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnClass(result, io.qameta.allure.TmsLink.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnMethod(result, io.qameta.allure.TmsLink.class).stream().map(ResultsUtils::createLink)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toSet());
    }

    private MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(MD_5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find md5 hashing algorithm", e);
        }
    }

    private String getHistoryId(final ITestNGMethod method, final Set<Parameter> parameters) {
        final MessageDigest digest = getMessageDigest();
        final String testClassName = method.getTestClass().getName();
        final String methodName = method.getMethodName();
        digest.update(testClassName.getBytes(UTF_8));
        digest.update(methodName.getBytes(UTF_8));
        parameters.stream()
                .sorted(comparing(Parameter::getName).thenComparing(Parameter::getValue))
                .forEachOrdered(parameter -> {
                    digest.update(parameter.getName().getBytes(UTF_8));
                    digest.update(parameter.getValue().getBytes(UTF_8));
                });
        final byte[] bytes = digest.digest();
        return new BigInteger(1, bytes).toString(16);
    }

    private Set<Parameter> getParameters(final ITestResult testResult) {
        final Stream<Parameter> tagsParameters = testResult.getTestContext()
                .getCurrentXmlTest().getAllParameters().entrySet()
                .stream()
                .map(entry -> new Parameter().setName(entry.getKey()).setValue(entry.getValue()));
        final String[] parameterNames = Optional.of(testResult)
                .map(ITestResult::getMethod)
                .map(ITestNGMethod::getConstructorOrMethod)
                .map(ConstructorOrMethod::getMethod)
                .map(Executable::getParameters)
                .map(Stream::of)
                .orElseGet(Stream::empty)
                .map(java.lang.reflect.Parameter::getName)
                .toArray(String[]::new);
        final String[] parameterValues = Stream.of(testResult.getParameters())
                .map(this::convertParameterValueToString)
                .toArray(String[]::new);
        final Stream<Parameter> methodParameters = range(0, min(parameterNames.length, parameterValues.length))
                .mapToObj(i -> new Parameter().setName(parameterNames[i]).setValue(parameterValues[i]));
        return Stream.concat(tagsParameters, methodParameters)
                .collect(Collectors.toSet());
    }

    private String convertParameterValueToString(final Object parameter) {
        if (Objects.nonNull(parameter) && parameter.getClass().isArray()) {
            return Arrays.toString((Object[]) parameter);
        }
        return Objects.toString(parameter);
    }

    private Set<Label> getInitialLabels(final ITestResult testResult) {
        final Set<Label> labels = new HashSet<>();
        final ITestNGMethod method = testResult.getMethod();
        final ITestClass testClass = method.getTestClass();
        labels.addAll(Arrays.asList(
                //Packages grouping
                new Label().setName("package").setValue(testClass.getName()),
                new Label().setName("testClass").setValue(testClass.getName()),
                new Label().setName("testMethod").setValue(method.getMethodName()),
                //xUnit grouping
                new Label().setName("suite").setValue(safeExtractSuiteName(testClass)),
                new Label().setName("tag").setValue(safeExtractTestTag(testClass)),
                new Label().setName("subSuite").setValue(safeExtractTestClassName(testClass)),
                //Timeline grouping
                new Label().setName("host").setValue(getHostName()),
                new Label().setName("thread").setValue(getThreadName())
        ));
        labels.addAll(getLabels(testResult));
        return labels;
    }

    private static String safeExtractSuiteName(final ITestClass testClass) {
        final Optional<XmlTest> xmlTest = Optional.ofNullable(testClass.getXmlTest());
        return xmlTest.map(XmlTest::getSuite).map(XmlSuite::getName).orElse("Undefined suite");
    }

    private static String safeExtractTestTag(final ITestClass testClass) {
        final Optional<XmlTest> xmlTest = Optional.ofNullable(testClass.getXmlTest());
        return xmlTest.map(XmlTest::getName).orElse("Undefined testng tag");
    }

    private static String safeExtractTestClassName(final ITestClass testClass) {
        return firstNonEmpty(testClass.getTestName(), testClass.getName()).orElse("Undefined class name");
    }

    private Set<Label> getLabels(final ITestResult result) {
        return Stream.of(
                getLabels(result, Epic.class, ResultsUtils::createLabel),
                getLabels(result, Feature.class, ResultsUtils::createLabel),
                getLabels(result, Story.class, ResultsUtils::createLabel),
                getLabels(result, Severity.class, ResultsUtils::createLabel),
                getLabels(result, Owner.class, ResultsUtils::createLabel)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toSet());
    }

    private <T extends Annotation> Stream<Label> getLabels(final ITestResult result, final Class<T> clazz,
                                                           final Function<T, Label> extractor) {
        final List<Label> onMethod = getAnnotationsOnMethod(result, clazz).stream()
                .map(extractor)
                .collect(Collectors.toList());
        if (!onMethod.isEmpty()) {
            return onMethod.stream();
        }
        return getAnnotationsOnClass(result, clazz).stream()
                .map(extractor);
    }

    private <T extends Annotation> List<T> getAnnotationsOnMethod(final ITestResult result, final Class<T> clazz) {
        return Stream.of(result)
                .map(ITestResult::getMethod)
                .filter(Objects::nonNull)
                .map(ITestNGMethod::getConstructorOrMethod)
                .map(ConstructorOrMethod::getMethod)
                .flatMap(method -> Stream.of(method.getAnnotationsByType(clazz)))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> List<T> getAnnotationsOnClass(final ITestResult result, final Class<T> clazz) {
        return Stream.of(result)
                .map(ITestResult::getTestClass)
                .filter(Objects::nonNull)
                .map(IClass::getRealClass)
                .flatMap(aClass -> Stream.of(aClass.getAnnotationsByType(clazz)))
                .map(clazz::cast)
                .collect(Collectors.toList());
    }
}
