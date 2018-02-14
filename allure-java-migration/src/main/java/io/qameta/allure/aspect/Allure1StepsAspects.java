package io.qameta.allure.aspect;

import io.qameta.allure.Allure;
import io.qameta.allure.Lifecycle;
import io.qameta.allure.model3.Parameter;
import io.qameta.allure.model3.Status;
import io.qameta.allure.model3.StepResult;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.qameta.allure.aspect.Allure1Utils.getName;
import static io.qameta.allure.aspect.Allure1Utils.getTitle;
import static io.qameta.allure.util.ResultsUtils.getStackTraceAsString;
import static io.qameta.allure.util.ResultsUtils.getStatus;

/**
 * Aspects (AspectJ) for handling {@link Step}.
 */
@Aspect
@SuppressWarnings("unused")
public class Allure1StepsAspects {

    private static Lifecycle lifecycle;

    @Pointcut("@annotation(ru.yandex.qatools.allure.annotations.Step)")
    public void withStepAnnotation() {
        //pointcut body, should be empty
    }

    @Pointcut("execution(* *(..))")
    public void anyMethod() {
        //pointcut body, should be empty
    }

    @Before("anyMethod() && withStepAnnotation()")
    public void stepStart(final JoinPoint joinPoint) {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final StepResult result = new StepResult()
                .setName(createTitle(joinPoint))
                .setParameters(getParameters(methodSignature, joinPoint.getArgs()));

        getLifecycle().startStep(result);
    }

    @AfterThrowing(pointcut = "anyMethod() && withStepAnnotation()", throwing = "e")
    public void stepFailed(final JoinPoint joinPoint, final Throwable e) {
        getLifecycle().updateStep(result -> result
                .setStatus(getStatus(e).orElse(Status.BROKEN))
                .setStatusMessage(e.getMessage())
                .setStatusTrace(getStackTraceAsString(e)));
        getLifecycle().stopStep();
    }

    @AfterReturning(pointcut = "anyMethod() && withStepAnnotation()", returning = "result")
    public void stepStop(final JoinPoint joinPoint, final Object result) {
        getLifecycle().updateStep(step -> step.setStatus(Status.PASSED));
        getLifecycle().stopStep();
    }

    public String createTitle(final JoinPoint joinPoint) {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final Step step = methodSignature.getMethod().getAnnotation(Step.class);
        return step.value().isEmpty()
                ? getName(methodSignature.getName(), joinPoint.getArgs())
                : getTitle(step.value(), methodSignature.getName(), joinPoint.getThis(), joinPoint.getArgs());
    }

    private static Set<Parameter> getParameters(final MethodSignature signature, final Object... args) {
        return IntStream.range(0, args.length).mapToObj(index -> {
            final String name = signature.getParameterNames()[index];
            final String value = Objects.toString(args[index]);
            return new Parameter().setName(name).setValue(value);
        }).collect(Collectors.toSet());
    }

    /**
     * For tests only.
     */
    public static void setLifecycle(final Lifecycle allure) {
        lifecycle = allure;
    }

    public static Lifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }
}
