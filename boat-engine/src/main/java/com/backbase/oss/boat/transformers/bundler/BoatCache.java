package com.backbase.oss.boat.transformers.bundler;

import static io.swagger.v3.parser.models.RefFormat.RELATIVE;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.util.DeserializationUtils;
import io.swagger.v3.parser.util.PathUtils;
import io.swagger.v3.parser.util.RefUtils;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BoatCache extends ResolverCache {

    private final ExamplesProcessor examplesProcessor;
    private final Path parentDirectory;

    private String rootPath ;
    private List<AuthorizationValue> auths;
    private OpenAPI openApi;


    public BoatCache(OpenAPI openApi, List<AuthorizationValue> auths, String parentFileLocation,
        ExamplesProcessor examplesProcessor) {
        super(openApi, auths, parentFileLocation);
        this.rootPath = parentFileLocation;
        this.openApi = openApi;
        this.examplesProcessor = examplesProcessor;
        this.auths = auths;


        if(parentFileLocation != null) {
            if(parentFileLocation.startsWith("http")) {
                parentDirectory = null;
            } else {
                parentDirectory = PathUtils.getParentDirectoryOfFile(parentFileLocation);
            }
        } else {
            parentDirectory = new File(".").toPath();
        }
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
        T result = null;
        try {
            result = super.loadRef(ref, refFormat, expectedType);
        } catch (Exception exception) {
            log.debug("Reference: {} is something else than json or yaml", ref);
            result = parseRef(ref, refFormat, expectedType);

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

    private <T> T parseRef(String ref, RefFormat refFormat, Class<T> expectedType) {
        final String[] refParts = ref.split("#/");

        if (refParts.length > 2) {
            throw new RuntimeException("Invalid ref format: " + ref);
        }

        final String file = refParts[0];
        final String definitionPath = refParts.length == 2 ? refParts[1] : null;
        String contents = getExternalFileCache().get(file);

        if(parentDirectory != null) {
            contents = RefUtils.readExternalRef(file, refFormat, auths, parentDirectory);
        }
        else if(rootPath != null && rootPath.startsWith("http")) {
            contents = RefUtils.readExternalUrlRef(file, refFormat, auths, rootPath);
        }
        else if (rootPath != null) {
            contents = RefUtils.readExternalClasspathRef(file, refFormat, auths, rootPath);
        }

        T result = BoatDeserializationUtils.deserialize(contents, file, expectedType);
        return result;
    }




}
