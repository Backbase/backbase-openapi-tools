package com.backbase.oss.boat.quay.ruleset

import com.typesafe.config.Config
import org.zalando.zally.core.toJsonPointer
import org.zalando.zally.rule.api.*

@Rule(
        ruleSet = BoatRuleSet::class,
        id = "B008",
        severity = Severity.SHOULD,
        title = "Check x-icon value in the info block"
)
class ApiDisplayIconRule {

    @Check(Severity.MUST)
    fun validate(context: Context): List<Violation>  =

        if(context.api.info.extensions == null || context.api.info.extensions["x-icon"] == null){
            listOf(context.violation("x-icon should be provided in the info block with the assigned value for the API", "/openapi/info/x-icon".toJsonPointer()))
        } else {
            emptyList()
        }

}
