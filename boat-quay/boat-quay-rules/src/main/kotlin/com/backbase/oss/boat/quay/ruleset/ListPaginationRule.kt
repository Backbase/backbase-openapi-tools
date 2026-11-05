package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.util.MediaTypeUtil
import com.backbase.oss.boat.quay.ruleset.util.PointerUtil
import com.backbase.oss.boat.quay.ruleset.util.SchemaUtil
import com.fasterxml.jackson.core.JsonPointer
import com.typesafe.config.Config
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.parameters.Parameter
import org.zalando.zally.rule.api.*
import java.math.BigDecimal

@Rule(
    ruleSet = BoatRuleSet::class,
    id = "B021",
    severity = Severity.SHOULD,
    title = "List endpoints declare size and page or cursor parameters"
)
class ListPaginationRule(config: Config) {

    private val defaultPageSize = config.getInt("${javaClass.simpleName}.defaultPageSize").toBigDecimal()

    private val maxPageSize = config.getInt("${javaClass.simpleName}.maxPageSize").toBigDecimal()

    private val clientApiMaxPageSize = config.getInt("${javaClass.simpleName}.clientApiMaxPageSize").toBigDecimal()

    /**
     * A list endpoint is a GET whose 200 response is an object with an `items` array.
     */
    @Check(severity = Severity.SHOULD)
    fun validate(context: Context): List<Violation> {
        val components = context.api.components
        return context.validatePaths { (path, pathItem) ->
            val get = pathItem?.get
            if (pathItem == null || get == null || !isList(get, components)) {
                emptyList()
            } else {
                paginationViolations(path, pathItem, get, components, context)
            }
        }
    }

    private fun paginationViolations(
        path: String,
        pathItem: PathItem,
        get: Operation,
        components: Components?,
        context: Context
    ): List<Violation> {
        val getPointer = PointerUtil.pathPointer(path, "get")
        val parameters = (withPointers(pathItem.parameters, PointerUtil.pathPointer(path)) + withPointers(get.parameters, getPointer))
            .map { (parameter, pointer) -> resolve(parameter, components) to pointer }
            .filter { (parameter, _) -> parameter.`in` == "query" }
            .associateBy { (parameter, _) -> parameter.name }
        val violations = mutableListOf<Violation>()
        val size = parameters["size"]
        if (size == null) {
            violations += context.violation("List endpoint should declare an integer size query parameter", getPointer)
        } else {
            val (parameter, pointer) = size
            val schema = SchemaUtil.resolve(parameter.schema, components)
            val maximum = if (path.startsWith("/client-api/")) clientApiMaxPageSize else maxPageSize
            val sizeMaximum = schema?.maximum
            if (!SchemaUtil.hasType(schema, "integer")) {
                violations += context.violation("Query parameter size should be an integer", pointer)
            }
            if (decimal(schema?.default)?.compareTo(defaultPageSize) != 0) {
                violations += context.violation("Query parameter size should default to $defaultPageSize", pointer)
            }
            if (sizeMaximum == null || sizeMaximum > maximum) {
                violations += context.violation("Query parameter size should have a maximum of at most $maximum", pointer)
            }
        }
        val page = parameters["page"]
        if (page == null && parameters["cursor"] == null) {
            violations += context.violation("List endpoint should declare a page or cursor query parameter", getPointer)
        }
        if (page != null && SchemaUtil.resolve(page.first.schema, components)?.minimum?.compareTo(BigDecimal.ZERO) != 0) {
            violations += context.violation("Query parameter page should have a minimum of 0", page.second)
        }
        return violations
    }

    private fun withPointers(parameters: List<Parameter>?, base: JsonPointer): List<Pair<Parameter, JsonPointer>> =
        parameters.orEmpty().mapIndexed { index, parameter -> parameter to PointerUtil.pointer(base, "parameters", index) }

    private fun isList(get: Operation, components: Components?): Boolean =
        get.responses?.get("200")?.content.orEmpty()
            .filterKeys { MediaTypeUtil.isJson(it) }.values
            .any { content ->
                val schema = SchemaUtil.resolve(content.schema, components)
                SchemaUtil.isArray(SchemaUtil.resolve(schema?.properties?.get("items"), components))
            }

    private fun resolve(parameter: Parameter, components: Components?): Parameter {
        val ref = parameter.`$ref` ?: return parameter
        return components?.parameters?.get(ref.substringAfterLast("/")) ?: parameter
    }

    private fun decimal(value: Any?): BigDecimal? = value?.toString()?.toBigDecimalOrNull()
}
