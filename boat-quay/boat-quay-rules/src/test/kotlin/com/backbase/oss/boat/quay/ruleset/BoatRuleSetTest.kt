package com.backbase.oss.boat.quay.ruleset

import io.swagger.models.Swagger
import org.junit.jupiter.api.Test
import org.zalando.zally.rule.api.Check
import org.zalando.zally.rule.api.Rule
import org.zalando.zally.rule.api.Severity
import org.zalando.zally.rule.api.Violation

class BoatRuleSetTest {

    /** TestRule used for testing RulesPolicy */
    @Rule(
        ruleSet = BoatRuleSet::class,
        id = "TestRule",
        severity = Severity.MUST,
        title = "Test Rule"
    )
    class TestRule(val result: Violation?) {

        @Suppress("UNUSED_PARAMETER")
        @Check(severity = Severity.MUST)
        fun validate(swagger: Swagger): Violation? = result
    }


    @Test
    fun testUrlFiltering() {
        val ruleSet = BoatRuleSet()
        val url = ruleSet.url(rule());
        assert(url.isAbsolute)

    }

    private fun rule() = TestRule(null).javaClass.getAnnotation(Rule::class.java)
}