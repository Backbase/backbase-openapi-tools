package com.backbase.oss.boat.quay.ruleset.util

import com.fasterxml.jackson.core.JsonPointer
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import org.zalando.zally.core.toJsonPointer
import java.util.Collections
import java.util.IdentityHashMap

object SchemaUtil {

    private const val SCHEMA_REF_PREFIX = "#/components/schemas/"

    /**
     * OpenAPI 3.0 documents carry the type in `type`, OpenAPI 3.1 documents in `types`.
     */
    fun typeNames(schema: Schema<*>?): Set<String> = when {
        schema == null -> emptySet()
        schema.type != null -> setOf(schema.type)
        else -> schema.types.orEmpty()
    }

    fun hasType(schema: Schema<*>?, type: String): Boolean = type in typeNames(schema)

    fun isArray(schema: Schema<*>?): Boolean = hasType(schema, "array") || schema?.items != null

    fun isObject(schema: Schema<*>?): Boolean = hasType(schema, "object") || !schema?.properties.isNullOrEmpty()

    class NamedProperty(val name: String, val schema: Schema<*>, val pointer: JsonPointer)

    /**
     * Every named property in the document, from components first and then from inline schemas in paths.
     * A property schema shared by several parents, as after resolving references, is reported once per name.
     */
    fun namedProperties(api: OpenAPI): List<NamedProperty> {
        val visited = Collections.newSetFromMap(IdentityHashMap<Schema<*>, Boolean>())
        val reported = IdentityHashMap<Schema<*>, MutableSet<String>>()
        val properties = mutableListOf<NamedProperty>()

        fun visit(schema: Schema<*>?, pointer: JsonPointer) {
            if (schema == null || !visited.add(schema)) {
                return
            }
            schema.properties.orEmpty().forEach { (name, property) ->
                val propertyPointer = PointerUtil.pointer(pointer, "properties", name)
                if (reported.getOrPut(property) { mutableSetOf() }.add(name)) {
                    properties += NamedProperty(name, property, propertyPointer)
                }
                visit(property, propertyPointer)
            }
            visit(schema.items, PointerUtil.pointer(pointer, "items"))
            visit(schema.not, PointerUtil.pointer(pointer, "not"))
            visit(schema.additionalProperties as? Schema<*>, PointerUtil.pointer(pointer, "additionalProperties"))
            mapOf("allOf" to schema.allOf, "anyOf" to schema.anyOf, "oneOf" to schema.oneOf).forEach { (keyword, members) ->
                members.orEmpty().forEachIndexed { index, member -> visit(member, PointerUtil.pointer(pointer, keyword, index)) }
            }
            if (schema.`$ref` != null) {
                visit(resolve(schema, api.components), pointer)
            }
        }

        rootSchemas(api).forEach { (schema, pointer) -> visit(schema, pointer) }
        return properties
    }

    private fun rootSchemas(api: OpenAPI): List<Pair<Schema<*>?, JsonPointer>> {
        val roots = mutableListOf<Pair<Schema<*>?, JsonPointer>>()
        val components = "/components".toJsonPointer()
        api.components?.schemas.orEmpty().forEach { (name, schema) ->
            roots += schema to PointerUtil.pointer(components, "schemas", name)
        }
        api.components?.parameters.orEmpty().forEach { (name, parameter) ->
            roots += parameter.schema to PointerUtil.pointer(components, "parameters", name, "schema")
        }
        api.components?.headers.orEmpty().forEach { (name, header) ->
            roots += header.schema to PointerUtil.pointer(components, "headers", name, "schema")
        }
        api.components?.requestBodies.orEmpty().forEach { (name, body) ->
            roots += contentSchemas(body.content, PointerUtil.pointer(components, "requestBodies", name))
        }
        api.components?.responses.orEmpty().forEach { (name, response) ->
            roots += responseSchemas(response, PointerUtil.pointer(components, "responses", name))
        }
        api.paths.orEmpty().forEach { (path, pathItem) ->
            roots += parameterSchemas(pathItem.parameters, PointerUtil.pathPointer(path))
            pathItem.readOperationsMap().forEach { (method, operation) ->
                val operationPointer = PointerUtil.pathPointer(path, method.name.lowercase())
                roots += parameterSchemas(operation.parameters, operationPointer)
                operation.requestBody?.let { roots += contentSchemas(it.content, PointerUtil.pointer(operationPointer, "requestBody")) }
                operation.responses.orEmpty().forEach { (status, response) ->
                    roots += responseSchemas(response, PointerUtil.pointer(operationPointer, "responses", status))
                }
            }
        }
        return roots
    }

    private fun parameterSchemas(parameters: List<Parameter>?, base: JsonPointer) =
        parameters.orEmpty().mapIndexed { index, parameter ->
            parameter.schema to PointerUtil.pointer(base, "parameters", index, "schema")
        }

    private fun contentSchemas(content: Map<String, MediaType>?, base: JsonPointer) =
        content.orEmpty().map { (type, mediaType) -> mediaType.schema to PointerUtil.pointer(base, "content", type, "schema") }

    private fun responseSchemas(response: ApiResponse, base: JsonPointer) =
        contentSchemas(response.content, base) + response.headers.orEmpty().map { (name, header) ->
            header.schema to PointerUtil.pointer(base, "headers", name, "schema")
        }

    /**
     * Follows `$ref` into `components.schemas` and merges the members of `allOf` into a single schema.
     * Zally resolves most references when parsing, but leaves them in place when that resolution fails.
     */
    fun resolve(schema: Schema<*>?, components: Components?): Schema<*>? =
        resolve(schema, components, emptySet())

    private fun resolve(schema: Schema<*>?, components: Components?, seenRefs: Set<String>): Schema<*>? {
        val ref = schema?.`$ref`
        if (ref != null) {
            if (!ref.startsWith(SCHEMA_REF_PREFIX) || ref in seenRefs) {
                return schema
            }
            return resolve(components?.schemas?.get(ref.removePrefix(SCHEMA_REF_PREFIX)), components, seenRefs + ref)
                ?: schema
        }
        if (schema?.allOf.isNullOrEmpty()) {
            return schema
        }
        val members = schema.allOf.mapNotNull { resolve(it, components, seenRefs) } + schema
        return Schema<Any>().apply {
            properties = members.mapNotNull { it.properties }.fold(LinkedHashMap()) { merged, properties ->
                merged.apply { putAll(properties) }
            }
            required = members.flatMap { it.required.orEmpty() }.distinct().ifEmpty { null }
            types = members.flatMap { typeNames(it) }.toSet().ifEmpty { null }
            items = members.firstNotNullOfOrNull { it.items }
        }
    }
}
