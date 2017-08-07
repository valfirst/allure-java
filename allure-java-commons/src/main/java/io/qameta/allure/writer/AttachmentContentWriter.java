package io.qameta.allure.writer;

import java.io.InputStream;

/**
 * @author charlie (Dmitry Baev).
 */
public interface AttachmentContentWriter {

    void withContent(InputStream content);

    void withContent(byte[] content);

    void withContent(String content);

}
