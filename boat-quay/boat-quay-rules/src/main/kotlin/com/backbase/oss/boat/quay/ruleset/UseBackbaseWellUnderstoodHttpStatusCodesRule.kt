package com.backbase.oss.boat.quay.ruleset

import com.typesafe.config.Config
import org.zalando.zally.rule.api.*
import io.swagger.v3.oas.models.PathItem

@Rule(
        ruleSet = BoatRuleSet::class,
        id = "B012",
        severity = Severity.SHOULD,
        title = "Use Well Understood HTTP Status Codes"
)
class UseBackbaseWellUnderstoodHttpStatusCodesRule(config: Config) {

        private val wellUnderstoodResponseCodesAndVerbs = config
                .getConfig("${javaClass.simpleName}.well_understood")
                .entrySet()
                .map { (key, config) ->
                    @Suppress("UNCHECKED_CAST")
                    key to config.unwrapped() as List<String>
                }
                .toMap()

        private val wellUnderstoodResponseCode = wellUnderstoodResponseCodesAndVerbs.keys


        /**
         * Validate that well-understood HTTP response codes are used properly
         * @param context the context to validate
         * @return list of identified violations
         */

        @Check(severity = Severity.SHOULD)
        fun checkWellUnderstoodResponseCodesUsage(context: Context): List<Violation> =
                context.validateOperations { (method, operation) ->
                    operation?.responses.orEmpty().filterNot { (status, _) ->
                        isAllowed(method, status)
                    }.map { (status, response) ->
                        if (wellUnderstoodResponseCode.contains(status)){
                            context.violation("$status is not allowed for this method, incorrect use of well known code", response)
                        }else{
                            context.violation("$status is not a well understood status code, should use well-understood HTTP status codes", response)
                        }

                    }
                }


        private fun isAllowed(method: PathItem.HttpMethod, statusCode: String): Boolean {
            val allowedMethods = wellUnderstoodResponseCodesAndVerbs[statusCode.lowercase()].orEmpty()
            return allowedMethods.contains(method.name) || allowedMethods.contains("ALL")
        }



}
