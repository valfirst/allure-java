package io.qameta.allure.testng;

import io.qameta.allure.model.Parameter;
import org.testng.ITestNGMethod;

import java.util.Set;

/**
 * An example of custom listener that disables history.
 *
 * @author charlie (Dmitry Baev).
 */
public class CustomListener extends AllureTestNg2 {

    @Override
    protected String getHistoryId(ITestNGMethod method, Set<Parameter> parameters) {
        return null;
    }
}
