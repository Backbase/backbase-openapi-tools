package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.core.rulesConfig

class MoneyAndCurrencyRuleTest {

    private val cut = MoneyAndCurrencyRule(rulesConfig)

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `numeric amounts and unrestricted or enumerated currencies are reported`(openApiVersion: String) {
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
                Payment:
                  type: object
                  properties:
                    amount:
                      type: number
                    feeAmount:
                      type: integer
                    currency:
                      type: string
                    currencyCode:
                      type: string
                      pattern: '^[A-Z]{3}$'
                      enum: [EUR, USD]
                    targetCurrency:
                      type: string
                      pattern: '[A-Z]{3}'
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .descriptionsEqualTo(
                "Property amount should be a decimal string (type: string, format: number)",
                "Property feeAmount should be a decimal string (type: string, format: number)",
                "Property currency should have pattern ^[A-Z]{3}$",
                "Property currencyCode should not list the allowed currencies in an enum",
                "Property targetCurrency should have pattern ^[A-Z]{3}$"
            )
            .pointersEqualTo(
                "/components/schemas/Payment/properties/amount",
                "/components/schemas/Payment/properties/feeAmount",
                "/components/schemas/Payment/properties/currency",
                "/components/schemas/Payment/properties/currencyCode",
                "/components/schemas/Payment/properties/targetCurrency"
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `decimal string amounts, ISO 4217 currencies and currency objects are allowed`(openApiVersion: String) {
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
                Payment:
                  type: object
                  properties:
                    amount:
                      type: string
                      format: number
                    currency:
                      type: string
                      pattern: '^[A-Z]{3}$'
                    transactionAmount:
                      ${'$'}ref: '#/components/schemas/Currency'
                Currency:
                  type: object
                  properties:
                    amount:
                      type: string
                      format: number
                    currencyCode:
                      type: string
                      pattern: '^[A-Z]{3}$'
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }
}
