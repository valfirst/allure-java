package io.qameta.allure.writer;

import io.qameta.allure.model3.TestResult;

/**
 * @author charlie (Dmitry Baev).
 */
public interface ResultsWriter {

    void writeResult(TestResult result);

    AttachmentContentWriter writeAttachment(String fileName);

}
