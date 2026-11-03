package com.backbase.oss.codegen.asyncapi;

import com.backbase.oss.codegen.java.BoatSpringCodeGen;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.features.DocumentationProviderFeatures.DocumentationProvider;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.ModelUtils;

/**
 * Generates Spring Cloud Stream producers and consumers from an AsyncAPI 3.0 document projected onto OpenAPI by
 * {@code AsyncApiOpenApiProjector}.
 *
 * <p>Models come from the {@code boat-spring} templates, so they are identical to the models generated from an
 * OpenAPI document with {@code boat-spring}. Per tag it generates:
 * <ul>
 *   <li>{@code {Tag}Handler}: one method per receive operation, plus one per reply to a send operation.</li>
 *   <li>{@code {Tag}FunctionsConfiguration}: a {@code Consumer} or {@code Function} bean per receive operation that
 *   delegates to the handler, and the publisher bean.</li>
 *   <li>{@code {Tag}Publisher}: one method per send operation, using {@code StreamBridge}.</li>
 * </ul>
 */
@Slf4j
public class BoatSpringCloudStreamCodegen extends BoatSpringCodeGen {

    public static final String NAME = "boat-spring-cloud-stream";
    public static final String LIBRARY = "spring-cloud-stream";
    private static final String LIBRARY_DESCRIPTION = "Spring Cloud Stream functions and StreamBridge publishers.";

    public static final String INVERSE_OPERATIONS = "inverseOperations";
    public static final String GENERATE_BINDING_PROPERTIES = "generateBindingProperties";

    static final String HANDLER_TEMPLATE = "handler.mustache";
    static final String FUNCTIONS_TEMPLATE = "functionsConfiguration.mustache";
    static final String PUBLISHER_TEMPLATE = "publisher.mustache";

    private static final String EXT_ACTION = "x-boat-async-action";
    private static final String EXT_OPERATION = "x-boat-async-operation";
    private static final String EXT_CHANNEL = "x-boat-async-channel";
    private static final String EXT_MESSAGE = "x-boat-async-message";
    private static final String EXT_REPLY = "x-boat-async-reply";
    private static final String SEND = "send";

    private static final Pattern HEADER_LOCATION = Pattern.compile("^\\$message\\.header#/([^/]+)");
    private static final Pattern ADDRESS_PARAMETER = Pattern.compile("\\{([^}]+)}");
    private static final Set<String> METHOD_PARAMETER_NAMES = new HashSet<>(List.of("payload", "headers"));

    /**
     * Generate the code of the other party: send operations become receive operations and vice versa.
     */
    @Getter
    @Setter
    protected boolean inverseOperations;

    /**
     * Generate {@code asyncapi-bindings.properties} with destinations and function definitions.
     */
    @Getter
    @Setter
    protected boolean generateBindingProperties = true;

    private boolean constructed;
    private final Map<String, Set<String>> rolesByClassname = new LinkedHashMap<>();
    private final List<File> roleLessFiles = new ArrayList<>();
    private final List<BoatAsyncOperation> asyncOperations = new ArrayList<>();

