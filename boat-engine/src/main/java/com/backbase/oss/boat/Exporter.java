package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.CachingResourceLoader;
import com.backbase.oss.boat.loader.RamlResourceLoader;
import com.backbase.oss.boat.transformers.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.jknack.handlebars.internal.Files;
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
import io.swagger.v3.oas.models.servers.ServerVariables;
import io.swagger.v3.oas.models.tags.Tag;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
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

@SuppressWarnings("rawtypes")
public class Exporter {

    private static final Logger log = LoggerFactory.getLogger(Exporter.class);
    private static final Pattern placeholderPattern = Pattern.compile("(\\{[^\\}]*\\})");
    public static final String X_EXAMPLES = "x-examples";
    public static final String NO_DESCRIPTION_AVAILABLE = "No description available";
    private static ObjectMapper mapper = Utils.createObjectMapper();

    private static boolean addJavaTypeExtensions;

    private Exporter() {
        throw new AssertionError("Private constructor");
    }

    public static OpenAPI export(File inputFile, boolean addJavaTypeExtensions, List<Transformer> transformers)
        throws ExportException {
        OpenAPI exported = export(inputFile, addJavaTypeExtensions);
        transformers.forEach(transformer -> transformer.transform(exported, new HashMap()));
        return exported;
    }


    public static OpenAPI export(File inputFile, boolean addJavaTypeExtensions) throws ExportException {

        // Guess Service Name
        AtomicReference<String> serviceName = new AtomicReference<>("serviceName");

        Arrays.stream(inputFile.getPath().split("/"))
            .filter(part -> part.endsWith("-spec"))
            .findFirst()
            .ifPresent(s -> serviceName.set(s.replaceAll("-spec", "-service")));

        return export(serviceName.get(), inputFile, addJavaTypeExtensions);
    }

