package io.qameta.allure;

import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultType;
import io.qameta.allure.test.InMemoryResultsWriter;
import io.qameta.allure.testdata.TestData;
import io.qameta.allure.writer.AttachmentContentWriter;
import io.qameta.allure.writer.ResultsWriter;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static io.qameta.allure.Allure.addByteAttachmentAsync;
import static io.qameta.allure.Allure.addStreamAttachmentAsync;
import static io.qameta.allure.Allure.setLifecycle;
import static io.qameta.allure.testdata.TestData.randomString;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author sskorol (Sergey Korol).
 */
public class AttachmentsTest {

    @Test
    public void shouldAttachAsync() throws Exception {
        final ResultsWriter results = mock(ResultsWriter.class);
        AttachmentContentWriter contentWriter = mock(AttachmentContentWriter.class);
        when(results.writeAttachment(anyString())).thenReturn(contentWriter);

        String attachmentName = randomString();
        String contentType = randomString();
        String fileExtension = randomString();
        byte[] content = randomString().getBytes(StandardCharsets.UTF_8);

        final Lifecycle lifecycle = new Lifecycle(results);
        setLifecycle(lifecycle);

        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setType(TestResultType.TEST);

        lifecycle.startTest(result);

        CompletableFuture<byte[]> mp4 = addByteAttachmentAsync(
                attachmentName, contentType, fileExtension,
                getStreamWithTimeout(1, content)
        );

        lifecycle.stopTest();
        lifecycle.writeTest(uuid);

        ArgumentCaptor<TestResult> resultCaptor = ArgumentCaptor.forClass(TestResult.class);

        verify(results, times(1))
                .writeResult(resultCaptor.capture());

        assertThat(resultCaptor.getAllValues())
                .flatExtracting(TestResult::getAttachments)
                .extracting(Attachment::getName, Attachment::getContentType)
                .containsExactlyInAnyOrder(tuple(attachmentName, contentType));

        mp4.join();

        ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);

        verify(results, times(1))
                .writeAttachment(fileNameCaptor.capture());

        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(contentWriter, times(1))
                .withContent(contentCaptor.capture());

        assertThat(contentCaptor.getValue())
                .describedAs("Should write correct attachment content")
                .containsExactly(content);

        assertThat(fileNameCaptor.getValue())
                .endsWith(fileExtension);

        assertThat(resultCaptor.getAllValues())
                .describedAs("Attachment link should be the same")
                .flatExtracting(TestResult::getAttachments)
                .extracting(Attachment::getSource)
                .containsExactlyInAnyOrder(fileNameCaptor.getValue());
    }

    private Supplier<byte[]> getStreamWithTimeout(final long sec, byte[] content) {
        return () -> {
            try {
                TimeUnit.SECONDS.sleep(sec);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return content;
        };
    }
}
