package io.qameta.allure.attachment;

import io.qameta.allure.Allure;
import io.qameta.allure.Lifecycle;

/**
 * @author charlie (Dmitry Baev).
 */
public class DefaultAttachmentProcessor implements AttachmentProcessor<AttachmentData> {

    private final Lifecycle lifecycle;

    public DefaultAttachmentProcessor() {
        this(Allure.getLifecycle());
    }

    public DefaultAttachmentProcessor(final Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void addAttachment(final AttachmentData attachmentData,
                              final AttachmentRenderer<AttachmentData> renderer) {
        final AttachmentContent content = renderer.render(attachmentData);
        lifecycle.addAttachment(
                attachmentData.getName(),
                content.getContentType(),
                content.getFileExtension()
        );
    }
}