    public BoatSpringCloudStreamCodegen() {
        super();
        this.apiNameSuffix = "";

        this.supportedLibraries.clear();
        this.supportedLibraries.put(LIBRARY, LIBRARY_DESCRIPTION);
        setLibrary(LIBRARY);
        this.constructed = true;
        this.cliOptions.removeIf(option -> CodegenConstants.LIBRARY.equals(option.getOpt()));
        CliOption library = new CliOption(CodegenConstants.LIBRARY, CodegenConstants.LIBRARY_DESC)
            .defaultValue(LIBRARY);
        library.setEnum(this.supportedLibraries);
        this.cliOptions.add(library);

        this.cliOptions.add(CliOption.newBoolean(INVERSE_OPERATIONS,
            "Generate the code of the other party: send operations become receive operations and vice versa.",
            this.inverseOperations));
        this.cliOptions.add(CliOption.newBoolean(GENERATE_BINDING_PROPERTIES,
            "Generate asyncapi-bindings.properties with binding destinations and function definitions.",
            this.generateBindingProperties));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public DocumentationProvider defaultDocumentationProvider() {
        return DocumentationProvider.NONE;
    }

    @Override
    public String getHelp() {
        return "Generates Spring Cloud Stream producers and consumers from an AsyncAPI 3.0 document.";
    }

    /**
     * Always uses {@value LIBRARY}. Called from the superclass constructors before this class is initialized.
     */
    @Override
    public void setLibrary(String library) {
        if (constructed && library != null && !LIBRARY.equals(library)) {
            log.warn("{} only supports library {}; ignoring {}", NAME, LIBRARY, library);
        }
        this.supportedLibraries.putIfAbsent(LIBRARY, LIBRARY_DESCRIPTION);
        super.setLibrary(LIBRARY);
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (this.additionalProperties.containsKey(INVERSE_OPERATIONS)) {
            this.inverseOperations = convertPropertyToBoolean(INVERSE_OPERATIONS);
        }
        if (this.additionalProperties.containsKey(GENERATE_BINDING_PROPERTIES)) {
            this.generateBindingProperties = convertPropertyToBoolean(GENERATE_BINDING_PROPERTIES);
        }
        writePropertyBack(INVERSE_OPERATIONS, this.inverseOperations);
        writePropertyBack(GENERATE_BINDING_PROPERTIES, this.generateBindingProperties);

        this.apiTemplateFiles.clear();
        this.apiTemplateFiles.put(HANDLER_TEMPLATE, "Handler.java");
        if (!this.interfaceOnly) {
            this.apiTemplateFiles.put(FUNCTIONS_TEMPLATE, "FunctionsConfiguration.java");
            this.apiTemplateFiles.put(PUBLISHER_TEMPLATE, "Publisher.java");
        }
        this.apiTestTemplateFiles.clear();
        this.apiDocTemplateFiles.clear();

        this.supportingFiles.removeIf(file -> !"BigDecimalCustomSerializer.mustache".equals(file.getTemplateFile()));
        this.supportingFiles.add(new SupportingFile("pom.mustache", "", "pom.xml"));
        if (!this.interfaceOnly) {
            this.supportingFiles.add(new SupportingFile("asyncApiHeaders.mustache",
                (sourceFolder + File.separator + apiPackage).replace(".", File.separator), "AsyncApiHeaders.java"));
        }
        if (this.generateBindingProperties) {
            this.supportingFiles.add(new SupportingFile("asyncapiBindings.mustache",
                ("src/main/resources").replace("/", File.separator), "asyncapi-bindings.properties"));
        }
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, List<Server> servers) {
        CodegenOperation codegenOperation = super.fromOperation(path, httpMethod, operation, servers);
        Map<String, Object> extensions = operation.getExtensions();
        if (extensions == null || !extensions.containsKey(EXT_ACTION)) {
            log.warn("Operation {} is not an AsyncAPI operation; generate from an AsyncAPI document",
                codegenOperation.operationId);
            return codegenOperation;
        }

        BoatAsyncOperation op = new BoatAsyncOperation(codegenOperation);
        boolean send = SEND.equals(extensions.get(EXT_ACTION));
        op.isSend = send != inverseOperations;
        op.isReceive = !op.isSend;
        op.asyncOperationId = string(extensions.get(EXT_OPERATION));
        op.payloadType = op.bodyParam != null ? op.bodyParam.dataType : "Object";

        Map<String, Object> message = map(extensions.get(EXT_MESSAGE));
        op.messageName = string(message.get("name"));
        op.contentType = string(message.get("contentType"));
        op.headersType = headersType(message, op, op.headerNames);
        op.correlationIdHeader = headerName(correlationLocation(message));

        Map<String, Object> channel = map(extensions.get(EXT_CHANNEL));
        op.channelName = string(channel.get("name"));
        op.channelAddress = string(channel.get("address"));
        op.channelParameterized = Boolean.TRUE.equals(channel.get("parameterized"));
        for (Object parameter : list(channel.get("parameters"))) {
            String name = string(map(parameter).get("name"));
            String paramName = toParamName(name);
            if (METHOD_PARAMETER_NAMES.contains(paramName)) {
                paramName = paramName + "Param";
            }
            op.channelParams.add(new BoatAsyncOperation.ChannelParam(name, paramName,
                string(map(parameter).get("description"))));
        }

        op.bindingName = op.operationId + (op.isReceive ? "-in-0" : "-out-0");
        applyDestination(op);

        Map<String, Object> reply = map(extensions.get(EXT_REPLY));
        if (!reply.isEmpty()) {
            applyReply(op, reply);
        }
        return op;
    }

    private void applyDestination(BoatAsyncOperation op) {
        if (op.channelAddress == null) {
            log.warn("Operation {} uses channel {} without an address; configure the destination of binding {}",
                op.operationId, op.channelName, op.bindingName);
            return;
        }
        if (!op.channelParameterized) {
            op.destination = op.channelAddress;
        } else if (op.isReceive) {
            op.destination = ADDRESS_PARAMETER.matcher(op.channelAddress).replaceAll("*");
            log.warn("Operation {} receives from parameterized address {}; binding {} uses wildcard destination {}. "
                    + "Configure the binder to treat it as a pattern (Kafka: destination-is-pattern with a regex, "
                    + "RabbitMQ: binding-routing-key).",
                op.operationId, op.channelAddress, op.bindingName, op.destination);
        } else {
            op.destinationExpression = destinationExpression(op);
        }
    }

    private void applyReply(BoatAsyncOperation op, Map<String, Object> reply) {
        op.hasReply = true;
        op.replyType = op.returnType != null ? op.returnType : "Object";

        Map<String, Object> replyMessage = map(reply.get("message"));
        op.replyHeadersType = headersType(replyMessage, op, op.replyHeaderNames);
        String replyCorrelation = headerName(correlationLocation(replyMessage));
        op.replyCorrelationIdHeader = replyCorrelation != null ? replyCorrelation : op.correlationIdHeader;

        Map<String, Object> replyChannel = map(reply.get("channel"));
        op.replyChannelName = string(replyChannel.get("name"));
        String replyAddress = string(replyChannel.get("address"));
        if (replyAddress != null && !Boolean.TRUE.equals(replyChannel.get("parameterized"))) {
            op.replyDestination = replyAddress;
        }

        String addressLocation = string(reply.get("addressLocation"));
        op.replyAddressHeader = headerName(addressLocation);
        if (addressLocation != null && op.replyAddressHeader == null) {
            log.warn("Operation {}: reply address location {} is not a message header; replies go to the "
                + "destination of the reply binding", op.operationId, addressLocation);
        }

        if (op.isReceive) {
            op.replyBindingName = op.operationId + "-out-0";
        } else {
            op.replyHandlerName = op.operationId + "Reply";
            op.replyBindingName = op.replyHandlerName + "-in-0";
        }
        if (op.replyDestination == null && op.replyAddressHeader == null) {
            log.warn("Operation {}: reply has no static address and no header address location; configure the "
                + "destination of binding {}", op.operationId, op.replyBindingName);
        }
    }

    private String headersType(Map<String, Object> message, CodegenOperation op, List<String> names) {
        String ref = string(map(message.get("headers")).get("$ref"));
        if (ref == null) {
            return null;
        }
        String schemaName = ModelUtils.getSimpleRef(ref);
        Schema<?> schema = this.openAPI != null ? ModelUtils.getSchema(this.openAPI, schemaName) : null;
        if (schema != null && schema.getProperties() != null) {
            names.addAll(schema.getProperties().keySet());
        }
        String type = toModelName(schemaName);
        op.imports.add(type);
        return type;
    }

    private static String correlationLocation(Map<String, Object> message) {
        return string(map(message.get("correlationId")).get("location"));
    }

    static String headerName(String location) {
        if (location == null) {
            return null;
        }
        Matcher matcher = HEADER_LOCATION.matcher(location);
        return matcher.find() ? matcher.group(1).replace("~1", "/").replace("~0", "~") : null;
    }

    private static String destinationExpression(BoatAsyncOperation op) {
        Map<String, String> paramNames = new LinkedHashMap<>();
        op.channelParams.forEach(p -> paramNames.put(p.getName(), p.getParamName()));

        List<String> parts = new ArrayList<>();
        Matcher matcher = ADDRESS_PARAMETER.matcher(op.channelAddress);
        int position = 0;
        while (matcher.find()) {
            if (matcher.start() > position) {
                parts.add(javaString(op.channelAddress.substring(position, matcher.start())));
            }
            String paramName = paramNames.get(matcher.group(1));
            parts.add(paramName != null ? paramName : javaString(matcher.group()));
            position = matcher.end();
        }
        if (position < op.channelAddress.length()) {
            parts.add(javaString(op.channelAddress.substring(position)));
        }
        if (parts.size() == 1 && !parts.get(0).startsWith("\"")) {
            return "String.valueOf(" + parts.get(0) + ")";
        }
        return String.join(" + ", parts);
    }

    private static String javaString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        OperationsMap result = super.postProcessOperationsWithModels(objs, allModels);
        OperationMap operations = result.getOperations();
        Set<String> roles = new LinkedHashSet<>();
        for (CodegenOperation operation : operations.getOperation()) {
            if (operation instanceof BoatAsyncOperation) {
                BoatAsyncOperation op = (BoatAsyncOperation) operation;
                asyncOperations.add(op);
                if (op.isHandled()) {
                    roles.add(HANDLER_TEMPLATE);
                }
                if (op.isSend) {
                    roles.add(PUBLISHER_TEMPLATE);
                }
                roles.add(FUNCTIONS_TEMPLATE);
            }
        }
        rolesByClassname.put(operations.getClassname(), roles);
        result.put("hasHandler", roles.contains(HANDLER_TEMPLATE));
        result.put("hasPublisher", roles.contains(PUBLISHER_TEMPLATE));
        result.put("publisherBeanName", StringUtils.uncapitalize(operations.getClassname()) + "Publisher");
        return result;
    }

