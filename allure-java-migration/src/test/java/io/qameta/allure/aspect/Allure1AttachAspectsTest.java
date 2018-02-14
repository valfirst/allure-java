package io.qameta.allure.aspect;

import io.qameta.allure.Lifecycle;
import io.qameta.allure.model3.TestResult;
import io.qameta.allure.test.InMemoryResultsWriter;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Attachment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * eroshenkoam
 * 30.04.17
 */
public class Allure1AttachAspectsTest {

    private InMemoryResultsWriter results;

    private Lifecycle lifecycle;

    @Before
    public void initLifecycle() {
        results = new InMemoryResultsWriter();
        lifecycle = new Lifecycle(results);
        Allure1AttachAspects.setLifecycle(lifecycle);
    }

    @Test
    public void shouldSetupAttachmentTitleFromAnnotation() {
        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().setUuid(uuid);

        lifecycle.startTest(result);

        attachmentWithTitleAndType("parameter value");

        lifecycle.stopTest();
        lifecycle.writeTest(uuid);

        assertThat(results.getAllTestResults())
                .flatExtracting(TestResult::getAttachments)
                .extracting("name", "contentType")
                .containsExactly(tuple("attachment with parameter value", "text/plain"));

    }

    @Test
    public void shouldSetupAttachmentTitleFromMethodSignature() {
        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().setUuid(uuid);

        lifecycle.startTest(result);

        attachmentWithoutTitle();

        lifecycle.stopTest();
        lifecycle.writeTest(uuid);

        assertThat(results.getAllTestResults())
                .flatExtracting(TestResult::getAttachments)
                .extracting("name", "contentType")
                .containsExactly(tuple("attachmentWithoutTitle", ""));

    }

    @Test
    public void shouldProcessNullAttachment() throws Exception {
        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().setUuid(uuid);

        lifecycle.startTest(result);

        attachmentWithNullValue();

        lifecycle.stopTest();
        lifecycle.writeTest(uuid);

        assertThat(results.getAllTestResults())
                .flatExtracting(TestResult::getAttachments)
                .extracting("name", "contentType")
                .containsExactly(tuple("attachmentWithNullValue", ""));
    }

    @Attachment
    public byte[] attachmentWithNullValue() {
        return null;
    }

    @Attachment
    public byte[] attachmentWithoutTitle() {
        return new byte[]{};
    }

    @Attachment(value = "attachment with {0}", type = "text/plain")
    public byte[] attachmentWithTitleAndType(String parameter) {
        return new byte[]{};
    }
}
