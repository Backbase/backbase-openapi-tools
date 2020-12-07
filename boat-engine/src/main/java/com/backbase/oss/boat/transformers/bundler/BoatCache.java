package com.backbase.oss.boat.transformers.bundler;

import com.backbase.oss.boat.transformers.TransformerException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.models.RefFormat;
import java.nio.file.Paths;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

import static io.swagger.v3.parser.models.RefFormat.RELATIVE;

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
     *
     * @param ref          Json Pointer
     * @param refFormat    Format
     * @param expectedType Type to expect
     * @param <T>          Return type
     * @return Instance of reference
     */
    @Override
    public <T> T loadRef(String ref, RefFormat refFormat, Class<T> expectedType) {

        log.debug("loadRef {}, {}, {}", ref, refFormat, expectedType);
        T result = null;
        try {
            result = super.loadRef(ref, refFormat, expectedType);
        } catch (Exception e) {
            log.debug("Reference: {} is something else than json or yaml", ref);
            throw new TransformerException("Reference: " + ref + " cannot be loaded", e);
        }

        if (result instanceof ApiResponse) {
            // resolve references from here...
            ApiResponse response = (ApiResponse) result;

            String relativePath = null;
            if (refFormat == RELATIVE) {
                relativePath = Paths.get(ref.substring(0, ref.indexOf("#"))).getParent().toString();
            }
            examplesProcessor.processContent(response.getContent(), relativePath);
        }
        return result;
    }

}
