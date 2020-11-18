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
    private final ResolverCache cache;
    private final ComponentsProcessor componentsProcessor;
    private final PathsProcessor pathProcessor;
    private final OperationProcessor operationsProcessor;
    private OpenAPIResolver.Settings settings;

    private final ExamplesProcessor examplesProcessor;

    public BoatOpenAPIResolver(OpenAPI openApi) {
        this(openApi, null, null, null);
    }

    public BoatOpenAPIResolver(OpenAPI openApi, List<AuthorizationValue> auths) {
        this(openApi, auths, null, null);
    }

    public BoatOpenAPIResolver(OpenAPI openApi, List<AuthorizationValue> auths, String parentFileLocation) {
        this(openApi, auths, parentFileLocation, null);
    }

    public BoatOpenAPIResolver(OpenAPI openApi, List<AuthorizationValue> auths, String parentFileLocation, OpenAPIResolver.Settings settings) {
        this.settings = new OpenAPIResolver.Settings();
        this.openApi = openApi;
        this.settings = settings != null ? settings : new OpenAPIResolver.Settings();
        this.examplesProcessor = new ExamplesProcessor(openApi, parentFileLocation);
        this.cache = new BoatCache(openApi, auths, parentFileLocation, examplesProcessor);
        this.componentsProcessor = new ComponentsProcessor(openApi, this.cache);
        this.pathProcessor = new PathsProcessor(this.cache, openApi, this.settings);
        this.operationsProcessor = new OperationProcessor(this.cache, openApi);
    }

    public OpenAPI resolve() {
        if (this.openApi == null) {
            return null;
        } else {
            examplesProcessor.processExamples(this.openApi);
            processOpenAPI();
            return this.openApi;
        }
    }

    private void processOpenAPI() {
        this.pathProcessor.processPaths();
        this.componentsProcessor.processComponents();
        if (this.openApi.getPaths() == null) {
            return;
        }
        Iterator var1 = this.openApi.getPaths().keySet().iterator();

        while(true) {
            PathItem pathItem;
            do {
                if (!var1.hasNext()) {
                    return;
                }

                String pathname = (String)var1.next();
                pathItem = (PathItem)this.openApi.getPaths().get(pathname);
            } while(pathItem.readOperations() == null);

            Iterator var4 = pathItem.readOperations().iterator();

            while(var4.hasNext()) {
                Operation operation = (Operation)var4.next();
                this.operationsProcessor.processOperation(operation);
            }
        }
    }

}
