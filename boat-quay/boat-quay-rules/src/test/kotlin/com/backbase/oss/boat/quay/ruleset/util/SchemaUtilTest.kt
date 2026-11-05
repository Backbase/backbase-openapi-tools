package com.backbase.oss.boat.quay.ruleset.util

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SchemaUtilTest {

    @Test
    fun `typeNames reads type on OpenAPI 3_0 and types on OpenAPI 3_1`() {
        assertThat(SchemaUtil.typeNames(Schema<Any>().type("string"))).containsExactly("string")
        assertThat(SchemaUtil.typeNames(Schema<Any>().types(setOf("string", "null")))).containsExactlyInAnyOrder("string", "null")
        assertThat(SchemaUtil.typeNames(Schema<Any>())).isEmpty()
        assertThat(SchemaUtil.typeNames(null)).isEmpty()
    }

    @Test
    fun `resolve follows refs and merges allOf`() {
        val components = Components()
            .addSchemas("Id", Schema<Any>().type("object").addProperty("id", Schema<Any>().type("string")).addRequiredItem("id"))
            .addSchemas("Base", Schema<Any>().type("object").addProperty("name", Schema<Any>().type("string")))
            .addSchemas(
                "Contact", Schema<Any>().addAllOfItem(Schema<Any>().`$ref`("#/components/schemas/Id"))
                    .addAllOfItem(Schema<Any>().`$ref`("#/components/schemas/Base"))
                    .addAllOfItem(Schema<Any>().addProperty("createdAt", Schema<Any>().type("string")))
            )

        val resolved = SchemaUtil.resolve(Schema<Any>().`$ref`("#/components/schemas/Contact"), components)

        assertThat(resolved?.properties?.keys).containsExactly("id", "name", "createdAt")
        assertThat(resolved?.required).containsExactly("id")
        assertThat(SchemaUtil.typeNames(resolved)).containsExactly("object")
    }

    @Test
    fun `resolve stops at circular and unknown refs`() {
        val components = Components()
            .addSchemas("Loop", Schema<Any>().`$ref`("#/components/schemas/Loop"))

        val loop = SchemaUtil.resolve(Schema<Any>().`$ref`("#/components/schemas/Loop"), components)
        val unknown = Schema<Any>().`$ref`("#/components/schemas/Unknown")

        assertThat(loop?.`$ref`).isEqualTo("#/components/schemas/Loop")
        assertThat(SchemaUtil.resolve(unknown, components)).isSameAs(unknown)
    }
}
