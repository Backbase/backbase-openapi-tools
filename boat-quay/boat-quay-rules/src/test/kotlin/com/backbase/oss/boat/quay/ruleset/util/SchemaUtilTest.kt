package com.backbase.oss.boat.quay.ruleset.util

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.Schema
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.zalando.zally.core.DefaultContextFactory

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

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `namedProperties reports each property once with its location in the document`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /things:
                get:
                  parameters:
                    - name: filter
                      in: query
                      schema:
                        type: object
                        properties:
                          fromDate:
                            type: string
                  responses:
                    '200':
                      description: ok
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              items:
                                type: array
                                items:
                                  ${'$'}ref: '#/components/schemas/Thing'
            components:
              schemas:
                Timestamp:
                  type: string
                  format: date-time
                Thing:
                  allOf:
                    - type: object
                      properties:
                        name:
                          type: string
                    - type: object
                      properties:
                        createdAt:
                          ${'$'}ref: '#/components/schemas/Timestamp'
                        updatedAt:
                          ${'$'}ref: '#/components/schemas/Timestamp'
            """.trimIndent()
        )

        val properties = SchemaUtil.namedProperties(context.api).map { it.name to it.pointer.toString() }
        // Resolving an OpenAPI 3.0 document merges the allOf members of component schemas
        val thing = if (openApiVersion.startsWith("3.0")) {
            listOf("/components/schemas/Thing", "/components/schemas/Thing")
        } else {
            listOf("/components/schemas/Thing/allOf/0", "/components/schemas/Thing/allOf/1")
        }

        assertThat(properties).containsExactly(
            "name" to "${thing[0]}/properties/name",
            "createdAt" to "${thing[1]}/properties/createdAt",
            "updatedAt" to "${thing[1]}/properties/updatedAt",
            "fromDate" to "/paths/~1things/get/parameters/0/schema/properties/fromDate",
            "items" to "/paths/~1things/get/responses/200/content/application~1json/schema/properties/items"
        )
    }
}
