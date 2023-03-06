package com.backbase.oss.boat.quay.ruleset

import com.fasterxml.jackson.core.JsonPointer
import com.typesafe.config.Config
import org.zalando.zally.rule.api.*
import java.nio.file.FileVisitOption
import java.util.Optional

@Rule(
        ruleSet = BoatRuleSet::class,
        id = "B013",
        severity = Severity.MUST,
        title = "Check access control is properly defined."
)
class EndpointAccessControlDefinedRule(config: Config) {

    @Check(Severity.MUST)
    fun validate(context: Context): List<Violation>  {

        fun notEmpty(s: Any?) : Boolean {
            return s != null && !s.toString().equals("")
        }

        val violations = mutableListOf<Violation>()
        context.api.paths.orEmpty().values
                .flatMap { it?.readOperations().orEmpty() }
                .forEach {
                    if (it.extensions == null) {
                        violations.add(
                                context.violation("Access Control not defined: ${it.operationId}", it))
                    } else {
                        val acEnabledSet: Boolean = notEmpty(it.extensions["x-BbAccessControl"])
                        val acResourceSet: Boolean = notEmpty(it.extensions["x-BbAccessControl-resource"])
                        val acFunctionSet: Boolean = notEmpty(it.extensions["x-BbAccessControl-function"])
                        val acPrivilegeSet: Boolean = notEmpty(it.extensions["x-BbAccessControl-privilege"])
                        val acExplicitlyDisabled: Boolean = "false" == it.extensions["x-BbAccessControl"].toString()
                        if (acExplicitlyDisabled) {
                            if (acResourceSet || acFunctionSet || acPrivilegeSet) {
                                violations.add(
                                        context.violation("AC both disabled and defined: ${it.operationId}", it))
                            }
                        } else if (!acEnabledSet) {
                            if (!acResourceSet || !acFunctionSet || !acPrivilegeSet) {
                                violations.add(
                                        context.violation("AC info not complete: ${it.extensions}", it))
                            }
                        }
                    }
                }
        return violations
    }

}
