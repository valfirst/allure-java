package io.qameta.allure.assertj;

import io.qameta.allure.Allure;
import io.qameta.allure.Lifecycle;
import io.qameta.allure.model3.Status;
import io.qameta.allure.model3.StepResult;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStackTraceAsString;
import static java.util.Objects.nonNull;

/**
 * @author charlie (Dmitry Baev).
 * @author sskorol (Sergey Korol).
 */
@SuppressWarnings("all")
@Aspect
public class AllureAssertJ {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureAssertJ.class);

    private static Lifecycle lifecycle;

    @Around("execution(* org.assertj.core.api.AbstractAssert+.*(..)) "
            + "|| execution(* org.assertj.core.api.Assertions.assertThat(..))")
    public Object step(final ProceedingJoinPoint joinPoint) throws Throwable {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final String name = joinPoint.getArgs().length > 0
                ? String.format("%s \'%s\'", methodSignature.getName(), arrayToString(joinPoint.getArgs()))
                : methodSignature.getName();
        final StepResult result = new StepResult()
                .setName(name);
        getLifecycle().startStep(result);
        try {
            final Object proceed = joinPoint.proceed();
            getLifecycle().updateStep(s -> s.setStatus(Status.PASSED));
            return proceed;
        } catch (Throwable e) {
            getLifecycle().updateStep(s -> s
                    .setStatus(getStatus(e).orElse(Status.BROKEN))
                    .setStatusMessage(e.getMessage())
                    .setStatusTrace(getStackTraceAsString(e)));
            throw e;
        } finally {
            getLifecycle().stopStep();
        }
    }

    /**
     * For tests only.
     *
     * @param allure allure lifecycle to set.
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

    private static String arrayToString(final Object... array) {
        return Stream.of(array)
                     .map(object -> nonNull(object) && object.getClass().isArray()
                             ? arrayToString((Object[]) object)
                             : Objects.toString(object))
                     .collect(Collectors.joining(" "));
    }
}
