package com.backbase.oss.boat.quay.ruleset

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import org.zalando.zally.rule.api.*
import java.util.*

@Rule(
        ruleSet = BoatRuleSet::class,
        id = "B014",
        severity = Severity.SHOULD,
        title = "The response example should contain all properties"
)
class RequestResponseExampleRule {

    /**
     * Validate if the example contain at least one response example with all defined properties in a schema
     * It will help to validate full response. Not just the required fields
     * @param context the context to validate
     * @return list of identified violations
     */

    @Check(severity = Severity.SHOULD)
    fun checkResponseExampleFulfill(context: Context): List<Violation> =
            context.validateOperations { (method, operation) ->
                operation?.responses.orEmpty()
                        .map { (status, response) ->
                            response.content.orEmpty()
                                    .filterNot { (_, content) ->
                                        hasDefinedAllParams(content)
                                    }.map { (type, content) ->
                                        context.violation("Fulfill the response example for ${method.name}:$status for $type to include all properties ", content)
                                    }
                        }.flatten()
            }

    private val objectMapper = ObjectMapper()

    private fun hasDefinedAllParams(content: MediaType): Boolean {
        val properties = content.schema?.properties ?: return true
        var examples = content.examples.orEmpty()
        if (examples.isEmpty() && content.example != null) {
            examples = mapOf("example" to content.example as Example)
        }
        return examples.any { (_, exampleObject) ->
            val value = exampleObject?.value
            properties.all { (name, _) ->
                val jsonObject = if (value is String) objectMapper.valueToTree(value) else value as JsonNode
                hasExample(name, properties[name], jsonObject)
            }
        }
    }


    private fun hasExample(propertyName: String, property: Schema<Any>?, jsonObject: JsonNode): Boolean {
        val fieldValue = jsonObject.findValue(propertyName)
        if (Objects.isNull(fieldValue)) {
            return false
        }
        return when (property?.type) {
            "array" -> fieldValue.isArray
                    && property.items?.properties?.all { prop -> arrayTypeCheck(prop, fieldValue) } ?: false

            "object" -> {
                if (fieldValue.isObject) {
                    if (property.additionalProperties != null) {
                        return !fieldValue.isEmpty
                    }
                    return property.properties?.all { prop -> objectTypeCheck(prop, fieldValue) } ?: false
                }
                return false
            }

            else -> fieldValue.toString().isNotBlank()
        }
    }

    private fun arrayTypeCheck(prop: Map.Entry<String, Schema<Any>>, fieldValue: JsonNode) = fieldValue.all { e -> hasExample(prop.key, prop.value, e) }

    private fun objectTypeCheck(prop: Map.Entry<String, Schema<Any>>, fieldValue: JsonNode) = hasExample(prop.key, prop.value, fieldValue)

}
