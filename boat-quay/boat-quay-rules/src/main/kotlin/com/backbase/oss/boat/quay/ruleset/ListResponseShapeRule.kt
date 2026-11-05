package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.util.MediaTypeUtil
import com.backbase.oss.boat.quay.ruleset.util.PathUtil
import com.backbase.oss.boat.quay.ruleset.util.PointerUtil
import com.backbase.oss.boat.quay.ruleset.util.SchemaUtil
import com.backbase.oss.boat.quay.ruleset.util.WordUtil
import com.typesafe.config.Config
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.responses.ApiResponse
import org.zalando.zally.core.util.PatternUtil
import org.zalando.zally.rule.api.*

@Rule(
    ruleSet = BoatRuleSet::class,
    id = "B020",
    severity = Severity.SHOULD,
    title = "List responses are objects with an items array"
)
class ListResponseShapeRule(config: Config) {

    private val prefixes = config.getStringList("PrefixPathRule.validPathPrefixes")

    private val nonCollectionSegments = config.getStringList("${javaClass.simpleName}.nonCollectionSegments").toSet()

    /**
     * A response body that is an object can carry paging information and counts next to the `items`.
     */
    @Check(severity = Severity.SHOULD)
    fun validate(context: Context): List<Violation> {
        val components = context.api.components
        return context.validatePaths { (path, pathItem) ->
            val topLevelArrays = pathItem?.readOperationsMap().orEmpty().flatMap { (method, operation) ->
                operation.responses.orEmpty().filterKeys { it.startsWith("2") }.flatMap { (status, response) ->
                    jsonContent(response)
                        .filterValues { SchemaUtil.isArray(SchemaUtil.resolve(it.schema, components)) }
                        .map { (type, _) ->
                            context.violation(
                                "Response body should be an object, not an array",
                                PointerUtil.pathPointer(path, method.name.lowercase(), "responses", status, "content", type)
                            )
                        }
                }
            }
            val get = pathItem?.get
            val collectionViolations = if (get != null && isCollection(path)) {
                listViolations(path, get, components, context)
            } else {
                emptyList()
            }
            topLevelArrays + collectionViolations
        }
    }

    private fun listViolations(path: String, get: Operation, components: Components?, context: Context): List<Violation> {
        val withoutItems = jsonContent(get.responses?.get("200"))
            .filterValues { content ->
                val schema = SchemaUtil.resolve(content.schema, components)
                schema != null && !SchemaUtil.isArray(schema) &&
                    !SchemaUtil.isArray(SchemaUtil.resolve(schema.properties?.get("items"), components))
            }
            .map { (type, _) ->
                context.violation(
                    "List response should be an object with an items array",
                    PointerUtil.pathPointer(path, "get", "responses", "200", "content", type)
                )
            }
        val notFound = if (get.responses?.containsKey("404") == true && isTopLevelCollection(path)) {
            context.violation(
                "List endpoint should return an empty list instead of 404",
                PointerUtil.pathPointer(path, "get", "responses", "404")
            )
        } else {
            null
        }
        return withoutItems + listOfNotNull(notFound)
    }

    private fun jsonContent(response: ApiResponse?): Map<String, MediaType> =
        response?.content.orEmpty().filterKeys { MediaTypeUtil.isJson(it) }

    private fun isCollection(path: String): Boolean {
        val last = PathUtil.resourceSegments(path, prefixes).lastOrNull() ?: return false
        return !PatternUtil.isPathVariable(last) && last !in nonCollectionSegments &&
            WordUtil.isPlural(last.substringAfterLast("-"))
    }

    private fun isTopLevelCollection(path: String): Boolean =
        PathUtil.resourceSegments(path, prefixes).none { PatternUtil.isPathVariable(it) }
}