    @Override
    public String apiFilename(String templateName, String tag) {
        String filename = super.apiFilename(templateName, tag);
        Set<String> roles = rolesByClassname.getOrDefault(toApiName(tag), Collections.emptySet());
        if (!roles.contains(templateName)) {
            roleLessFiles.add(new File(filename));
        }
        return filename;
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        Map<String, Object> result = super.postProcessSupportingFileData(objs);
        List<String> functions = new ArrayList<>();
        List<String> outputBindings = new ArrayList<>();
        for (BoatAsyncOperation op : asyncOperations) {
            if (op.isReceive) {
                functions.add(op.operationId);
            } else if (op.destinationExpression == null) {
                outputBindings.add(op.operationId);
            }
            if (op.replyHandlerName != null) {
                functions.add(op.replyHandlerName);
            }
        }
        result.put("asyncOperations", asyncOperations);
        result.put("functionDefinition", String.join(";", functions));
        result.put("outputBindings", String.join(";", outputBindings));
        return result;
    }

    /**
     * Removes the per-tag files of roles a tag does not have, e.g. a publisher for a tag without send operations.
     */
    @Override
    public void postProcess() {
        for (File file : roleLessFiles) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                log.warn("Cannot remove {}", file, e);
            }
        }
        super.postProcess();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
    }

    private static List<?> list(Object value) {
        return value instanceof List ? (List<?>) value : Collections.emptyList();
    }

    private static String string(Object value) {
        return value == null ? null : StringUtils.defaultIfEmpty(value.toString(), null);
    }
}
