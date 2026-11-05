package com.backbase.oss.boat.quay.ruleset.util

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.Schema

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
