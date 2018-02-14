package io.qameta.allure;

import io.qameta.allure.model3.Label;
import io.qameta.allure.model3.TestResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.function.Consumer;

import static io.qameta.allure.testdata.TestData.randomLabel;
import static io.qameta.allure.testdata.TestData.randomLink;
import static io.qameta.allure.testdata.TestData.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureTest {

    private Lifecycle lifecycle;

    @Before
    public void setUp() throws Exception {
        lifecycle = mock(Lifecycle.class);
        Allure.setLifecycle(lifecycle);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddLabels() throws Exception {
        Label first = randomLabel();
        Label second = randomLabel();
        Label third = randomLabel();

        Allure.addLabels(first, second);

        ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(lifecycle, times(1)).updateTest(captor.capture());

        Consumer consumer = captor.getValue();
        TestResult result = new TestResult();
        result.getLabels().add(third);
        consumer.accept(result);

        assertThat(result.getLabels())
                .hasSize(3)
                .containsExactlyInAnyOrder(first, third, second);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddLinks() throws Exception {
        io.qameta.allure.model3.Link first = randomLink();
        io.qameta.allure.model3.Link second = randomLink();
        io.qameta.allure.model3.Link third = randomLink();

        Allure.addLinks(first, second);

        ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(lifecycle, times(1)).updateTest(captor.capture());

        Consumer consumer = captor.getValue();
        TestResult result = new TestResult();
        result.getLinks().add(third);
        consumer.accept(result);

        assertThat(result.getLinks())
                .hasSize(3)
                .containsExactlyInAnyOrder(third, first, second);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddDescription() throws Exception {
        String description = randomString();

        Allure.addDescription(description);

        ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(lifecycle, times(1)).updateTest(captor.capture());

        Consumer consumer = captor.getValue();
        TestResult result = new TestResult().setDescription(randomString());
        consumer.accept(result);

        assertThat(result)
                .isNotNull()
                .hasFieldOrPropertyWithValue("description", description);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddDescriptionHtml() throws Exception {
        String description = randomString();

        Allure.addDescriptionHtml(description);

        ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(lifecycle, times(1)).updateTest(captor.capture());

        Consumer consumer = captor.getValue();
        TestResult result = new TestResult().setDescriptionHtml(randomString());
        consumer.accept(result);

        assertThat(result)
                .isNotNull()
                .hasFieldOrPropertyWithValue("descriptionHtml", description);
    }
}