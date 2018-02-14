package io.qameta.allure.testdata

import io.qameta.allure.Lifecycle
import io.qameta.allure.aspect.AttachmentsAspects
import io.qameta.allure.aspect.StepsAspects
import io.qameta.allure.model3.TestResult
import io.qameta.allure.spock.AllureSpock
import io.qameta.allure.test.InMemoryResultsWriter
import org.junit.runner.notification.RunNotifier
import org.spockframework.runtime.JUnitDescriptionGenerator
import org.spockframework.runtime.RunContext
import org.spockframework.runtime.SpecInfoBuilder
import org.spockframework.runtime.model.SpecInfo


/**
 * Created on 14.06.2017
 *
 * @author Yuri Kudryavtsev
 *         skype: yuri.kudryavtsev.indeed
 *         email: yuri.kudryavtsev@indeed-id.com
 */

class AllureSpockRunner {

    private final static NOTIFIER = new RunNotifier()

    static List<TestResult> run(Class clazz) {
        InMemoryResultsWriter results = new InMemoryResultsWriter()
        Lifecycle lifecycle = new Lifecycle(results)

        StepsAspects.setLifecycle(lifecycle)
        AttachmentsAspects.setLifecycle(lifecycle)

        SpecInfo spec = new SpecInfoBuilder(clazz).build()
        spec.addListener(new AllureSpock(lifecycle))
        new JUnitDescriptionGenerator(spec).describeSpecMethods()
        new JUnitDescriptionGenerator(spec).describeSpec()
        RunContext.get().createSpecRunner(spec, NOTIFIER).run()

        return results.getAllTestResults()
    }
}
