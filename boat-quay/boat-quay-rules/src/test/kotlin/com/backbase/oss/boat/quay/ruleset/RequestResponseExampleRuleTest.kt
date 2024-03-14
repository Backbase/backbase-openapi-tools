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
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            paths: 
                /client-api/v1/trading/instruments/{id}/portfolios:
                    summary: Trading Portfolios By Instrument
                    description: Trading details information for portfolios selected by instrument
                    get:
                      tags:
                      - portfolio-trading-accounts
                      summary: Discretionary/non-discretionary portfolios detail information
                      description: |
                        Trading portfolios detail information
                      operationId: getPortfoliosByInstrument
                      parameters:
                      - name: id
                        in: path
                        description: Instrument internal id
                        required: true
                        style: simple
                        explode: false
                        schema:
                          type: string
                      responses:
                        
                        "400":
                          description: BadRequest
                          content:
                            application/json:
                              schema:
                                  title: BadRequestError
                                  required:
                                  - key
                                  - message
                                  type: object
                                  properties:
                                    message:
                                      minLength: 1
                                      type: string
                                      description: Any further information
                                    key:
                                      minLength: 1
                                      type: string
                                      description: Error summary
                                    errors:
                                      type: array
                                      description: Detailed error information
                                      items:
                                          required:
                                          - key
                                          - message
                                          type: object
                                          title: ErrorItem
                                          properties: 
                                            message:
                                              minLength: 1
                                              type: string
                                              description: Any further information.
                                            key:
                                              minLength: 1
                                              type: string
                                              description: "{capability-name}.api.{api-key-name}. For generated validation\
                                                \ errors this is the path in the document the error resolves to. e.g.\
                                                \ object name + '.' + field"
                                            context:
                                              title: Context
                                              type: object
                                              additionalProperties:
                                                type: string
                                              description: Context can be anything used to construct localised messages.
                              examples:
                                example:
                                  summary: example-1
                                  value:
                                    message: Bad Request
                                    key: GENERAL_ERROR
                                    errors:
                                    - message: "Value Exceeded. Must be between {min} and {max}."
                                      key: common.api.shoesize
                                      context:
                                        max: "50"
                                        min: "1"
            """.trimIndent()
        )

        val violations = cut.checkResponseExampleFulfill(context)

        ZallyAssertions
                .assertThat(violations)
                .isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = [EXAMPLE_TYPE_MISS_MATCH, EXAMPLE_INCORRECT_OBJECT])
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
    }

}