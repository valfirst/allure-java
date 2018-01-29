package io.qameta.allure.selenide;

import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.logevents.LogEvent;
import com.codeborne.selenide.logevents.LogEventListener;
import io.qameta.allure.Allure;
import io.qameta.allure.Lifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.util.ResultsUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.nio.charset.StandardCharsets;

import static io.qameta.allure.util.ResultsUtils.getStackTraceAsString;

/**
 * @author Artem Eroshenko.
 */
public class AllureSelenide implements LogEventListener {

    private final Lifecycle lifecycle;

    public AllureSelenide() {
        this(Allure.getLifecycle());
    }

    public AllureSelenide(final Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void onEvent(final LogEvent event) {
        lifecycle.startStep(new StepResult()
                .setName(event.toString())
                .setStatus(Status.PASSED));

        lifecycle.updateStep(stepResult -> stepResult.setStart(stepResult.getStart() - event.getDuration()));

        if (LogEvent.EventStatus.FAIL.equals(event.getStatus())) {
            lifecycle.addAttachment("Screenshot", "image/png", "png").withContent(getScreenshotBytes());
            lifecycle.addAttachment("Page source", "text/html", "html").withContent(getPageSourceBytes());
            lifecycle.updateStep(stepResult -> {
                stepResult.setStatus(ResultsUtils.getStatus(event.getError()).orElse(Status.BROKEN));
                stepResult.setStatusMessage(event.getError() != null ? event.getError().getMessage() : null);
                stepResult.setStatusTrace(getStackTraceAsString(event.getError()));
            });
        }
        lifecycle.stopStep();
    }

    private static byte[] getScreenshotBytes() {
        return ((TakesScreenshot) WebDriverRunner.getWebDriver()).getScreenshotAs(OutputType.BYTES);
    }

    private static byte[] getPageSourceBytes() {
        return WebDriverRunner.getWebDriver().getPageSource().getBytes(StandardCharsets.UTF_8);
    }
}
