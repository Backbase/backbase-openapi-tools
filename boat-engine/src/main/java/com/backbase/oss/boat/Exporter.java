package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.CachingResourceLoader;
import com.backbase.oss.boat.loader.RamlResourceLoader;
import com.backbase.oss.boat.transformers.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.CaseFormat;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;
import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.api.Library;
import org.raml.v2.api.model.v10.bodies.Response;
import org.raml.v2.api.model.v10.datamodel.JSONTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeInstance;
import org.raml.v2.api.model.v10.datamodel.TypeInstanceProperty;
import org.raml.v2.api.model.v10.datamodel.XMLTypeDeclaration;
import org.raml.v2.api.model.v10.declarations.AnnotationRef;
import org.raml.v2.api.model.v10.methods.Method;
import org.raml.v2.api.model.v10.resources.Resource;
import org.raml.v2.api.model.v10.system.types.AnnotableStringType;
import org.raml.v2.api.model.v10.system.types.MarkdownString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.backbase.oss.boat.ExampleUtils.getExampleObject;

@SuppressWarnings("rawtypes")
public class Exporter {

    private static final Logger log = LoggerFactory.getLogger(Exporter.class);
    private static final Pattern placeholderPattern = Pattern.compile("(\\{[^}]*\\})");
    public static final String X_EXAMPLES = "x-examples";
    public static final String NO_DESCRIPTION_AVAILABLE = "No description available";
    public static final String NEW_LINE = "\n";
    public static final String EXAMPLE = "example";
    private static final ObjectMapper mapper = Utils.createObjectMapper();

    private final ExporterOptions exporterOptions;

    private Exporter(ExporterOptions exporterOptions) {
        super();
        this.exporterOptions = exporterOptions;
    }

    /**
     * Better to use {@link #export(File, ExporterOptions)}
     *
     * @param inputFile             The input file.
     * @param addJavaTypeExtensions whether to annotate with x-java-type when json schema contains javaType.
     * @param transformers          a list of transformers.
     * @return OpenApi
     * @throws ExportException things going south.
     */
    public static OpenAPI export(File inputFile, boolean addJavaTypeExtensions, List<Transformer> transformers)
        throws ExportException {
        ExporterOptions exporterOptions = new ExporterOptions();
        exporterOptions.setAddJavaTypeExtensions(addJavaTypeExtensions);
        exporterOptions.setTransformers(transformers);
        return export(inputFile, exporterOptions);
    }

    /**
     * Guesses the service name from the file path and calls {@link #export(String, File)}.
     *
     * @param inputFile The input file.
     * @param options   options.
     * @return OpenApi
     * @throws ExportException things going south.
     */
    public static OpenAPI export(File inputFile, ExporterOptions options) throws ExportException {
        // Guess Service Name
        AtomicReference<String> serviceName = new AtomicReference<>("serviceName");

        String[] split = inputFile.getPath().split("/");
        ArrayUtils.reverse(split);
        Arrays.stream(split)
            .filter(part -> part.endsWith("-spec"))
            .findFirst()
            .ifPresent(s -> serviceName.set(s.replace("-spec", "-service")));

        log.debug("Export: {} with options: {}", inputFile, options);
        return new Exporter(options).export(serviceName.get(), inputFile);
    }

    @SuppressWarnings("java:S3776")
    @SneakyThrows
    private OpenAPI export(String serviceName, File inputFile) throws ExportException {

        File parentFile = inputFile.getParentFile();
        URL baseUrl = parentFile.toURI().toURL();

        Map<String, String> ramlTypeReferences = new TreeMap<>();
        // Parse raml document as yaml instead to reverse engineer json references from types
        try {

            String ramlAsString = new String(Files.readAllBytes(inputFile.toPath()), Charset.defaultCharset());
            JsonNode jsonNode = mapper.readTree(ramlAsString);
            parseRamlTypeReferences(baseUrl, ramlTypeReferences, jsonNode);
        } catch (Exception e) {
            throw new ExportException("Failed to export ramlTypes", e);
        }

        CachingResourceLoader resourceLoader = new CachingResourceLoader(
            new RamlResourceLoader(inputFile, inputFile.getParentFile()));
        RamlModelBuilder ramlModelBuilder = new RamlModelBuilder(resourceLoader);
        RamlModelResult ramlModelResult = ramlModelBuilder.buildApi(inputFile);

        validateRamlModelResult(inputFile, ramlModelResult);

        Api ramlApi = ramlModelResult.getApiV10();
        Components components = new Components();
        components.setSchemas(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));

