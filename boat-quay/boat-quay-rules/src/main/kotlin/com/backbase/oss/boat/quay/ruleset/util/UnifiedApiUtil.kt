package com.backbase.oss.boat.quay.ruleset.util

import io.swagger.v3.oas.models.info.Info

object UnifiedApiUtil {

    private val API_TYPE = "x-api-type"
    private val UNIFIED_BACKBASE_API = "Unified Backbase API"

    fun isUnifiedBackbaseApi(openApiInfo: Info) : Boolean =
        openApiInfo.extensions != null && openApiInfo.extensions[API_TYPE] == UNIFIED_BACKBASE_API

}