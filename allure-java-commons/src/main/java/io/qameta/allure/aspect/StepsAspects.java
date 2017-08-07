package io.qameta.allure.aspect;

import io.qameta.allure.Allure;
import io.qameta.allure.Lifecycle;
import io.qameta.allure.Step;
import io.qameta.allure.model.StepResult;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static io.qameta.allure.util.AspectUtils.getParameters;
import static io.qameta.allure.util.AspectUtils.getParametersMap;
import static io.qameta.allure.util.NamingUtils.processNameTemplate;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 * @author sskorol (Sergey Korol)
 */
@Aspect
public class StepsAspects {

    private static Lifecycle lifecycle;

    @SuppressWarnings("PMD.UnnecessaryLocalBeforeReturn")
    @Around("@annotation(io.qameta.allure.Step) && execution(* *(..))")
    public Object step(final ProceedingJoinPoint joinPoint) throws Throwable {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final Step step = methodSignature.getMethod().getAnnotation(Step.class);

        final String uuid = UUID.randomUUID().toString();
        final String name = Optional.of(step.value())
                .filter(StringUtils::isNoneEmpty)
                .map(value -> processNameTemplate(value, getParametersMap(methodSignature, joinPoint.getArgs())))
                .orElse(methodSignature.getName());

        final StepResult result = new StepResult()
                .setName(name)
                .setParameters(getParameters(methodSignature, joinPoint.getArgs()));
        getLifecycle().startStep(result);
        try {
            final Object proceed = joinPoint.proceed();
            getLifecycle().updateStep(Lifecycle.stepPassed());
            return proceed;
        } catch (Throwable e) {
            getLifecycle().updateStep(Lifecycle.stepFailed(e));
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
}
