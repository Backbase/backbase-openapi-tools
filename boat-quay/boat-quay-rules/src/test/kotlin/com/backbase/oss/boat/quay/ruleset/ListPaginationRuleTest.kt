package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.core.rulesConfig

class ListPaginationRuleTest {

    private val cut = ListPaginationRule(rulesConfig)

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `missing or misconfigured pagination parameters are reported`(openApiVersion: String) {
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
                  parameters:
                    - name: size
                      in: query
                      schema:
                        type: integer
                        default: 10
                        maximum: 1000
                    - name: page
                      in: query
                      schema:
                        type: integer
                        minimum: 1
                  responses:
                    '200':
                      ${'$'}ref: '#/components/responses/ThingList'
              /service-api/v1/things:
                get:
                  responses:
                    '200':
                      ${'$'}ref: '#/components/responses/ThingList'
              /integration-api/v1/things:
                get:
                  parameters:
                    - name: size
                      in: query
                      schema:
                        type: string
                        default: 20
                        maximum: 1000
                    - name: cursor
                      in: query
                      schema:
                        type: string
                  responses:
                    '200':
                      ${'$'}ref: '#/components/responses/ThingList'
            components:
              responses:
                ThingList:
                  description: ok
                  content:
                    application/json:
                      schema:
                        type: object
                        properties:
                          items:
                            type: array
                            items:
                              type: object
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .descriptionsEqualTo(
                "Query parameter size should default to 20",
                "Query parameter size should have a maximum of at most 100",
                "Query parameter page should have a minimum of 0",
                "List endpoint should declare an integer size query parameter",
                "List endpoint should declare a page or cursor query parameter",
                "Query parameter size should be an integer"
            )
            .pointersEqualTo(
                "/paths/~1client-api~1v1~1things/get/parameters/0",
                "/paths/~1client-api~1v1~1things/get/parameters/0",
                "/paths/~1client-api~1v1~1things/get/parameters/1",
                "/paths/~1service-api~1v1~1things/get",
                "/paths/~1service-api~1v1~1things/get",
                "/paths/~1integration-api~1v1~1things/get/parameters/0"
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `size with page or cursor is allowed and other endpoints are not checked`(openApiVersion: String) {
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
                  parameters:
                    - ${'$'}ref: '#/components/parameters/size'
                    - ${'$'}ref: '#/components/parameters/page'
                  responses:
                    '200':
                      ${'$'}ref: '#/components/responses/ThingList'
              /service-api/v1/things:
                parameters:
                  - name: size
                    in: query
                    schema:
                      type: integer
                      default: 20
                      maximum: 1000
                get:
                  parameters:
                    - name: cursor
                      in: query
                      schema:
                        type: string
                  responses:
                    '200':
                      ${'$'}ref: '#/components/responses/ThingList'
              /client-api/v1/things/{thingId}:
                get:
                  responses:
                    '200':
                      description: ok
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              name:
                                type: string
            components:
              parameters:
                size:
                  name: size
                  in: query
                  schema:
                    type: integer
                    default: 20
                    maximum: 100
                page:
                  name: page
                  in: query
                  schema:
                    type: integer
                    default: 0
                    minimum: 0
              responses:
                ThingList:
                  description: ok
                  content:
                    application/json:
                      schema:
                        type: object
                        properties:
                          items:
                            type: array
                            items:
                              type: object
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }
}
