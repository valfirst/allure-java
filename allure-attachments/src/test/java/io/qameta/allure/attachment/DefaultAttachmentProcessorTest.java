package io.qameta.allure.attachment;

import io.qameta.allure.Lifecycle;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import org.junit.Test;

import static io.qameta.allure.attachment.testdata.TestData.randomAttachmentContent;
import static io.qameta.allure.attachment.testdata.TestData.randomHttpRequestAttachment;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author charlie (Dmitry Baev).
 */
public class DefaultAttachmentProcessorTest {

    @SuppressWarnings("unchecked")
    @Test
    public void shouldProcessAttachments() throws Exception {
        final HttpRequestAttachment attachment = randomHttpRequestAttachment();
        final Lifecycle lifecycle = mock(Lifecycle.class);
        final AttachmentRenderer<AttachmentData> renderer = mock(AttachmentRenderer.class);
        final AttachmentContent content = randomAttachmentContent();
        doReturn(content)
                .when(renderer)
                .render(attachment);

        new DefaultAttachmentProcessor(lifecycle)
                .addAttachment(attachment, renderer);

        verify(renderer, times(1)).render(attachment);
        verify(lifecycle, times(1))
                .addAttachment(
                        eq(attachment.getName()),
                        eq(content.getContentType()),
                        eq(content.getFileExtension())
                );
    }
}