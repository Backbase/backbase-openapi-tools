package com.backbase.oss.boat.transformers.bundler;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.models.RefFormat;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BoatCache extends ResolverCache {

    private final ExamplesProcessor examplesProcessor;

    public BoatCache(OpenAPI openApi, List<AuthorizationValue> auths, String parentFileLocation,
        ExamplesProcessor examplesProcessor) {
        super(openApi, auths, parentFileLocation);
        this.examplesProcessor = examplesProcessor;
    }

    /**
     * When loading references resources that contain links themselves, also resolve these references.
     * @param ref
     * @param refFormat
     * @param expectedType
     * @param <T>
     * @return
     */
    @Override
    public <T> T loadRef(String ref, RefFormat refFormat, Class<T> expectedType) {

        log.debug("loadRef {}, {}, {}", ref, refFormat, expectedType);
        T result = super.loadRef(ref, refFormat, expectedType);

        if (result instanceof ApiResponse) {
            // resolve references from here...
            ApiResponse response = (ApiResponse) result;
            examplesProcessor.processContent(response.getContent());
        }
        return result;
    }
}