        JsonSchemaToOpenApi jsonSchemaToOpenApi = new JsonSchemaToOpenApi(
            baseUrl,
            components,
            ramlTypeReferences
        );

        assert ramlApi != null;
        Map<String, TypeDeclaration> types = collectTypesFromRamlSpec(ramlApi);
        List<Operation> operations = new ArrayList<>();

        log.debug("Converting RAML Types from Spec and Libraries");
        for (Map.Entry<String, TypeDeclaration> entry : types.entrySet()) {
            String name = entry.getKey();
            TypeDeclaration typeDeclaration = entry.getValue();
            Schema typeSchema;
            String schemaName = Utils.normalizeSchemaName(name);
            if (typeDeclaration instanceof JSONTypeDeclaration) {
                typeSchema = jsonSchemaToOpenApi.convert(schemaName, (JSONTypeDeclaration) typeDeclaration);
            } else {
                typeSchema = RamlSchemaToOpenApi.convert(schemaName, typeDeclaration, components);
            }
            if (typeDeclaration.examples() != null && !typeDeclaration.examples().isEmpty()) {
                List<Example> examples = typeDeclaration.examples().stream()
                    .map(exampleSpec -> new Example()
                        .value(getExampleObject(exampleSpec, exporterOptions.isConvertExamplesToYaml()))
                        .summary(exampleSpec.name()))
                    .collect(Collectors.toList());
                typeSchema.addExtension(X_EXAMPLES, examples);
            }

            log.debug("Added type: {}", typeSchema.getName());
        }

        // Start Dereference Process
        List<Schema> schemas = new ArrayList<>(components.getSchemas().values());
        for (Schema schema : schemas) {
            try {
                jsonSchemaToOpenApi.dereferenceSchema(schema, components);
            } catch (Exception e) {
                throw new ExportException("Failed to dereference schema", e);
            }
        }
        components.getSchemas().values()
            .forEach(schema1 -> Utils.cleanUp(schema1, !exporterOptions.isAddJavaTypeExtensions()));

        Info info = setupInfo(ramlApi);
        List<Tag> tags = setupTags(ramlApi);

        String url = "/" + serviceName + "/";

        List<Server> servers = new LinkedList<>();
        servers.add(
            new Server()
                .url(url)
                .description("The server")
                .variables(null));

        Paths paths = new Paths();

        String ramlBaseUrl = buildBaseUri(ramlApi);

        try {
            convertResources(ramlBaseUrl, ramlApi.resources(), paths, components, jsonSchemaToOpenApi, operations);
        } catch (DerefenceException e) {
            throw new ExportException("Faield to defererence resources", e);
        }

        OpenAPI openAPI = new OpenAPI();
        openAPI.setOpenapi("3.0.3");
        openAPI.setInfo(info);
        openAPI.setTags(tags);
        openAPI.setServers(servers);
        openAPI.setComponents(components);
        openAPI.setPaths(paths);

        // Start dereference Process
        schemas = new ArrayList<>(components.getSchemas().values());
        for (Schema schema : schemas) {
            try {
                jsonSchemaToOpenApi.dereferenceSchema(schema, components);
            } catch (Exception e) {
                throw new ExportException("Failed to dereference schema", e);
            }
        }
        components.getSchemas().values()
            .forEach(schema -> Utils.cleanUp(schema, !exporterOptions.isAddJavaTypeExtensions()));

        exporterOptions.getTransformers().forEach(transformer -> transformer.transform(openAPI, new HashMap()));

