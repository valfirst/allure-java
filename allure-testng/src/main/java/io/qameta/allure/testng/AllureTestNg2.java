package io.qameta.allure.testng;

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
import org.testng.IAttributes;
import org.testng.IClass;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener2;
import org.testng.ISuite;
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
@SuppressWarnings("all")
public class AllureTestNg2 implements IInvokedMethodListener2 {

    private static final String ALLURE_UUID = "ALLURE_UUID";
    private static final String MD_5 = "md5";

    private final Map<ISuite, Set<String>> suiteConfigurations = new ConcurrentHashMap<>();
    private final Map<ISuite, Set<String>> suiteTests = new ConcurrentHashMap<>();

    private final Map<ISuite, Set<String>> classConfigurations = new ConcurrentHashMap<>();
    private final Map<ISuite, Set<String>> classTests = new ConcurrentHashMap<>();

    private final Map<ITestContext, String> testConfigurations = new ConcurrentHashMap<>();
    private final Map<String, String> groupConfigurations = new ConcurrentHashMap<>();
    private final Map<String, String> methodConfigurations = new ConcurrentHashMap<>();

    private final Lifecycle lifecycle;

    public AllureTestNg2(final Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void beforeInvocation(final IInvokedMethod method,
                                 final ITestResult testResult, final ITestContext context) {
        final ITestNGMethod testMethod = method.getTestMethod();
        final Set<Parameter> parameters = getParameters(testResult);
        final TestResult result = getResult(testMethod)
                .setHistoryId(getHistoryId(testMethod, parameters))
                .setFullName(getQualifiedName(testMethod))
                .setParameters(parameters)
                .setLinks(getLinks(testResult))
                .setLabels(getInitialLabels(testResult));;

        if (testMethod.isBeforeSuiteConfiguration() || testMethod.isAfterSuiteConfiguration()) {
            if (Objects.isNull(suiteConfigurations.get(context.getSuite()))) {
                suiteConfigurations.put(context.getSuite(), new HashSet<>());
            }
            suiteConfigurations.get(context.getSuite()).add(result.getUuid());
        }
        if (testMethod.isBeforeTestConfiguration() || testMethod.isAfterTestConfiguration()) {
            testConfigurations.put(context, result.getUuid());
        }
        if (testMethod.isBeforeClassConfiguration() || testMethod.isAfterClassConfiguration()) {
            if (Objects.isNull(classConfigurations.get(context.getSuite()))) {
                classConfigurations.put(context.getSuite(), new HashSet<>());
            }
            classConfigurations.get(context.getSuite()).add(result.getUuid());
        }
        if (testMethod.isBeforeGroupsConfiguration() || testMethod.isAfterGroupsConfiguration()) {
            Arrays.stream(testMethod.getGroups())
                    .forEach(group -> groupConfigurations.put(group, result.getUuid()));
        }
        if (testMethod.isBeforeMethodConfiguration() || testMethod.isAfterMethodConfiguration()) {
            methodConfigurations.put(testMethod.getMethodName(), result.getUuid());
        }
        if (testMethod.isTest()) {
            Optional.ofNullable(suiteConfigurations.get(context.getSuite())).ifPresent(
                set -> set.forEach(uid -> result.getChildren().add(uid))
            );
            Optional.ofNullable(testConfigurations.get(context)).ifPresent(
                uid -> result.getChildren().add(uid)
            );
            Optional.ofNullable(classConfigurations.get(context.getSuite())).ifPresent(
                set -> set.forEach(uid -> result.getChildren().add(uid))
            );
            Arrays.stream(testMethod.getGroups()).forEach(
                group -> Optional.ofNullable(groupConfigurations.get(group)).ifPresent(
                    uid -> result.getChildren().add(uid)
                )
            );
            Optional.ofNullable(methodConfigurations.get(testMethod.getMethodName())).ifPresent(
                uid -> result.getChildren().add(uid)
            );
        }
        lifecycle.startTest(result);
    }

    @Override
    public void beforeInvocation(final IInvokedMethod method, final ITestResult testResult) {
        //do nothing
    }

    @Override
    public void afterInvocation(final IInvokedMethod method, final ITestResult testResult,
                                final ITestContext context) {
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
        lifecycle.currentTest().ifPresent(current -> {
            lifecycle.stopTest();
            lifecycle.writeTest(current.getUuid());
        });
    }

    @Override
    public void afterInvocation(final IInvokedMethod method, final ITestResult testResult) {
        //do nothing
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

    private String getUniqueUuid(final IAttributes suite) {
        if (Objects.isNull(suite.getAttribute(ALLURE_UUID))) {
            suite.setAttribute(ALLURE_UUID, UUID.randomUUID().toString());
        }
        return Objects.toString(suite.getAttribute(ALLURE_UUID));
    }

    private MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(MD_5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find md5 hashing algorithm", e);
        }
    }

    protected String getHistoryId(final ITestNGMethod method, final Set<Parameter> parameters) {
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
                new Label().setName("parentSuite").setValue(safeExtractSuiteName(testClass)),
                new Label().setName("suite").setValue(safeExtractTestTag(testClass)),
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