    public static OpenAPI export(String serviceName, File inputFile, boolean addJavaTypeExtensions)
        throws ExportException {

        File parentFile = inputFile.getParentFile();
        URL baseUrl;
        try {
            baseUrl = parentFile.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new ExportException("Unable to determine parent location", e);
        }

        Map<String, String> ramlTypeReferences = new TreeMap<>();
        // Parse raml document as yaml instead to reverse engineer json references from types
        try {
            String ramlAsString = Files.read(inputFile, Charset.defaultCharset());
            JsonNode jsonNode = mapper.readTree(ramlAsString);
            parseRamlTypeReferences(baseUrl, ramlTypeReferences, jsonNode);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExportException("Cannot read yaml file", e);
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
            ramlTypeReferences,
            addJavaTypeExtensions);

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
                    .map(exampleSpec -> {
                        return new Example()
                            .value(ExampleUtils.getExampleObject(exampleSpec, true))
                            .summary(exampleSpec.name());
                    })
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
                throw new ExportException("Cannot dereference json schema: " + schema.getName(), e);
            }
        }
        components.getSchemas().values().forEach(Utils::cleanUp);

        Info info = setupInfo(ramlApi);
        List<Tag> tags = setupTags(ramlApi);

        String ramlBaseUrl = ramlApi.baseUri() != null ? ramlApi.baseUri().value() : "/";
        if (ramlBaseUrl.contains("{version}")) {
            ramlBaseUrl = StringUtils.replace(ramlBaseUrl, "{version}", ramlApi.version().value());
        }

        String url = "/" + serviceName + "/" + ramlBaseUrl;

        List<Server> servers = new LinkedList<>();
        servers.add(
            new Server()
                .url(url)
                .description("The server")
                .variables(new ServerVariables()));

        Paths paths = new Paths();

        try {
            convertResources(ramlApi.resources(), paths, components, jsonSchemaToOpenApi, operations);
        } catch (DerefenceException e) {
            throw new ExportException("Failed to dereference resources", e);
        }

        OpenAPI openAPI = new OpenAPI();
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
                throw new ExportException("Cannot dereference json schema: " + schema.getName(), e);
            }
        }
        components.getSchemas().values().forEach(Utils::cleanUp);

        return openAPI;
    }

    private static Info setupInfo(Api ramlApi) {
        String description = getDescription(ramlApi.description());
        log.debug("Setup Description");

        String value = ramlApi.version() != null ? ramlApi.version().value() : "1.0";
        Info info = new Info()
            .title(ramlApi.title().value())
            .version(value);

        if (ramlApi.documentation() != null && !ramlApi.documentation().isEmpty()) {
            final StringBuilder markdown = new StringBuilder();
            ramlApi.documentation().stream().forEach(
                documentationItem -> {
                    MarkdownString content = documentationItem.content();

                    String markdownString = cleanupMarkdownString(content.value());

                    markdown.append(markdownString);
                    markdown.append("\n");
                }
            );

            if (info.getDescription() == null) {
                info.setDescription(markdown.toString());
            } else {
                info.setDescription(info.getDescription() + "\n" + markdown);
            }
        } else {
            info.setDescription(NO_DESCRIPTION_AVAILABLE);
        }
        return info;
    }

    private static List<Tag> setupTags(Api ramlApi) {
        String title = ramlApi.title().value().toLowerCase(Locale.ROOT);
        return Collections.singletonList(new Tag().name(title));
    }

    private static String cleanupMarkdownString(String value) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] lines = value.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i == 0 && lines[i].startsWith("#")) {
                String title = "# " + StringUtils.substringAfterLast(lines[i], "#").trim();
                stringBuilder.append(title).append("\n");
            } else {
                stringBuilder.append(lines[i]).append("\n");
            }
        }
        return stringBuilder.toString().trim();
    }

    private static void parseRamlTypeReferences(URL baseUrl, Map<String, String> ramlTypeReferences,
        JsonNode jsonNode) {
        if (jsonNode.hasNonNull("types")) {
            ObjectNode types = (ObjectNode) jsonNode.get("types");
            types.fields().forEachRemaining(nodeEntry -> {
                parseRamlRefEntry(baseUrl, ramlTypeReferences, nodeEntry);
            });
        }
        if (jsonNode.hasNonNull("schemas")) {
            ObjectNode schemas = (ObjectNode) jsonNode.get("schemas");
            schemas.fields().forEachRemaining(nodeEntry -> {
                parseRamlRefEntry(baseUrl, ramlTypeReferences, nodeEntry);
            });
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

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        }


    }

    private static void parseRamlRefEntry(URL baseUrl, Map<String, String> ramlTypeReferences,
        Map.Entry<String, JsonNode> nodeEntry) {
        String key = nodeEntry.getKey();
        JsonNode typeReference = nodeEntry.getValue();
        if (typeReference.has("type") && typeReference instanceof ObjectNode) {
            JsonNode typeNode = typeReference.get("type");
            if (typeNode instanceof TextNode && Utils.isUrl(typeNode.textValue())) {
                URL absoluteReference = Utils.getAbsoluteReference(baseUrl, typeNode.textValue());
                ramlTypeReferences.put(key, absoluteReference.toString());
                log.debug("Add raml type reference: {} to: {}", key, absoluteReference);
            } else {
                log.debug("Cannot create raml ref for: {}", typeNode.textValue());
            }
        } else if (typeReference instanceof TextNode && typeReference.textValue().endsWith(".json")) {
            URL absoluteReference = Utils.getAbsoluteReference(baseUrl, typeReference.textValue());
            ramlTypeReferences.put(key, absoluteReference.toString());
            log.debug("Add raml type reference: {} to: {}", key, absoluteReference);
        }
    }

    private static Map<String, TypeDeclaration> collectTypesFromRamlSpec(Api ramlApi) {
        Map<String, TypeDeclaration> types = new TreeMap<>();

        for (Library library : ramlApi.uses()) {
            library.schemas().forEach(typeDeclaration -> types.put(typeDeclaration.name(), typeDeclaration));
            library.types().forEach(typeDeclaration -> types.put(typeDeclaration.name(), typeDeclaration));
        }
        ramlApi.schemas().forEach(typeDeclaration -> types.put(typeDeclaration.name(), typeDeclaration));
        ramlApi.types().forEach(typeDeclaration -> types.put(typeDeclaration.name(), typeDeclaration));
        return types;
    }

    private static void validateRamlModelResult(File file, RamlModelResult ramlModelResult) throws ExportException {
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

    private static void convertResources(List<Resource> resources, Paths paths, Components components,
        JsonSchemaToOpenApi jsonSchemaToOpenApi, List<Operation> operations)
        throws ExportException, DerefenceException {
        for (Resource resource : resources) {
            log.debug("Mapping RAML Resource displayName: {} relativeUrl: {} with description: {} resourcePath: {}",
                resource.displayName().value(),
                resource.relativeUri().value(),
                resource.description() != null ? resource.description().value() : null,
                resource.resourcePath());

            PathItem pathItem = convertResource(resource.resourcePath(), resource, components, jsonSchemaToOpenApi,
                operations);
            if (pathItem.readOperations().size() > 0) {
                paths.addPathItem(resource.resourcePath(), pathItem);
            }
            convertResources(resource.resources(), paths, components, jsonSchemaToOpenApi, operations);
        }
    }

    private static PathItem convertResource(String resourcePath, Resource resource, Components components,
        JsonSchemaToOpenApi jsonSchemaToOpenApi, List<Operation> operationss)
        throws ExportException, DerefenceException {
        PathItem pathItem = new PathItem();
        pathItem.summary(getDisplayName(resource.displayName()));
        pathItem.description(getDescription(resource));
        mapUriParameters(resourcePath, resource, pathItem, components);
        mapMethods(resourcePath, resource, pathItem, components, jsonSchemaToOpenApi, operationss);
        return pathItem;
    }

    private static void mapUriParameters(String resourcePath, Resource resource, PathItem pathItem,
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
                    log.warn("{} has double Parameter {} in path: {} Detected. ignoring", resourcePath,
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
    private static void resolveUnspecifiedPathParameters(Resource resource, PathItem pathItem) {
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

    private static void log(TypeDeclaration typeDeclaration) {
        String description = getDescription(typeDeclaration.description());
        if (log.isDebugEnabled()) {
            log.debug("Type name: {} type: {} displayName: {} defaultValue: {} description: {}"
                , typeDeclaration.name(), typeDeclaration.type(), getDisplayName(typeDeclaration.displayName()),
                typeDeclaration.defaultValue(), description);

        }
    }


    private static String getDisplayName(AnnotableStringType parameter) {
        return parameter != null ? parameter.value() : null;
    }

    private static String getDescription(MarkdownString description) {
        if (description == null) {
            return NO_DESCRIPTION_AVAILABLE;
        }
        String result = description.value();
        return StringEscapeUtils.unescapeJavaScript(result);
    }

    private static void mapMethods(String resourcePath, Resource resource, PathItem pathItem, Components components,
        JsonSchemaToOpenApi jsonSchemaToOpenApi, List<Operation> operations)
        throws ExportException, DerefenceException {
        for (Method ramlMethod : resource.methods()) {
            PathItem.HttpMethod httpMethod = getHttpMethod(ramlMethod);
            ApiResponses apiResponses = mapResponses(resource, ramlMethod, components, jsonSchemaToOpenApi);

            ArrayList<Parameter> parameters = new ArrayList<>();
            addHeaders(ramlMethod, parameters, components);
            addQueryParameters(ramlMethod, parameters, components);

            RequestBody requestBody = convertRequestBody(resource, ramlMethod, components, jsonSchemaToOpenApi);

            String tag = Arrays.stream(resourcePath.substring(1).split("/")).findFirst().orElse("tag");
            String operationId = getOperationId(resource, ramlMethod, operations, tag, requestBody, resourcePath);
            String description = getDescription(ramlMethod.description());
            String summary = getSummary(ramlMethod.description());

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

    private static String getSummary(MarkdownString description) {
        if (description == null) {
            return null;
        }
        return Stream.of(description.value().split("\n"))
            .findFirst()
            .map(firstLine -> firstLine.replaceAll("#", ""))
            .map(String::trim)
            .map(firstLine -> firstLine.endsWith(".") ? firstLine : firstLine + ".")
            .orElse(null);
    }

    private static String getOperationId(Resource resource, Method ramlMethod, List<Operation> operations, String tag,
        RequestBody requestBody, String resourcePath) {
        AnnotableStringType annotableStringType = ramlMethod.displayName();
        String httpMethod;
        if (annotableStringType == null) {
            httpMethod = ramlMethod.method();
        } else {
            httpMethod = annotableStringType.value();
        }
        String resourceName = resource.displayName().value();

        String operationId = httpMethod;

        // If operationId is equal the http method name, take the display name  or resource path name of the raml resource
        if (operationId.equalsIgnoreCase(ramlMethod.method())) {
            operationId = Utils.normalizeDisplayName(StringUtils.substringAfterLast(resource.resourcePath(), "/"));
        }

        // If that name contains spaces, concat the name with capitalizing each word
        if (operationId.contains(" ")) {
            operationId = Arrays.stream(operationId.split(" ")).map(StringUtils::capitalize)
                .collect(Collectors.joining());
        }

        // prepend http name ot to the operationId and ensure the rest has a capital
        operationId = ramlMethod.method() + StringUtils.capitalize(operationId);

        // path has path parameter, add By<PathParam> if not already there and hope for the best.
        Set<String> placeHolders = Utils.getPlaceholders(resource.resourcePath());
        if (!placeHolders.isEmpty()) {
            String suffix =
                "By" + placeHolders.stream().map(StringUtils::capitalize).collect(Collectors.joining("And"));
            if (!operationId.toLowerCase().endsWith(suffix.toLowerCase())) {
                operationId += suffix;
            }
        }
        // Ensure format is lower camel case.
        operationId = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL).convert(operationId);

        String finalOperationId = operationId;
        if (operationIdExists(operations, finalOperationId) && requestBody != null) {
            Optional<MediaType> first = requestBody.getContent().values().stream().findFirst();
            if (first.isPresent()) {
                String $ref = first.get().getSchema().get$ref();
                String name = first.get().getSchema().getName();
                String suffix = name;
                if (suffix == null && $ref != null) {
                    suffix = StringUtils.substringAfterLast($ref, "/");
                }
                operationId += "With" + suffix;
            } else {
                operationId += "Duplicate";
            }
        }

        if (operationIdExists(operations, finalOperationId)) {
            // So many ways these things go wrong. Now trying last part of resource path before placeholder
            operationId += httpMethod + StringUtils.capitalize(Utils.normalizeDisplayName(resourceName));

        }

        log.debug("Resolve operationId: {} from resource: {} with method: {} and path: {}", operationId,
            resource.displayName().value(), httpMethod, resource.resourcePath());
        return operationId;
    }

    private static boolean operationIdExists(List<Operation> operations, String finalOperationId) {
        return operations.stream().anyMatch(operation -> operation.getOperationId().equals(finalOperationId));
    }

    private static void processMethodAnnotations(String resourcePath, Components components, Method ramlMethod,
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
                if (property.isArray()) {
                    operation.addExtension("-" + annotationSchema.getName() + "-" + property.name(), property.values());
                } else {
                    operation.addExtension("x-" + annotationSchema.getName() + "-" + property.name(),
                        property.value().value());
                }
            }
        }
    }

    private static Schema getAnnotationSchema(String resourcePath, Components components, TypeDeclaration annotation,
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
                Utils.cleanUp(annotationSchema);
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

    private static RequestBody convertRequestBody(Resource resource, Method ramlMethod, Components components,
        JsonSchemaToOpenApi jsonSchemaToOpenApi) throws DerefenceException, ExportException {
        if (ramlMethod.body() == null || ramlMethod.body().size() == 0) {
            return null;
        }

        Content content = new Content();
        for (TypeDeclaration body : ramlMethod.body()) {
            String name = getName(resource, ramlMethod) + "Response";
            MediaType mediaType = convertBody(body, name, components, jsonSchemaToOpenApi);
            content.addMediaType(body.name(), mediaType);
        }
        RequestBody requestBody = new RequestBody();
        requestBody.setContent(content);
        requestBody.setDescription(ramlMethod.description() != null ? getDescription(ramlMethod.description()) : null);
        return requestBody;
    }

    private static void addQueryParameters(Method ramlMethod, ArrayList<Parameter> parameters, Components components) {
        ramlMethod.queryParameters().forEach(typeDeclaration -> {
            Parameter parameter = new QueryParameter();
            convertTypeToParameter(typeDeclaration, parameter, components);
            parameters.add(parameter);
        });
    }

    private static void addHeaders(Method ramlMethod, ArrayList<Parameter> parameters, Components components) {
        ramlMethod.headers().forEach(type -> {
            Parameter parameter = new HeaderParameter();
            convertTypeToParameter(type, parameter, components);
            parameters.add(parameter);
        });
    }

    private static void convertTypeToParameter(TypeDeclaration typeDeclaration, Parameter parameter,
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
                example.setValue(ExampleUtils.getExampleObject(exampleSpec, true));
                example.setSummary(exampleSpec.name());
                parameter.getExamples().put(exampleSpec.name(), example);
            });
        } else {
            parameter.setExample(ExampleUtils.getExampleObject(typeDeclaration.example(), true));
        }

    }

    private static ApiResponses mapResponses(Resource resource, Method ramlMethod, Components components,
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

    private static MediaType convertBody(TypeDeclaration body, String name, Components components,
        JsonSchemaToOpenApi jsonSchemaToOpenApi) throws ExportException, DerefenceException {
        Schema bodySchema = null;
        MediaType mediaType = null;

        if (body instanceof JSONTypeDeclaration) {
            mediaType = new MediaType();
            JSONTypeDeclaration jsonType = (JSONTypeDeclaration) body;
            if (jsonType.type().equals(jsonType.schemaContent())) {
                //Check the if the
                bodySchema = components.getSchemas().get(name);
                if (bodySchema == null) {
                    // Type is inline. parse it as a new one
                    bodySchema = jsonSchemaToOpenApi.convert(name, jsonType);
                    jsonSchemaToOpenApi.dereferenceSchema(bodySchema, components);

                    mediaType.setSchema(bodySchema);
                    Utils.cleanUp(bodySchema);
                }
                mediaType.setSchema(new Schema().$ref(name));
            } else {
                String schemaName = Utils.getProposedSchemaName(jsonType.type());
                bodySchema = components.getSchemas().get(schemaName);
                if (bodySchema == null) {
                    log.error("No Schema with the name: {} resolved from: {} is present: ", schemaName,
                        jsonType.type());
                    throw new ExportException("Invalid Schema");
                }
                mediaType.setSchema(new Schema().$ref(schemaName));
            }

        } else if (body instanceof XMLTypeDeclaration) {
//            bodySchema = XmlSchemaToOpenApi.convert(name, ((XMLTypeDeclaration)body).schemaContent(), components);
            log.debug("No OpenAPI  schema for: " + name);
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

    private static void convertExamples(TypeDeclaration body, MediaType mediaType) {
        if (body.examples() != null && !body.examples().isEmpty()) {
            body.examples().forEach(exampleSpec -> {
                Example example = new Example();
                example.setValue(ExampleUtils.getExampleObject(exampleSpec, true));
                example.setSummary(exampleSpec.name());
                mediaType.addExamples(exampleSpec.name(), example);
                mediaType.setExample(null);
            });
        } else {
            mediaType.setExamples(null);
            mediaType.setExample(ExampleUtils.getExampleObject(body.example(), true));
        }
    }


    private static String getName(Resource resource, Method ramlMethod) {
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

    private static String getDescription(Response ramlResponse) {
        String description = getDescription(ramlResponse.description());
        if (description == null) {
            description = "Automagically created by RAML to Open API Exporter. Update RAML to include proper description for each response!";
        }
        return description;
    }

    private static String getDescription(Resource resource) {
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
