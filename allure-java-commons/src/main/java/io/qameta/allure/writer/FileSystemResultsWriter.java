package io.qameta.allure.writer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.model.TestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newOutputStream;

/**
 * @author charlie (Dmitry Baev).
 */
public class FileSystemResultsWriter implements ResultsWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemResultsWriter.class);

    private final Path outputDirectory;
    private final ObjectMapper mapper;

    public FileSystemResultsWriter(final Path outputDirectory) {
        this.outputDirectory = outputDirectory;
        this.mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public void writeResult(final TestResult result) {
        final String fileName = String.format("%s-allure.json", result.getUuid());
        try (OutputStream os = newOutputStream(createDirectories(outputDirectory).resolve(fileName))) {
            mapper.writeValue(os, result);
        } catch (IOException e) {
            LOGGER.error("Could not write result to directory {}", outputDirectory, e);
        }
    }

    @Override
    public AttachmentContentWriter writeAttachment(final String fileName) {
        try {
            final Path outputFile = createDirectories(outputDirectory).resolve(fileName);
            return new FileSystemAttachmentContentWriter(outputFile);
        } catch (IOException e) {
            LOGGER.error("Could not write attachment", e);
        }
        return new DummyAttachmentContentWriter();
    }

}
