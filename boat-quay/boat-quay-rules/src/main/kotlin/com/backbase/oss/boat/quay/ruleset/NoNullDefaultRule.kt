package com.backbase.oss.boat.quay.ruleset

import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.zalando.zally.core.EMPTY_JSON_POINTER
import org.zalando.zally.core.plus
import org.zalando.zally.core.toEscapedJsonPointer
import org.zalando.zally.rule.api.*

@Rule(
    ruleSet = BoatRuleSet::class,
    id = "B025",
    severity = Severity.MUST,
    title = "A schema can only default to null when it allows null"
)
class NoNullDefaultRule {

    /**
     * Reports schemas with `default: null` that do not allow null: no `nullable: true` (OpenAPI 3.0) and no `null` in
     * `type` (OpenAPI 3.1). Code generators reject such specs, for example with "default is not of type `object`".
     *
     * Reads the document as written: the model in the context has its `allOf` schemas merged, which drops their
     * `default`, and Zally's own document tree drops every `null` value.
     * @param context the context to validate
     * @return list of identified violations
     */
    @Check(Severity.MUST)
    fun validate(context: Context): List<Violation> {
        if (!context.isOpenAPI3()) return emptyList()

        return schemas(YAML.readTree(context.source))
            .filter { (schema, _) -> schema.path("default").isNull && !allowsNull(schema) }
            .map { (_, pointer) ->
                context.violation(
                    "`default: null` is only valid when the schema allows null: remove the default, or make the " +
                        "schema nullable",
                    pointer
                )
            }
    }

    private fun allowsNull(schema: JsonNode): Boolean {
        val type = schema.path("type")
        return schema.path("nullable").asBoolean(false) ||
            type.asText() == "null" ||
            type.any { it.asText() == "null" }
    }

    /**
     * Every schema in the document with its location. A `$ref` is not a schema of its own: the schema it points to
     * decides whether null is allowed.
     */
    private fun schemas(document: JsonNode): List<Pair<JsonNode, JsonPointer>> {
        val found = mutableListOf<Pair<JsonNode, JsonPointer>>()

        fun schema(node: JsonNode, at: JsonPointer) {
            if (!node.isObject || node.has("\$ref")) return
            found += node to at
            SUB_SCHEMAS.forEach { node.child(it, at, ::schema) }
            SUB_SCHEMA_COLLECTIONS.forEach { node.children(it, at, ::schema) }
        }

        fun content(node: JsonNode, at: JsonPointer) =
            node.children("content", at) { mediaType, mediaTypeAt -> mediaType.child("schema", mediaTypeAt, ::schema) }

        fun parameter(node: JsonNode, at: JsonPointer) {
            node.child("schema", at, ::schema)
            content(node, at)
        }

        fun response(node: JsonNode, at: JsonPointer) {
            content(node, at)
            node.children("headers", at, ::parameter)
        }

        fun operation(node: JsonNode, at: JsonPointer) {
            node.children("parameters", at, ::parameter)
            node.child("requestBody", at, ::content)
            node.children("responses", at, ::response)
        }

        fun pathItem(node: JsonNode, at: JsonPointer) {
            node.children("parameters", at, ::parameter)
            HTTP_METHODS.forEach { node.child(it, at, ::operation) }
        }

        document.child("components", EMPTY_JSON_POINTER) { components, at ->
            components.children("schemas", at, ::schema)
            components.children("parameters", at, ::parameter)
            components.children("headers", at, ::parameter)
            components.children("requestBodies", at, ::content)
            components.children("responses", at, ::response)
            components.children("pathItems", at, ::pathItem)
        }
        document.children("paths", EMPTY_JSON_POINTER, ::pathItem)
        document.children("webhooks", EMPTY_JSON_POINTER, ::pathItem)
        return found
    }

    private fun JsonNode.child(name: String, at: JsonPointer, visit: (JsonNode, JsonPointer) -> Unit) {
        get(name)?.let { visit(it, at + name.toEscapedJsonPointer()) }
    }

    /** Visits the values of the [name] object, or the elements of the [name] array. */
    private fun JsonNode.children(name: String, at: JsonPointer, visit: (JsonNode, JsonPointer) -> Unit) {
        val node = get(name) ?: return
        val nodeAt = at + name.toEscapedJsonPointer()
        when {
            node.isObject -> node.properties().forEach { (key, value) -> visit(value, nodeAt + key.toEscapedJsonPointer()) }
            node.isArray -> node.forEachIndexed { index, value -> visit(value, nodeAt + index.toString().toEscapedJsonPointer()) }
        }
    }

    private companion object {
        val YAML = ObjectMapper(YAMLFactory())
        val HTTP_METHODS = listOf("get", "put", "post", "delete", "options", "head", "patch", "trace")
        val SUB_SCHEMAS = listOf(
            "items", "additionalProperties", "not", "if", "then", "else", "contains", "propertyNames",
            "unevaluatedItems", "unevaluatedProperties"
        )
        val SUB_SCHEMA_COLLECTIONS = listOf(
            "properties", "patternProperties", "dependentSchemas", "\$defs", "allOf", "anyOf", "oneOf", "prefixItems"
        )
    }
}
