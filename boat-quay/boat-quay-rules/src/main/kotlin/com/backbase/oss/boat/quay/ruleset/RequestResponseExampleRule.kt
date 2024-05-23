package com.backbase.oss.boat.quay.ruleset

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import org.zalando.zally.rule.api.*

@Rule(
    ruleSet = BoatRuleSet::class,
    id = "B014",
    severity = Severity.HINT,
    title = "The request body & response example should contain all properties"
)
class RequestResponseExampleRule {

    /**
     * Validate if the example contain at least one response example with all defined properties in a schema
     * It will help to validate full response. Not just the required fields.
     * Check only 2xx responses.
     * @param context the context to validate
     * @return list of identified violations
     */

    @Check(severity = Severity.HINT)
    fun checkResponseExampleFulfill(context: Context): List<Violation> =
        context.validateOperations { (method, operation) ->
            requestBodyExampleViolations(operation, context, method)
                .plus(responseExampleViolations(operation, context, method))
        }

    private fun requestBodyExampleViolations(
        operation: Operation?,
        context: Context,
        method: PathItem.HttpMethod
    ): List<Violation> {
        return when {
            (method == PathItem.HttpMethod.POST || method == PathItem.HttpMethod.PUT) -> {
                operation?.requestBody?.content.orEmpty()
                    .map { (type, content) ->
                        findMissPropsInExample(content).map { missProps ->
                            context.violation(
                                "Not defined value(s) (`${missProps.second}`) of example(`${missProps.first}`) for request body of ${operation?.operationId}:${method.name} of $type",
                                content
                            )
                        }
                    }
                    .flatten()
            }

            else -> emptyList()
        }
    }

    private fun responseExampleViolations(
        operation: Operation?,
        context: Context,
        method: PathItem.HttpMethod
    ) = operation?.responses.orEmpty()
        .filter { (responseCode, _) ->
            responseCode.startsWith("2")
        }
        .map { (status, response) ->
            response.content.orEmpty()
                .map { (type, content) ->
                    findMissPropsInExample(content).map { missProps ->
                        context.violation(
                            "Not defined value(s) (`${missProps.second}`) of example(`${missProps.first}`) for ${operation?.operationId}:${method.name}:$status of $type",
                            content
                        )
                    }
                }
                .flatten()
        }
        .flatten()

    private val objectMapper = ObjectMapper()

    private fun findMissPropsInExample(content: MediaType): List<Pair<String, List<String>>> {
        val properties = when {
            content.schema?.properties != null -> content.schema?.properties
            content.schema?.items?.properties != null -> content.schema?.items?.properties
            else -> return emptyList()
        }
        var examples = content.examples.orEmpty()
        if (examples.isEmpty()) {
            examples = mapOf("example" to Example().value(content.example))
        }
        val missedExampleProps = examples.map { (name, exampleObject) ->
            val jsonObject =
                when (val value = exampleObject?.value) {
                    null -> JsonNodeFactory.instance.objectNode()
                    is String -> objectMapper.valueToTree(
                        value
                    )

                    else -> value as JsonNode
                }
            val noExampleProps = properties!!.map { (propName, _) ->
                hasExample(propName, properties[propName], jsonObject)
            }.flatten()
            Pair(name, noExampleProps)
        }
        return when {
            missedExampleProps.any { ep -> ep.second.isEmpty() } -> emptyList()
            else -> listOf(missedExampleProps.first())
        }
    }

    private fun hasExample(propertyName: String, property: Schema<Any>?, jsonObject: JsonNode): List<String> {
        return hasExample(null, propertyName, property, jsonObject)
    }

    private fun hasExample(
        parentName: String?,
        propertyName: String,
        property: Schema<Any>?,
        jsonObject: JsonNode
    ): List<String> {
        val fieldValue = jsonObject.findValue(propertyName) ?: return listOf(missedProperty(parentName, propertyName))
        return when {
            "array" == property?.type && fieldValue.isArray -> {
                when {
                    property.items.type == "object" -> {
                        property.items?.properties?.map { prop -> arrayTypeCheck(propertyName, prop, fieldValue) }!!.flatten()
                    }
                    else -> {
                        emptyList()
                    }
                }
            }

            "object" == property?.type && fieldValue.isObject -> {
                when {
                    property.additionalProperties != null -> {
                        if (fieldValue.isEmpty) {
                            listOf("additionalProperties")
                        } else {
                            emptyList()
                        }
                    }

                    else -> {
                        property.properties?.map { prop -> objectTypeCheck(propertyName, prop, fieldValue) }!!.flatten()
                    }
                }
            }

            else -> {
                when {
                    fieldValue.isArray || fieldValue.isObject || fieldValue.toString().isBlank() -> {
                        listOf(missedProperty(parentName, propertyName))
                    }

                    else -> {
                        emptyList();
                    }
                }
            }
        }
    }

    private fun arrayTypeCheck(parentName: String?, prop: Map.Entry<String, Schema<Any>>, fieldValue: JsonNode) =
        fieldValue.map { e -> hasExample(parentName, prop.key, prop.value, e) }.flatten()

    private fun objectTypeCheck(parentName: String?, prop: Map.Entry<String, Schema<Any>>, fieldValue: JsonNode) =
        hasExample(parentName, prop.key, prop.value, fieldValue)

    private fun missedProperty(parentName: String?, propertyName: String): String {
        return when {
            parentName == null -> propertyName
            else -> "$parentName.$propertyName"
        }
    }

}
