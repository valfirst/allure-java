package io.qameta.allure.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author charlie (Dmitry Baev).
 */
public class DummyAttachmentContentWriter implements AttachmentContentWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DummyAttachmentContentWriter.class);

    @Override
    public void withContent(final InputStream content) {
        try {
            content.close();
        } catch (IOException e) {
            LOGGER.debug("Could not close attachment input stream", e);
        }
    }

    @Override
    public void withContent(final byte[] content) {
        //do nothing
    }

    @Override
    public void withContent(final String content) {
        //do nothing
    }
}
