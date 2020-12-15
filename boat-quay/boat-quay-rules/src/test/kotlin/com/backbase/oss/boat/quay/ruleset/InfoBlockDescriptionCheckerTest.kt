package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.zalando.zally.core.DefaultContextFactory

class InfoBlockDescriptionCheckerTest {

    private val cut = InfoBlockDescriptionChecker()


    @Test
    fun `correct value for description`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              title: Thing API
              description: This is a meaningful description for API
              version: 1.0.0
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isEmpty()
    }

    @Test
    fun `incorrect value for description max length`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              title: Thing API
              description: Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed tempus placerat justo. Mauris eget tellus non ante interdum lacinia. Sed hendrerit justo.
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty
    }

    @Test
    fun `no value for description`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty
    }

    @Test
    fun `empty value for description`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              title: Thing API
              description: ''
              version: 1.0.0
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty
    }

}