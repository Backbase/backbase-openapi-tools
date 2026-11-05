package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.core.rulesConfig

class ListResponseShapeRuleTest {

    private val cut = ListResponseShapeRule(rulesConfig)

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `array responses, lists without items and 404 on top-level lists are reported`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /client-api/v1/things:
                get:
                  responses:
                    '200':
                      description: ok
                      content:
                        application/json:
                          schema:
                            type: array
                            items:
                              type: object
                    '404':
                      description: not found
              /client-api/v1/things/{thingId}/parts:
                get:
                  responses:
                    '200':
                      description: ok
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              parts:
                                type: array
                                items:
                                  type: object
                    '404':
                      description: thing not found
              /client-api/v1/things/{thingId}:
                put:
                  responses:
                    '200':
                      description: ok
                      content:
                        application/json:
                          schema:
                            type: array
                            items:
                              type: string
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .descriptionsEqualTo(
                "Response body should be an object, not an array",
                "List endpoint should return an empty list instead of 404",
                "List response should be an object with an items array",
                "Response body should be an object, not an array"
            )
            .pointersEqualTo(
                "/paths/~1client-api~1v1~1things/get/responses/200/content/application~1json",
                "/paths/~1client-api~1v1~1things/get/responses/404",
                "/paths/~1client-api~1v1~1things~1{thingId}~1parts/get/responses/200/content/application~1json",
                "/paths/~1client-api~1v1~1things~1{thingId}/put/responses/200/content/application~1json"
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `objects with items, single resources and non-collection segments are allowed`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /client-api/v1/things:
                get:
                  responses:
                    '200':
                      description: ok
                      content:
                        application/json:
                          schema:
                            ${'$'}ref: '#/components/schemas/ThingList'
              /client-api/v1/things/{thingId}:
                get:
                  responses:
                    '200':
                      description: ok
                      content:
                        application/json:
                          schema:
                            ${'$'}ref: '#/components/schemas/Thing'
                    '404':
                      description: not found
              /client-api/v1/things/{thingId}/parts:
                get:
                  responses:
                    '200':
                      description: ok
                      content:
                        application/json:
                          schema:
                            ${'$'}ref: '#/components/schemas/ThingList'
                    '404':
                      description: thing not found
              /client-api/v1/things/counts:
                get:
                  responses:
                    '200':
                      description: ok
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              total:
                                type: integer
            components:
              schemas:
                Thing:
                  type: object
                  properties:
                    name:
                      type: string
                ThingList:
                  type: object
                  properties:
                    items:
                      type: array
                      items:
                        ${'$'}ref: '#/components/schemas/Thing'
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }
}
