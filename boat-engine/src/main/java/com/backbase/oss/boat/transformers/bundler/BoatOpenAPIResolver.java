package com.backbase.oss.boat.transformers.bundler;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIResolver;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.processors.ComponentsProcessor;
import io.swagger.v3.parser.processors.OperationProcessor;
import io.swagger.v3.parser.processors.PathsProcessor;

import java.util.Iterator;
import java.util.List;

/**
 * Same as io.swagger.v3.parser.OpenAPIResolver but allowing us to get to the ResolverCache
 */
public class BoatOpenAPIResolver {

    private final OpenAPI openApi;
    private final ComponentsProcessor componentsProcessor;
    private final PathsProcessor pathProcessor;
    private final OperationProcessor operationsProcessor;
    private final ExamplesProcessor examplesProcessor;

    public BoatOpenAPIResolver(OpenAPI openApi, List<AuthorizationValue> auths, String parentFileLocation) {
        this(openApi, auths, parentFileLocation, null);
    }

    public BoatOpenAPIResolver(OpenAPI openApi, List<AuthorizationValue> auths, String parentFileLocation, OpenAPIResolver.Settings settings) {
        this.openApi = openApi;
        OpenAPIResolver.Settings openApiResolverSettings = settings != null ? settings : new OpenAPIResolver.Settings();
        this.examplesProcessor = new ExamplesProcessor(openApi, parentFileLocation);
        ResolverCache cache = new BoatCache(openApi, auths, parentFileLocation, examplesProcessor);
        this.componentsProcessor = new ComponentsProcessor(openApi, cache);
        this.pathProcessor = new PathsProcessor(cache, openApi, openApiResolverSettings);
        this.operationsProcessor = new OperationProcessor(cache, openApi);
    }

    public OpenAPI resolve() {
        if (this.openApi == null) {
            return null;
        }
        processOpenAPI();
        return this.openApi;
    }

    private void processOpenAPI() {
        this.examplesProcessor.processExamples();
        this.pathProcessor.processPaths();
        this.componentsProcessor.processComponents();
        if (this.openApi.getPaths() == null) {
            return;
        }
        Iterator<String> var1 = this.openApi.getPaths().keySet().iterator();

        while (true) {
            PathItem pathItem;
            do {
                if (!var1.hasNext()) {
                    return;
                }

                String pathname = var1.next();
                pathItem = this.openApi.getPaths().get(pathname);
            } while (pathItem.readOperations() == null);

            for (Operation operation : pathItem.readOperations()) {
                this.operationsProcessor.processOperation(operation);
            }
        }
    }

}
