package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.core.rulesConfig

class EndpointAccessControlDefinedRuleTest {

    private val rule = EndpointAccessControlDefinedRule(rulesConfig)

    @Test
    fun `Endpoint has ac defined as false`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            paths: 
              /client-api/foo:
                get:
                  x-BbAccessControl: false
            """.trimIndent()
        )

        val violations = rule.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isEmpty()
    }

    @Test
    fun `Endpoint has ac defined correctly`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            paths: 
              /client-api/foo:
                get:
                  x-BbAccessControl-resource: Users
                  x-BbAccessControl-function: Manage Users
                  x-BbAccessControl-privilege: view
            """.trimIndent()
        )

        val violations = rule.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isEmpty()
    }

    @Test
    fun `Endpoint has ac enabled AND defined correctly (a bit too much but still ok)`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            paths: 
              /client-api/foo:
                get:
                  x-BbAccessControl: true
                  x-BbAccessControl-resource: Users
                  x-BbAccessControl-function: Manage Users
                  x-BbAccessControl-privilege: view
            """.trimIndent()
        )

        val violations = rule.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isEmpty()
    }

    @Test
    fun `Endpoint has no ac defined at all`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            paths: 
              /client-api/foo:
                get:
                  x-SomethingElse: yes
            """.trimIndent()
        )

        val violations = rule.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty
    }

    @Test
    fun `Endpoint has ac defined but with empty value`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            paths: 
              /client-api/foo:
                get:
                  x-BbAccessControl-resource: Users
                  x-BbAccessControl-function: ""
                  x-BbAccessControl-privilege: view
            """.trimIndent()
        )

        val violations = rule.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty
    }

    @Test
    fun `Endpoint has ac defined but resource missing`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            paths: 
              /client-api/foo:
                get:
                  x-BbAccessControl-function: Manage Users
                  x-BbAccessControl-privilege: view
            """.trimIndent()
        )

        val violations = rule.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty
    }

    @Test
    fun `Endpoint has ac defined but function missing`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            paths: 
              /client-api/foo:
                get:
                  x-BbAccessControl-resource: Users
                  x-BbAccessControl-privilege: view
            """.trimIndent()
        )

        val violations = rule.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty
    }

    @Test
    fun `Endpoint has ac defined but privilege missing`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            paths: 
              /client-api/foo:
                get:
                  x-BbAccessControl-resource: Users
                  x-BbAccessControl-function: Manage Users
            """.trimIndent()
        )

        val violations = rule.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty
    }

    @Test
    fun `Endpoint has ac both defined and explicitly set false `() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            paths: 
              /client-api/foo:
                get:
                  x-BbAccessControl: false
                  x-BbAccessControl-resource: Users
                  x-BbAccessControl-function: Manage Users
                  x-BbAccessControl-privilege: view
            """.trimIndent()
        )

        val violations = rule.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty
    }

}