package io.qameta.allure.test;

import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultType;
import io.qameta.allure.writer.AttachmentContentWriter;
import io.qameta.allure.writer.ResultsWriter;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author charlie (Dmitry Baev).
 */
public class InMemoryResultsWriter implements ResultsWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryResultsWriter.class);

    private final List<TestResult> results = new CopyOnWriteArrayList<>();

    private final Map<String, byte[]> attachments = new ConcurrentHashMap<>();

    @Override
    public void writeResult(final TestResult result) {
        results.add(result);
    }

    @Override
    public AttachmentContentWriter writeAttachment(final String fileName) {
        return new AttachmentContentWriter() {
            @Override
            public void withContent(final InputStream content) {
                try (InputStream is = content) {
                    final byte[] bytes = IOUtils.toByteArray(is);
                    attachments.put(fileName, bytes);
                } catch (IOException e) {
                    LOGGER.error("Could not write attachment content", e);
                }
            }
            @Override
            public void withContent(final byte[] content) {
                attachments.put(fileName, content);
            }
            @Override
            public void withContent(final String content) {
                attachments.put(fileName, content.getBytes(UTF_8));
            }
        };
    }

    public List<TestResult> getResults() {
        return results;
    }

    public List<TestResult> getAllTestResults() {
        return results.stream()
                .filter(testResult -> TestResultType.TEST.equals(testResult.getType()))
                .collect(Collectors.toList());
    }

    public List<TestResult> getAllSetUpResults() {
        return results.stream()
                .filter(testResult -> TestResultType.SET_UP.equals(testResult.getType()))
                .collect(Collectors.toList());
    }

    public List<TestResult> getAllTearDownResults() {
        return results.stream()
                .filter(testResult -> TestResultType.TEAR_DOWN.equals(testResult.getType()))
                .collect(Collectors.toList());
    }

    public Map<String, byte[]> getAttachments() {
        return attachments;
    }
}
