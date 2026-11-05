package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.zalando.zally.core.DefaultContextFactory

class ErrorBodyShapeRuleTest {

    private val cut = ErrorBodyShapeRule()

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `4xx bodies that differ from the SSDK error body are reported`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /client-api/v1/things:
                post:
                  responses:
                    '201':
                      description: created
                    '400':
                      description: bad request
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              problem:
                                type: string
                    '404':
                      description: not found
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              message:
                                type: integer
                              key:
                                type: integer
                              errors:
                                type: array
                                items:
                                  type: string
                    '409':
                      description: conflict
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              message:
                                type: string
                              errors:
                                type: object
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .descriptionsEqualTo(
                "400 response body should have a string message",
                "404 response body should have a string message",
                "404 response body key should be a string",
                "404 response body errors should be an array of objects",
                "409 response body errors should be an array of objects"
            )
            .pointersEqualTo(
                "/paths/~1client-api~1v1~1things/post/responses/400/content/application~1json",
                "/paths/~1client-api~1v1~1things/post/responses/404/content/application~1json",
                "/paths/~1client-api~1v1~1things/post/responses/404/content/application~1json",
                "/paths/~1client-api~1v1~1things/post/responses/404/content/application~1json",
                "/paths/~1client-api~1v1~1things/post/responses/409/content/application~1json"
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `SSDK error bodies, other media types and bodiless responses are allowed`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /client-api/v1/things:
                post:
                  responses:
                    '201':
                      description: created
                    '400':
                      description: bad request
                      content:
                        application/json:
                          schema:
                            ${'$'}ref: '#/components/schemas/BadRequestError'
                    '401':
                      description: unauthorized
                      content:
                        application/problem+json:
                          schema:
                            type: object
                    '403':
                      description: forbidden
                    default:
                      description: unexpected
                      content:
                        application/json:
                          schema:
                            type: object
            components:
              schemas:
                BadRequestError:
                  type: object
                  required: [message]
                  properties:
                    message:
                      type: string
                    key:
                      type: string
                    errors:
                      type: array
                      items:
                        ${'$'}ref: '#/components/schemas/ErrorItem'
                ErrorItem:
                  type: object
                  properties:
                    message:
                      type: string
                    key:
                      type: string
                    context:
                      type: object
                      additionalProperties:
                        type: string
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }
}
