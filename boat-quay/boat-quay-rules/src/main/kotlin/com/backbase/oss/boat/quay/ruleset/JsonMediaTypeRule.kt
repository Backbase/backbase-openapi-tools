package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.util.MediaTypeUtil
import com.backbase.oss.boat.quay.ruleset.util.PointerUtil
import org.zalando.zally.rule.api.*

@Rule(
    ruleSet = BoatRuleSet::class,
    id = "B019",
    severity = Severity.SHOULD,
    title = "Use application/json without parameters"
)
class JsonMediaTypeRule {

    /**
     * `application/json` is always UTF-8, so `application/json; charset=utf-8` adds nothing.
     */
    @Check(severity = Severity.SHOULD)
    fun validate(context: Context): List<Violation> =
        context.validatePaths { (path, pathItem) ->
            pathItem?.readOperationsMap().orEmpty().flatMap { (method, operation) ->
                val operationPointer = PointerUtil.pathPointer(path, method.name.lowercase())
                val requestTypes = operation.requestBody?.content.orEmpty().keys
                    .map { it to PointerUtil.pointer(operationPointer, "requestBody", "content", it) }
                val responseTypes = operation.responses.orEmpty().flatMap { (status, response) ->
                    response.content.orEmpty().keys
                        .map { it to PointerUtil.pointer(operationPointer, "responses", status, "content", it) }
                }
                (requestTypes + responseTypes)
                    .filter { (type, _) -> MediaTypeUtil.isApplicationJson(type) && MediaTypeUtil.hasParameters(type) }
                    .map { (type, pointer) -> context.violation("Use application/json instead of $type", pointer) }
            }
        }
}
