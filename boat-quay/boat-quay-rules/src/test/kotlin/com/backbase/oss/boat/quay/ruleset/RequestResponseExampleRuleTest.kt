package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.zalando.zally.core.DefaultContextFactory

class RequestResponseExampleRuleTest {

    private val cut = RequestResponseExampleRule()

    @Test
    fun `correct check if example fields are present`() {
        @Language("yml")
        val context = DefaultContextFactory().getOpenApiContext(
            {}.javaClass.getResource("/request-response-example-success-1.yaml").readText().trim()
        )

        val violations = cut.checkResponseExampleFulfill(context)

        ZallyAssertions
                .assertThat(violations)
                .isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = [EXAMPLE_TYPE_MISS_MATCH, EXAMPLE_INCORRECT_OBJECT, EXAMPLE_REQUEST_BODY_INCORRECT_OBJECT])
    fun `incorrect example check`(value: String) {
        val violations = cut.checkResponseExampleFulfill(DefaultContextFactory()
                .getOpenApiContext(value.trimIndent()))
        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = [EXAMPLE_NO_PROPERTIES, EXAMPLE_NO_SCHEMA])
    fun `incorrect example value`(value: String) {

        val violations = cut.checkResponseExampleFulfill(DefaultContextFactory()
                .getOpenApiContext(value.trimIndent()))

        ZallyAssertions
                .assertThat(violations)
                .isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = [EXAMPLE_OBJECT_WITHOUT_PROPERTIES, EXAMPLE_ARRAY_OF_OBJECTS_WITHOUT_PROPERTIES])
    fun `schema without properties does not crash`(value: String) {
        val violations = cut.checkResponseExampleFulfill(DefaultContextFactory()
            .getOpenApiContext(value.trimIndent()))

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = [EXAMPLE_STRING_ARRAY])
    fun `Array of Strings`(value: String) {

        val violations = cut.checkResponseExampleFulfill(DefaultContextFactory()
            .getOpenApiContext(value.trimIndent()))

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }

