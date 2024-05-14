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
                /payments:
                    post:
                      tags:
                        - payments
                      summary: post
                      description: Create payments
                      operationId: createPayments
                      requestBody:
                        description: Create payments
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
                                      name: Trading payment
                                      amountNumberAsString: 101.01
                                      amountNumberAsStringOptional: 100.00
                                      additions:
                                        test1: value1
                                        test2: value2
                      responses:
                        "204":
                          description: OK
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
                        "200":
                          description: Portfolios by instrument information
                          content:
                            application/json:
                              schema:
                                  required:
                                    - portfolios
                                  type: object
                                  properties:
                                    portfolios:
                                      type: array
                                      description: Details of a portfolio.
                                      items:
                                        required:
                                        - name
                                        - portfolioId
                                        type: object
                                        properties:
                                            name:
                                              type: string
                                              description: Portfolio's name.
                                            alias:
                                              type: string
                                              description: Portfolio's alias.
                                            portfolioId:
                                              type: string
                                              description: Portfolio's internal id.
                                            availableBalance:
                                              required:
                                              - amount
                                              - currency
                                              type: object
                                              properties:
                                                amount:
                                                  type: number
                                                  description: The amount in the specified currency
                                                currency:
                                                  pattern: "^[A-Z]{3}${'$'}"
                                                  type: string
                                                  description: The alpha-3 code (complying with ISO 4217) of the currency
                                                    that qualifies the amount
                                            availableForTradingQty:
                                              type: number
                                              description: The number of shares available to sell (holdings - standing
                                                orders).
                                            iban:
                                              type: string
                                              description: "Account ID or an IBAN associated with a portfolio, where applicable."
                                              deprecated: true
                                            arrangementDisplay:
                                                type: object
                                                properties:
                                                  name:
                                                    type: string
                                                    description: The name that can be assigned by the bank to label an arrangement
                                                  displayName:
                                                    type: string
                                                    description: "Represents an arrangement by it's correct naming identifier.\
                                                      \ It could be account alias or user alias depending on the journey selected\
                                                      \ by the financial institution. If none of those is set, the arrangement\
                                                      \ name will be used."
                                                  bankAlias:
                                                    type: string
                                                    description: The name that can be assigned by the customer to label the
                                                      arrangement
                                                  iban:
                                                    type: string
                                                    description: "The International Bank Account Number. If specified, it must\
                                                      \ be a valid IBAN, otherwise an invalid value error could be raised."
                                                  bban:
                                                    type: string
                                                    description: BBAN is the country-specific bank account number. It is short
                                                      for Basic Bank Account Number. Account numbers usually match the BBAN.
                                                  number:
                                                    type: string
                                                    description: First 6 and/or last 4 digits of a Payment card. All other digits
                                                      will/to be masked. Be aware that using card number differently is potential
                                                      PCI risk.
                                                  bic:
                                                    type: string
                                                    description: Bank Identifier Code - international bank code that identifies
                                                      particular banks worldwide
                                                description: Arrangement information from Arrangement Manger
                                            canSell:
                                              type: boolean
                                              description: Showing that the instrument can be sold from the selected portfolio.
                              examples:
                                example1:
                                  value:
                                      portfolios:
                                        - name: Trading portfolio
                                          alias: My portfolio to trade
                                          portfolioId: 68bbeace-274e-11ec-9621-0242ac130002
                                          availableBalance:
                                            amount: 5068.3
                                            currency: USD
                                          availableForTradingQty: null
                                          iban: NL79RABO5373380466
                                          arrangementDisplay:
                                            name: Trading portfolio one
                                            displayName: First portfolio account
                                            bankAlias: Portfolio account
                                            iban: ••••••••••••••••••0466
                                            bban: ••••••••••0466
                                            number: •••••ffix
                                            bic: BICExample1
                                          canSell: true
                                          accounts:
                                          - id: 55bbeace-274e-22ec-5487-0242ac130004
                                          - id: 44bbeace-274e-22ec-5487-0242ac130005
                                example2:
                                  value:
                                      portfolios:
                                        - name: Trading portfolio
                                          alias: My portfolio to trade
                                          portfolioId: 68bbeace-274e-11ec-9621-0242ac130002
                                          
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
    }

}