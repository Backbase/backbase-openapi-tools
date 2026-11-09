package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.zalando.zally.core.DefaultContextFactory

class NoNullDefaultRuleTest {

    private val cut = NoNullDefaultRule()

    @Test
    fun `object schema with default null fails`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                Consent:
                  type: object
                  properties:
                    externalId:
                      type: string
                  default: null
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .hasSize(1)
            .pointersEqualTo("/components/schemas/Consent")
    }

    @Test
    fun `property with default null fails`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                Consent:
                  type: object
                  properties:
                    externalId:
                      type: string
                      default: null
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .hasSize(1)
            .pointersEqualTo("/components/schemas/Consent/properties/externalId")
    }

    @Test
    fun `parameter and response schemas with default null fail`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: API
              version: 1.0.0
            paths:
              /consents/{consentId}:
                get:
                  parameters:
                    - name: consentId
                      in: path
                      required: true
                      schema:
                        type: string
                        default: null
                  responses:
                    '200':
                      description: OK
                      content:
                        application/json:
                          schema:
                            type: object
                            default: null
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .pointersEqualTo(
                "/paths/~1consents~1{consentId}/get/parameters/0/schema",
                "/paths/~1consents~1{consentId}/get/responses/200/content/application~1json/schema"
            )
    }

    @Test
    fun `allOf schema with default null fails at the place it is declared`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                AccountInformation:
                  type: object
                  properties:
                    iban:
                      type: string
                      default: null
                CofAccountInformation:
                  allOf:
                    - ${'$'}ref: '#/components/schemas/AccountInformation'
                  default: null
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .pointersEqualTo(
                "/components/schemas/AccountInformation/properties/iban",
                "/components/schemas/CofAccountInformation"
            )
    }

    @Test
    fun `nullable schema with default null passes`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                Consent:
                  type: object
                  nullable: true
                  default: null
                  properties:
                    externalId:
                      type: string
                      nullable: true
                      default: null
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }

    @Test
    fun `schemas without default or with a non-null default pass`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                Consent:
                  type: object
                  properties:
                    externalId:
                      type: string
                    status:
                      type: string
                      default: PENDING
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }

    @Test
    fun `openapi 3_1 schema that allows null with default null passes`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.1.0
            info:
              title: API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                Consent:
                  type:
                    - object
                    - "null"
                  default: null
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }

    @Test
    fun `openapi 3_1 schema that does not allow null with default null fails`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.1.0
            info:
              title: API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                Consent:
                  type: object
                  default: null
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .hasSize(1)
            .pointersEqualTo("/components/schemas/Consent")
    }
}