    @Test
    fun `correct check if example fields are present on OpenAPI 3_1`() {
        val context = DefaultContextFactory().getOpenApiContext(
            openApi31({}.javaClass.getResource("/request-response-example-success-1.yaml").readText().trim())
        )

        val violations = cut.checkResponseExampleFulfill(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [EXAMPLE_TYPE_MISS_MATCH, EXAMPLE_INCORRECT_OBJECT, EXAMPLE_REQUEST_BODY_INCORRECT_OBJECT,
            EXAMPLE_NO_PROPERTIES, EXAMPLE_NO_SCHEMA, EXAMPLE_OBJECT_WITHOUT_PROPERTIES,
            EXAMPLE_ARRAY_OF_OBJECTS_WITHOUT_PROPERTIES, EXAMPLE_STRING_ARRAY, EXAMPLE_REFERENCED_SCHEMA]
    )
    fun `OpenAPI 3_1 reports the same violations as OpenAPI 3_0`(value: String) {
        val violations30 = cut.checkResponseExampleFulfill(
            DefaultContextFactory().getOpenApiContext(value.trimIndent())
        )
        val violations31 = cut.checkResponseExampleFulfill(
            DefaultContextFactory().getOpenApiContext(openApi31(value.trimIndent()))
        )

        assertThat(violations31.map { it.description }).isEqualTo(violations30.map { it.description })
    }

    private fun openApi31(spec: String) = spec.replaceFirst("openapi: 3.0.3", "openapi: 3.1.0")

    companion object {

        @Language("YAML")
        const val EXAMPLE_TYPE_MISS_MATCH: String = """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            paths: 
              /foo:
                get:
                  description: Lorem Ipsum
                  operationId: foo
                  responses:
                    200:
                      description: Lorem Ipsum
                      content:
                        application/json:
                          schema:
                              type: object
                              properties:
                                name:
                                  type: string
                                  description: Portfolio's name.
                                alias:
                                  type: string
                                  description: Portfolio's alias.
                          example:
                            value:
                              name: aaa
                              alias: [a,d]
            """

        @Language("YAML")
        const val EXAMPLE_REQUEST_BODY_INCORRECT_OBJECT: String = """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /users:
                put:
                  requestBody:
                    content:
                      application/json:
                        schema:
                          type: array
                          items:
                            required:
                              - name
                            type: object
                            properties:
                                name:
                                  type: string
                                  description: Request name
                                  minLength: 3
                                  maxLength: 251
                                amountNumberAsString:
                                  description: The amount string in number format
                                  type: string
                                  format: number
                                  minimum: -999.99999
                                  maximum: 999.99999
                                amountNumberAsStringOptional:
                                  description: The amount string in number format
                                  type: string
                                  format: number
                                  minimum: -999.99999
                                  maximum: 999.99999
                                additions:
                                  type: object
                                  additionalProperties:
                                    type: string   
                        examples:
                            example1:
                              value:
                                  amountNumberAsStringOptional: 100.00 
                                  additions: 
                                    dd: vv
                  responses:
                        "204":
                          description: OK
            """

        @Language("YAML")
        const val EXAMPLE_INCORRECT_OBJECT: String =
                """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            paths: 
              /foo:
                get:
                  description: Lorem Ipsum
                  operationId: foo
                  responses:
                    200:
                      description: Lorem Ipsum
                      content:
                        application/json:
                          schema:
                              type: object
                              properties:
                                name:
                                  type: string
                                  description: Portfolio's name.
                                alias:
                                  type: string
                                  description: Portfolio's alias.
                          examples:
                              example1:
                                value:
                                  name: aaa
                                  alias: [a,b] 
                              example2:
                                value:
                                  name: 
                                    incorrectObject:
                                      a: 1
                                  
            """

        @Language("YAML")
        const val EXAMPLE_NO_SCHEMA: String = """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            paths: 
              /foo:
                get:
                  description: Lorem Ipsum
                  operationId: foo
                  responses:
                    200:
                      description: Lorem Ipsum
                      content:
                        application/json:
                          examples:
                              example1:
                                value:
                                  name: aaa
                                  alias: [a,d] 
                              example2:
                                value:
                                  name: 
                                    incorrectObject:
                                      a: 1
                                  
            """

        @Language("YAML")
        const val EXAMPLE_NO_PROPERTIES: String = """
                openapi: 3.0.3
                info:
                  title: Thing API
                  version: 1.0.0
                paths:
                  /foo:
                    get:
                      description: Lorem Ipsum
                      operationId: foo
                      responses:
                        200:
                          description: Lorem Ipsum
                          content:
                            application/json:
                              schema:
                                  type: object
                                  properties:
                              examples:
                                  example1:
                                    value:
                                      name: aaa
                                      alias: [a,d]
                                  example2:
                                    value:
                                      name:
                                        incorrectObject:
                                          a: 1
                """
        @Language("YAML")
        const val EXAMPLE_OBJECT_WITHOUT_PROPERTIES: String = """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /foo:
                get:
                  description: Lorem Ipsum
                  operationId: foo
                  responses:
                    200:
                      description: Lorem Ipsum
                      content:
                        application/json:
                          schema:
                            type: object
                          examples:
                            example1:
                              value:
                                anything: true
            """

        @Language("YAML")
        const val EXAMPLE_ARRAY_OF_OBJECTS_WITHOUT_PROPERTIES: String = """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /foo:
                get:
                  description: Lorem Ipsum
                  operationId: foo
                  responses:
                    200:
                      description: Lorem Ipsum
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              items:
                                type: array
                                items:
                                  type: object
                          examples:
                            example1:
                              value:
                                items:
                                  - anything: true
            """

        @Language("YAML")
        const val EXAMPLE_STRING_ARRAY: String = """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /foo:
                get:
                  description: Lorem Ipsum
                  operationId: foo
                  responses:
                    200:
                      description: Lorem Ipsum
                      content:
                        application/json:
                          schema:
                            type: object
                            properties: 
                              names:
                                type: array
                                items:
                                  type: string
                          examples:
                            example1:
                              value:
                                names: 
                                  - aaa
                                  - bbb
                                  - ccc 
                                  
                """

        // On OpenAPI 3.1 the parser returns the repeated request body example as a Java map instead of a JsonNode
        @Language("YAML")
        const val EXAMPLE_REFERENCED_SCHEMA: String = """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /contacts:
                post:
                  operationId: createContact
                  requestBody:
                    content:
                      application/json:
                        schema:
                          ${'$'}ref: '#/components/schemas/Contact'
                        examples:
                          example:
                            value:
                              name: Jane Doe
                              address:
                                city: Amsterdam
                  responses:
                    201:
                      description: Created
                    400:
                      ${'$'}ref: '#/components/responses/BadRequest'
              /contacts/{contactId}:
                put:
                  operationId: replaceContact
                  requestBody:
                    content:
                      application/json:
                        schema:
                          ${'$'}ref: '#/components/schemas/Contact'
                        examples:
                          example:
                            value:
                              name: Jane Doe
                              address:
                                city: Amsterdam
                  responses:
                    204:
                      description: Replaced
                    400:
                      ${'$'}ref: '#/components/responses/BadRequest'
            components:
              responses:
                BadRequest:
                  description: Bad request
                  content:
                    application/json:
                      schema:
                        type: object
                        properties:
                          message:
                            type: string
                      examples:
                        example:
                          value:
                            message: Bad Request
              schemas:
                Contact:
                  type: object
                  properties:
                    name:
                      type: string
                    address:
                      type: object
                      properties:
                        city:
                          type: string
            """

    }
}