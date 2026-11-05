package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.core.rulesConfig

class DateTimeFormatRuleTest {

    private val cut = DateTimeFormatRule(rulesConfig)

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `date and time strings without a date-time, date or time format are reported`(openApiVersion: String) {
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
                            type: object
                            properties:
                              updatedAt:
                                type: string
            components:
              schemas:
                Thing:
                  type: object
                  properties:
                    createdAt:
                      type: string
                    executionDate:
                      type: string
                      format: datetime
                    time:
                      type: string
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .descriptionsEqualTo(
                "Property createdAt should have format date-time or date or time",
                "Property executionDate should have format date-time or date or time",
                "Property time should have format date-time or date or time",
                "Property updatedAt should have format date-time or date or time"
            )
            .pointersEqualTo(
                "/components/schemas/Thing/properties/createdAt",
                "/components/schemas/Thing/properties/executionDate",
                "/components/schemas/Thing/properties/time",
                "/paths/~1client-api~1v1~1things/get/responses/200/content/application~1json/schema/properties/updatedAt"
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `date and time strings with a date-time, date or time format are allowed`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                Thing:
                  type: object
                  properties:
                    createdAt:
                      type: string
                      format: date-time
                    birthDate:
                      type: string
                      format: date
                    startTime:
                      type: string
                      format: time
                    dateFormat:
                      type: string
                    history:
                      type: array
                      items:
                        type: object
                        properties:
                          changedAt:
                            type: string
                            format: date-time
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }
}
