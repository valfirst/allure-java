package io.qameta.allure.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author charlie (Dmitry Baev).
 */
public class FileSystemAttachmentContentWriter implements AttachmentContentWriter {

    public static final Logger LOGGER = LoggerFactory.getLogger(FileSystemAttachmentContentWriter.class);

    private final Path outputFile;

    public FileSystemAttachmentContentWriter(final Path outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public void withContent(final InputStream content) {
        try (InputStream is = content) {
            Files.copy(is, outputFile);
        } catch (IOException e) {
            LOGGER.error("Could not write stream attachment content", e);
        }
    }

    @Override
    public void withContent(final byte[] content) {
        try (InputStream is = new ByteArrayInputStream(content)) {
            Files.copy(is, outputFile);
        } catch (IOException e) {
            LOGGER.error("Could not write byte array attachment content", e);
        }
    }

    @Override
    public void withContent(final String content) {
        try (InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            Files.copy(is, outputFile);
        } catch (IOException e) {
            LOGGER.error("Could not write string attachment content", e);
        }
    }

}
