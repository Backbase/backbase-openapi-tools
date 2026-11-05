package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.util.MediaTypeUtil
import com.backbase.oss.boat.quay.ruleset.util.PointerUtil
import com.backbase.oss.boat.quay.ruleset.util.SchemaUtil
import com.fasterxml.jackson.core.JsonPointer
import io.swagger.v3.oas.models.media.MediaType
import org.zalando.zally.rule.api.*

@Rule(
    ruleSet = BoatRuleSet::class,
    id = "B024",
    severity = Severity.SHOULD,
    title = "4xx responses use the SSDK error body"
)
class ErrorBodyShapeRule {

    /**
     * The SSDK error body has a string `message`, an optional string `key` and optional `errors` items.
     */
    @Check(severity = Severity.SHOULD)
    fun validate(context: Context): List<Violation> =
        context.validatePaths { (path, pathItem) ->
            pathItem?.readOperationsMap().orEmpty().flatMap { (method, operation) ->
                operation.responses.orEmpty()
                    .filterKeys { it.length == 3 && it.startsWith("4") }
                    .flatMap { (status, response) ->
                        response.content.orEmpty()
                            .filterKeys { MediaTypeUtil.isApplicationJson(it) }
                            .flatMap { (type, content) ->
                                val pointer = PointerUtil.pathPointer(path, method.name.lowercase(), "responses", status, "content", type)
                                errorBodyViolations(status, content, pointer, context)
                            }
                    }
            }
        }

    private fun errorBodyViolations(status: String, content: MediaType, pointer: JsonPointer, context: Context): List<Violation> {
        val components = context.api.components
        val properties = SchemaUtil.resolve(content.schema, components)?.properties.orEmpty()
        val message = SchemaUtil.resolve(properties["message"], components)
        val key = SchemaUtil.resolve(properties["key"], components)
        val errors = SchemaUtil.resolve(properties["errors"], components)
        val errorItem = SchemaUtil.resolve(errors?.items, components)
        return listOfNotNull(
            context.violation("$status response body should have a string message", pointer)
                .takeIf { !SchemaUtil.hasType(message, "string") },
            context.violation("$status response body key should be a string", pointer)
                .takeIf { key != null && !SchemaUtil.hasType(key, "string") },
            context.violation("$status response body errors should be an array of objects", pointer)
                .takeIf { errors != null && !(SchemaUtil.isArray(errors) && SchemaUtil.isObject(errorItem)) }
        )
    }
}