        return openAPI;
    }

    private String buildBaseUri(Api ramlApi) {
        if (ramlApi.baseUri() == null) {
            return "/";
        }
        String ramlBaseUrl = ramlApi.baseUri().value();
        if (!ramlBaseUrl.startsWith("/")) {
            ramlBaseUrl = "/" + ramlBaseUrl;
        }
        if (ramlBaseUrl.contains("{version}")) {
            ramlBaseUrl = StringUtils.replace(ramlBaseUrl, "{version}", ramlApi.version().value());
        }
        return ramlBaseUrl;
    }

    @SuppressWarnings("java:S3776")
    private Info setupInfo(Api ramlApi) {
        log.debug("Setup Description");

        String version = ramlApi.version() != null ? ramlApi.version().value() : "1.0";
        Info info = new Info()
            .title(ramlApi.title().value())
            .version(version);

        final StringBuilder markdown = new StringBuilder();
        if (isNotBlank(ramlApi.description())) {
            markdown
                .append(ramlApi.description().value())
                .append(NEW_LINE);
        }
        if (ramlApi.documentation() != null) {
            ramlApi.documentation().forEach(
                documentationItem -> {
                    String title = null;
                    String documentation = null;

                    if (isNotBlank(documentationItem.title())) {
                        title = documentationItem.title().value();
                    }
                    if (isNotBlank(documentationItem.content())) {
                        documentation = documentationItem.content().value();
                    }

                    if (documentation != null && documentation.startsWith("# ")) {
                        markdown.append(documentationItem.content().value());
                        markdown.append(NEW_LINE);
                    } else if (title != null) {
                        if (!title.startsWith("# ")) {
                            markdown.append("# ");
                        }
                        markdown.append(title);
                        markdown.append(NEW_LINE);
                        if (documentation != null) {
                            markdown.append(cleanupMarkdownString(documentationItem.content().value()));
                            markdown.append(NEW_LINE);
                        }
                    }
                }
            );
        }
        if (markdown.length() != 0) {
            info.setDescription(markdown.toString());
        } else {
            info.setDescription(NO_DESCRIPTION_AVAILABLE);
            log.warn("No description available.");
        }
        return info;
    }

    private boolean isNotBlank(AnnotableStringType annotableStringType) {
        return annotableStringType != null && StringUtils.isNotBlank(annotableStringType.value());
    }

    private List<Tag> setupTags(Api ramlApi) {
        String title = ramlApi.title().value().toLowerCase(Locale.ROOT);
        return Collections.singletonList(new Tag().name(title));
    }

    private String cleanupMarkdownString(String value) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] lines = value.split(NEW_LINE);
        for (int i = 0; i < lines.length; i++) {
            if (i == 0 && lines[i].startsWith("#")) {
                String title = "# " + StringUtils.substringAfterLast(lines[i], "#").trim();
                stringBuilder.append(title).append(NEW_LINE);
            } else {
                stringBuilder.append(lines[i]).append(NEW_LINE);
            }
        }
        return stringBuilder.toString().trim();
    }

    private void parseRamlTypeReferences(URL baseUrl, Map<String, String> ramlTypeReferences,
                                         JsonNode jsonNode) {
        if (jsonNode.hasNonNull("types")) {
            ObjectNode types = (ObjectNode) jsonNode.get("types");
            types.fields().forEachRemaining(nodeEntry -> parseRamlRefEntry(baseUrl, ramlTypeReferences, nodeEntry));
        }
        if (jsonNode.hasNonNull("schemas")) {
            ObjectNode schemas = (ObjectNode) jsonNode.get("schemas");
            schemas.fields().forEachRemaining(nodeEntry -> parseRamlRefEntry(baseUrl, ramlTypeReferences, nodeEntry));
        }

        if (jsonNode.hasNonNull("uses")) {
            ObjectNode traits = (ObjectNode) jsonNode.get("uses");
            traits.fields().forEachRemaining(traitMap -> {
                String traitRef = traitMap.getValue().textValue();
                log.debug("Parsing trait ref: {}", traitRef);
                try {
                    URL absoluteReference = Utils.getAbsoluteReference(baseUrl, traitRef);
                    URL absoluteReferenceParent = Utils.getAbsoluteReferenceParent(absoluteReference);
                    JsonNode trait = mapper.readTree(absoluteReference);
                    parseRamlTypeReferences(absoluteReferenceParent, ramlTypeReferences, trait);

                } catch (IOException e) {
                    log.error("Failed ot read url: {}", traitRef, e);
                }
            });

        }


    }

    private void parseRamlRefEntry(URL baseUrl, Map<String, String> ramlTypeReferences,
                                   Map.Entry<String, JsonNode> nodeEntry) {
        String key = nodeEntry.getKey();
        JsonNode typeReference = nodeEntry.getValue();
        if (typeReference.has("type") && typeReference instanceof ObjectNode) {
            JsonNode typeNode = typeReference.get("type");
            String url = typeNode.textValue();
            if (typeNode instanceof TextNode && Utils.isUrl(url)) {
                URL absoluteReference = Utils.getAbsoluteReference(baseUrl, url);
                ramlTypeReferences.put(key, absoluteReference.toString());
                log.debug("Add raml type reference: {} to: {}", key, absoluteReference);
            } else {
                log.debug("Cannot create raml ref for: {}", url);
            }
        } else if (typeReference instanceof TextNode && typeReference.textValue().endsWith(".json")) {
            URL absoluteReference = Utils.getAbsoluteReference(baseUrl, typeReference.textValue());
            ramlTypeReferences.put(key, absoluteReference.toString());
            log.debug("Add raml type reference: {} to: {}", key, absoluteReference);
        }
    }

    private Map<String, TypeDeclaration> collectTypesFromRamlSpec(Api ramlApi) {
        Map<String, TypeDeclaration> types = new TreeMap<>();

        for (Library library : ramlApi.uses()) {
            library.schemas().forEach(typeDeclaration -> types.put(typeDeclaration.name(), typeDeclaration));
            library.types().forEach(typeDeclaration -> types.put(typeDeclaration.name(), typeDeclaration));
        }
        ramlApi.schemas().forEach(typeDeclaration -> types.put(typeDeclaration.name(), typeDeclaration));
        ramlApi.types().forEach(typeDeclaration -> types.put(typeDeclaration.name(), typeDeclaration));
        return types;
    }

    private void validateRamlModelResult(File file, RamlModelResult ramlModelResult) throws ExportException {
        if (ramlModelResult.hasErrors()) {
            log.error("Error validating RAML document: {}", file);
            ramlModelResult.getValidationResults()
                .forEach(validationResult -> log.error(validationResult.getMessage()));
            throw new ExportException("Error validation RAML");
        }

        if (ramlModelResult.getApiV10() == null) {
            throw new ExportException("Not a valid RAML 1.0 document");
        }
    }

    private void convertResources(String rootPath, List<Resource> resources, Paths paths, Components components,
                                  JsonSchemaToOpenApi jsonSchemaToOpenApi, List<Operation> operations)
        throws ExportException, DerefenceException {
        for (Resource resource : resources) {
            if (log.isDebugEnabled()) {
                log.debug("Mapping RAML Resource displayName: {} relativeUrl: {} with description: {} resourcePath: {}",
                    resource.displayName().value(),
                    resource.relativeUri().value(),
                    resource.description() != null ? resource.description().value() : null,
                    resource.resourcePath());
            }
            PathItem pathItem = convertResource(resource, components, jsonSchemaToOpenApi,
                operations);
            String path = rootPath + resource.resourcePath();
            if (!pathItem.readOperations().isEmpty()) {
                paths.addPathItem(path, pathItem);
            }
            convertResources(rootPath, resource.resources(), paths, components, jsonSchemaToOpenApi, operations);
        }
    }

    private PathItem convertResource(Resource resource, Components components,
                                     JsonSchemaToOpenApi jsonSchemaToOpenApi, List<Operation> operations)
        throws ExportException, DerefenceException {

        PathItem pathItem = new PathItem();
        pathItem.summary(getDisplayName(resource.displayName()));
        pathItem.description(getDescription(resource));
        log.debug("Mapping RAML resource: {}", pathItem.getSummary());
        mapUriParameters(resource, pathItem, components);
        mapMethods(resource, pathItem, components, jsonSchemaToOpenApi, operations);
        return pathItem;
    }

    private void mapUriParameters(Resource resource, PathItem pathItem,
                                  Components components) {
        Resource current = resource;
        Resource parent = current.parentResource();

        while (parent != null) {
            current.uriParameters().forEach(type -> {
                log.debug("Mapping URI parameter: {}", type.name());
                log(type);
                Parameter parameter = new PathParameter();
                convertTypeToParameter(type, parameter, components);
                if (pathItem.getParameters() != null && pathItem.getParameters().stream().anyMatch(
                    existingParam -> existingParam.getName().equals(parameter.getName()) && existingParam.getIn()
                        .equals(parameter.getIn()))) {
                    log.warn("{} has double Parameter {} in path: {} Detected. ignoring", resource.resourcePath(),
                        parameter.getName(), pathItem.getDescription());
                } else {
                    pathItem.addParametersItem(parameter);
                }
            });

            parent = current.parentResource();
            current = parent;
        }
        resolveUnspecifiedPathParameters(resource, pathItem);
    }

    /**
     * Some raml specs have unspecified uri parameters. OpenAPI requires all parameters to be specified.
     *
     * @param resource RAML Resource Item
     * @param pathItem Path Item mapped to RAML Resource with added path parameters
     */
    private void resolveUnspecifiedPathParameters(Resource resource, PathItem pathItem) {
        String resourcePath = resource.resourcePath();
        Matcher matcher = placeholderPattern.matcher(resourcePath);
        while (matcher.find()) {
            String placeHolder = matcher.group();
            String placeholderName = placeHolder.substring(1, placeHolder.length() - 1);

            if (pathItem.getParameters() == null) {
                pathItem.setParameters(new ArrayList<>());
            }
            Optional<Parameter> optionalParameter = pathItem.getParameters().stream()
                .filter(parameter -> parameter.getName().equals(placeholderName))
                .findFirst();

            if (!optionalParameter.isPresent()) {
                log.debug("Unspecified URI parameter: {} in RAML. Generating URI Parameter in OpenAPI",
                    placeholderName);
                Parameter parameter = new PathParameter();
                parameter.setName(placeholderName);
                parameter.setRequired(true);
                parameter.description("Generated parameter by BOAT. Please specify the URI parameter in RAML");
                parameter.setSchema(new StringSchema());
                pathItem.addParametersItem(parameter);
            }
        }
    }

    private void log(TypeDeclaration typeDeclaration) {
        String description = getDescription(typeDeclaration.description());
        if (log.isDebugEnabled()) {
            log.debug("Type name: {} type: {} displayName: {} defaultValue: {} description: {}",
                typeDeclaration.name(), typeDeclaration.type(), getDisplayName(typeDeclaration.displayName()),
                typeDeclaration.defaultValue(), description);

        }
    }


    private String getDisplayName(AnnotableStringType parameter) {
        return parameter != null ? parameter.value() : null;
    }

    private String getDescription(MarkdownString description) {
        if (description == null) {
            return NO_DESCRIPTION_AVAILABLE;
        }
        String result = description.value();
        return StringEscapeUtils.unescapeJavaScript(result);
    }

    private void mapMethods(Resource resource, PathItem pathItem, Components components,
                            JsonSchemaToOpenApi jsonSchemaToOpenApi, List<Operation> operations)
        throws ExportException, DerefenceException {
        for (Method ramlMethod : resource.methods()) {
            PathItem.HttpMethod httpMethod = getHttpMethod(ramlMethod);
            ApiResponses apiResponses = mapResponses(resource, ramlMethod, components, jsonSchemaToOpenApi);

            log.debug("Mapping method: {}", httpMethod);

            ArrayList<Parameter> parameters = new ArrayList<>();
            addHeaders(ramlMethod, parameters, components);
            addQueryParameters(ramlMethod, parameters, components);

            RequestBody requestBody = convertRequestBody(resource, ramlMethod, components, jsonSchemaToOpenApi);
            String resourcePath = resource.resourcePath();

            String tag = Arrays.stream(resourcePath.substring(1).split("/")).findFirst().orElse("tag");
            String operationId = getOperationId(resource, ramlMethod, operations, requestBody);
            String description = getDescription(ramlMethod.description());
            String summary;
            List<String> unwantedSummaries = new ArrayList<>(Arrays.asList("put", "get", "post", "delete"));
            if (ramlMethod.displayName() == null || unwantedSummaries.contains(ramlMethod.displayName().value())) {
                summary = getSummary(ramlMethod.description());
            } else {
                summary = ramlMethod.displayName().value();
            }

            Operation operation = new Operation();

            operation.addTagsItem(tag);
            operation.setDescription(description);
            operation.setResponses(apiResponses);
            operation.setSummary(summary);
            operation.setParameters(parameters.isEmpty() ? null : parameters);
            operation.setOperationId(operationId);
            operations.add(operation);

            if (httpMethod.equals(PathItem.HttpMethod.DELETE) && requestBody != null) {
                log.warn("{} is a DELETE operation and must NOT have an requestBody. Removing operation entirely",
                    resourcePath);
            } else {
                operation.setRequestBody(requestBody);
            }

            processMethodAnnotations(resourcePath, components, ramlMethod, httpMethod, operation, jsonSchemaToOpenApi);
            if (description != null && description.contains("deprecated")) {
                operation.deprecated(true);
            }

            pathItem.operation(httpMethod, operation);

        }
    }

    private String getSummary(MarkdownString description) {
        if (description == null) {
            return null;
        }
        return Stream.of(description.value().split(NEW_LINE))
            .findFirst()
            .map(firstLine -> firstLine.replace("#", ""))
            .map(String::trim)
            .map(firstLine -> firstLine.endsWith(".") ? firstLine : firstLine + ".")
            .orElse(null);
    }

    @SuppressWarnings("java:S3776")
    private String getOperationId(Resource resource, Method ramlMethod, List<Operation> operations,
                                  RequestBody requestBody) {

        String httpMethod = ramlMethod.method();
        String resourceName = resource.displayName().value();

        // Some RAML display names consist "/"
        if (resourceName.contains("/")) {
            resourceName = Arrays.stream(resourceName.split("/")).map(StringUtils::capitalize)
                .collect(Collectors.joining());
        }

        String operationId = httpMethod + resourceName;

        // If operationId is equal the http method name,
        // take the display name  or resource path name of the raml resource
        if (operationId.equalsIgnoreCase(ramlMethod.method())) {
            operationId = Utils.normalizeDisplayName(StringUtils.substringAfterLast(resource.resourcePath(), "/"));
        }

        // If that name contains spaces, concat the name with capitalizing each word
        if (operationId.contains(" ")) {
            operationId = Arrays.stream(operationId.split(" ")).map(StringUtils::capitalize)
                .collect(Collectors.joining());
        }

        // prepend http name ot to the operationId and ensure the rest has a capital
        operationId = Utils.normalizeDisplayName(operationId);

        // path has path parameter, add By<PathParam> if not already there and hope for the best.
        // Ensure format is lower camel case.
        operationId = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL).convert(operationId);

        String finalOperationId = operationId;
        if (operationIdExists(operations, finalOperationId) && requestBody != null) {

            Set<String> placeHolders = Utils.getPlaceholders(resource.resourcePath());
            if (!placeHolders.isEmpty()) {
                String suffix =
                    "By" + placeHolders.stream().map(StringUtils::capitalize).collect(Collectors.joining("And"));
                if (!operationId.toLowerCase().endsWith(suffix.toLowerCase())) {
                    operationId += suffix;
                }
            }
            if(log.isWarnEnabled()) {
                log.warn("Operation {} for path: {}  already exists! using: {}", finalOperationId, resource.resourcePath(), operationId);
            }
        }

        if (operationIdExists(operations, finalOperationId)) {
            // So many ways these things go wrong. Now trying last part of resource path before placeholder
            operationId += httpMethod + StringUtils.capitalize(Utils.normalizeDisplayName(resourceName));

        }
        if (log.isDebugEnabled()) {
            log.debug("Resolve operationId: {} from resource: {} with method: {} and path: {}", operationId,
                resource.displayName().value(), httpMethod, resource.resourcePath());
        }
        return operationId;
    }

    private boolean operationIdExists(List<Operation> operations, String finalOperationId) {
        return operations.stream().anyMatch(operation -> operation.getOperationId().equalsIgnoreCase(finalOperationId));
    }

    private void processMethodAnnotations(String resourcePath, Components components, Method ramlMethod,
                                          PathItem.HttpMethod httpMethod, Operation operation, JsonSchemaToOpenApi jsonSchemaToOpenApi)
        throws ExportException {
        for (AnnotationRef annotationRef : ramlMethod.annotations()) {
            TypeDeclaration annotation = annotationRef.annotation();
            TypeInstance typeInstance = annotationRef.structuredValue();

            Schema annotationSchema = getAnnotationSchema(resourcePath, components, annotation, jsonSchemaToOpenApi);

            if (annotation.name().toLowerCase().contains("deprecat")) {
                log.debug("Operation {} in path: {} marked deprecated", httpMethod, resourcePath);
                operation.deprecated(true);
            }

            for (TypeInstanceProperty property : typeInstance.properties()) {
                String extensionName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, annotationSchema.getName());
                String propertyName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, property.name());
                if (property.isArray().booleanValue()) {
                    operation.addExtension("-" + extensionName + "-" + propertyName, property.values());
                } else {
                    operation.addExtension("x-" + extensionName + "-" + propertyName, property.value().value());
                }
            }
        }
    }

    private Schema getAnnotationSchema(String resourcePath, Components components, TypeDeclaration annotation,
                                       JsonSchemaToOpenApi jsonSchemaToOpenApi) throws ExportException {
        Schema annotationSchema;
        if (annotation instanceof JSONTypeDeclaration) {
            JSONTypeDeclaration jsonType = (JSONTypeDeclaration) annotation;
            if (jsonType.type().equals(jsonType.schemaContent())) {
                // Type is inline. parse it as a new one
                try {
                    annotationSchema = jsonSchemaToOpenApi.convert(annotation.name(), jsonType);
                    jsonSchemaToOpenApi.dereferenceSchema(annotationSchema, components);
                } catch (DerefenceException e) {
                    throw new ExportException(
                        "Cannot dereference inline schema: : " + jsonType.schemaContent() + " for response: "
                            + resourcePath);
                }
                Utils.cleanUp(annotationSchema, !exporterOptions.isAddJavaTypeExtensions());
            } else {
                String modelName = Utils.getProposedSchemaName(jsonType.type());
                annotationSchema = components.getSchemas().get(modelName);
                if (annotationSchema == null) {
                    throw new ExportException(
                        "Cannot find schema definition for: " + jsonType.type() + " for response: " + resourcePath);
                }
            }
        } else {
            annotationSchema = RamlSchemaToOpenApi.convert(annotation.name(), annotation, components);
        }
        return annotationSchema;
    }

    private RequestBody convertRequestBody(Resource resource, Method ramlMethod, Components components,
                                           JsonSchemaToOpenApi jsonSchemaToOpenApi) throws DerefenceException, ExportException {
        if (ramlMethod.body() == null || ramlMethod.body().isEmpty()) {
            return null;
        }

        Content content = new Content();
        for (TypeDeclaration body : ramlMethod.body()) {
            String name = getName(resource, ramlMethod) + "Request";
            MediaType mediaType = convertBody(body, name, components, jsonSchemaToOpenApi);
            content.addMediaType(body.name(), mediaType);
        }
        RequestBody requestBody = new RequestBody();
        requestBody.setContent(content);
        requestBody.setDescription(ramlMethod.description() != null ? getDescription(ramlMethod.description()) : null);
        return requestBody;
    }

    private void addQueryParameters(Method ramlMethod, ArrayList<Parameter> parameters, Components components) {
        ramlMethod.queryParameters().forEach(typeDeclaration -> {
            Parameter parameter = new QueryParameter();
            convertTypeToParameter(typeDeclaration, parameter, components);
            parameters.add(parameter);
        });
    }

    private void addHeaders(Method ramlMethod, ArrayList<Parameter> parameters, Components components) {
        ramlMethod.headers().forEach(type -> {
            Parameter parameter = new HeaderParameter();
            convertTypeToParameter(type, parameter, components);
            parameters.add(parameter);
        });
    }

    private void convertTypeToParameter(TypeDeclaration typeDeclaration, Parameter parameter,
                                        Components components) {
        if (log.isDebugEnabled()) {
            log.debug("Converting Parameter from: {} with type: {} into: {}", typeDeclaration.name(),
                typeDeclaration.type(), parameter.getClass().getName());
        }
        Schema schema = RamlSchemaToOpenApi.convert(typeDeclaration.name(), typeDeclaration, components);

        parameter.setName(typeDeclaration.name());
        parameter.setRequired(typeDeclaration.required());
        parameter.setSchema(schema);
        if (Boolean.TRUE.equals(schema.getDeprecated())) {
            parameter.deprecated(true);
        }
        parameter.setDescription(getDescription(typeDeclaration.description()));

        if (typeDeclaration.examples() != null && !typeDeclaration.examples().isEmpty()) {
            parameter.setExamples(new LinkedHashMap<>());
            typeDeclaration.examples().forEach(exampleSpec -> {
                Example example = new Example();
                example.setValue(getExampleObject(exampleSpec, true));
                example.setSummary(exampleSpec.name());
                parameter.getExamples().put(exampleSpec.name(), example);
            });
        } else {
            parameter.setExamples(new LinkedHashMap<>());
            Example example = new Example();
            example.setValue(getExampleObject(typeDeclaration.example(), true));
            example.setSummary(EXAMPLE);
            parameter.getExamples().put(EXAMPLE, example);
        }

    }

    private ApiResponses mapResponses(Resource resource, Method ramlMethod, Components components,
                                      JsonSchemaToOpenApi jsonSchemaToOpenApi) throws ExportException, DerefenceException {
        ApiResponses apiResponses = new ApiResponses();
        for (Response ramlResponse : ramlMethod.responses()) {
            Content apiResponseContent = new Content();
            String responseCode = ramlResponse.code().value();
            String description = getDescription(ramlResponse);

            ApiResponse apiResponse = new ApiResponse();
            apiResponse.setDescription(description);

            if (!responseCode.equals("204")) {
                List<TypeDeclaration> bodies = ramlResponse.body();
                for (TypeDeclaration body : bodies) {
                    String contentType = body.name();
                    MediaType mediaType;
                    if ("application/json".equals(contentType) || "body".equals(contentType)) {
                        // This matches the name generated from RAML, e.g. PaymentCardsGetResponseBody
                        // The response is defined using an inline schema
                        String name = getName(resource, ramlMethod) + StringUtils.capitalize(ramlMethod.method())
                            + "ResponseBody";
                        mediaType = convertBody(body, name, components, jsonSchemaToOpenApi);
                        apiResponseContent.addMediaType(contentType, mediaType);
                    }
                }
                apiResponse.setContent(apiResponseContent);
            }

            apiResponses.addApiResponse(responseCode, apiResponse);
        }
        return apiResponses;
    }

    @SuppressWarnings("java:S3776")
    private MediaType convertBody(TypeDeclaration body, String name, Components components,
                                  JsonSchemaToOpenApi jsonSchemaToOpenApi) throws ExportException, DerefenceException {
        Schema bodySchema = null;
        MediaType mediaType = null;

        if (body instanceof JSONTypeDeclaration) {
            mediaType = new MediaType();
            JSONTypeDeclaration jsonType = (JSONTypeDeclaration) body;
            String type = jsonType.type();
            if (type.equals(jsonType.schemaContent())) {
                //Check the if the
                bodySchema = components.getSchemas().get(name);
                if (bodySchema == null) {
                    // Type is inline. parse it as a new one
                    bodySchema = jsonSchemaToOpenApi.convert(name, jsonType);
                    jsonSchemaToOpenApi.dereferenceSchema(bodySchema, components);

                    mediaType.setSchema(bodySchema);
                    Utils.cleanUp(bodySchema, !exporterOptions.isAddJavaTypeExtensions());
                }
                mediaType.setSchema(new Schema().$ref(name));
            } else {
                String schemaName = Utils.getProposedSchemaName(type);
                bodySchema = components.getSchemas().get(schemaName);
                if (bodySchema == null) {
                    log.error("No Schema with the name: {} resolved from: {} is present: ", schemaName,
                        type);
                    throw new ExportException("Invalid Schema");
                }
                mediaType.setSchema(new Schema().$ref(schemaName));
            }

        } else if (body instanceof XMLTypeDeclaration) {
            log.debug("No OpenAPI  schema for: {} ", name);
        } else {
            bodySchema = RamlSchemaToOpenApi.convert(name, body, components);
            mediaType = new MediaType();
            mediaType.setSchema(bodySchema);
        }

        if (mediaType != null) {
            convertExamples(body, mediaType);

            if (bodySchema.getExtensions() != null && bodySchema.getExtensions().containsKey(X_EXAMPLES)) {
                List<Example> examples = (List<Example>) bodySchema.getExtensions().get(X_EXAMPLES);
                for (Example example : examples) {
                    mediaType.addExamples(example.getSummary(), example);
                }
                mediaType.setExample(null);
            }

        }

        return mediaType;
    }

    private void convertExamples(TypeDeclaration body, MediaType mediaType) {
        if (body.examples() != null && !body.examples().isEmpty()) {
            body.examples().forEach(exampleSpec -> {
                Example example = new Example();
                example.setValue(getExampleObject(exampleSpec, exporterOptions.isConvertExamplesToYaml()));
                example.setSummary(exampleSpec.name());
                mediaType.addExamples(exampleSpec.name(), example);
                mediaType.setExample(null);
            });
        } else {

            Object exampleObject = getExampleObject(body.example(), exporterOptions.isConvertExamplesToYaml());
            if (exampleObject != null) {
                Example example = new Example();
                example.setValue(exampleObject);
                mediaType.addExamples(EXAMPLE, example);
                mediaType.setExample(null);
            }
        }
    }


    private String getName(Resource resource, Method ramlMethod) {
        AnnotableStringType annotableStringType = resource.displayName();
        if (annotableStringType != null) {
            String value = annotableStringType.value();
            if (!value.contains("{") && !value.startsWith("/")) {
                return Utils.normalizeSchemaName(value);
            }
        }

        List<String> parts = new ArrayList<>(Arrays.asList(resource.resourcePath().split("/")));
        parts.add(ramlMethod.method());

        return parts.stream()
            .filter(StringUtils::isNotEmpty)
            .map(Exporter::replacePlaceHolder)
            .map(StringUtils::capitalize).collect(Collectors.joining());
    }

    private static String replacePlaceHolder(String part) {
        if (part.contains("{")) {
            return "ById";
        } else {
            return part;
        }
    }

    private String getDescription(Response ramlResponse) {
        String description = getDescription(ramlResponse.description());
        if (description == null) {
            description = "Automagically created by RAML to Open API Exporter. "
                + "Update RAML to include proper description for each response!";
        }
        return description;
    }

    private String getDescription(Resource resource) {
        String description = getDescription(resource.description());
        if (description == null) {
            return "Generated description for " + resource.displayName().value()
                + ". Please update RAML spec to provide description for this resource";
        } else {
            return description;
        }
    }


    private static PathItem.HttpMethod getHttpMethod(Method ramlMethod) {
        return PathItem.HttpMethod.valueOf(ramlMethod.method().toUpperCase());
    }

}
