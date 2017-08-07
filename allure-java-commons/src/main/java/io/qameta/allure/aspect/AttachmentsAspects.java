package io.qameta.allure.aspect;

import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import io.qameta.allure.Lifecycle;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static io.qameta.allure.util.AspectUtils.getParametersMap;
import static io.qameta.allure.util.NamingUtils.processNameTemplate;

/**
 * Aspects (AspectJ) for handling {@link Attachment}.
 *
 * @author Dmitry Baev charlie@yandex-team.ru
 * Date: 24.10.13
 */
@Aspect
public class AttachmentsAspects {

    private static Lifecycle lifecycle;

    public static Lifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }

    /**
     * Sets lifecycle for aspect. Usually used in tests.
     *
     * @param lifecycle allure lifecycle to set.
     */
    public static void setLifecycle(final Lifecycle lifecycle) {
        AttachmentsAspects.lifecycle = lifecycle;
    }

    /**
     * Process data returned from method annotated with {@link Attachment}.
     * If returned data is not a byte array, then use toString() method, and get bytes from it.
     *
     * @param joinPoint the join point to process.
     * @param result    the returned value.
     */
    @AfterReturning(pointcut = "@annotation(io.qameta.allure.Attachment) && execution(* *(..))", returning = "result")
    public void attachment(final JoinPoint joinPoint, final Object result) {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final Attachment attachment = methodSignature.getMethod()
                .getAnnotation(Attachment.class);
        final byte[] bytes = (result instanceof byte[]) ? (byte[]) result : Objects.toString(result)
                .getBytes(StandardCharsets.UTF_8);

        final String name = attachment.value().isEmpty()
                ? methodSignature.getName()
                : processNameTemplate(attachment.value(), getParametersMap(methodSignature, joinPoint.getArgs()));
        getLifecycle()
                .addAttachment(name, attachment.type(), attachment.fileExtension())
                .withContent(bytes);
    }
}
