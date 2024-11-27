package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
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
    @ValueSource(strings = [EXAMPLE_STRING_ARRAY])
    fun `Array of Strings`(value: String) {

        val violations = cut.checkResponseExampleFulfill(DefaultContextFactory()
            .getOpenApiContext(value.trimIndent()))

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }

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

    }
}