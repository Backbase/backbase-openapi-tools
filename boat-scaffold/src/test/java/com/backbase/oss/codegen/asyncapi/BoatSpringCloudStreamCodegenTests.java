package com.backbase.oss.codegen.asyncapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.oss.boat.asyncapi.AsyncApiLoader;
import com.backbase.oss.boat.asyncapi.AsyncApiOpenApiProjector;
import com.backbase.oss.codegen.java.MavenProjectCompiler;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.languages.SpringCodegen;

class BoatSpringCloudStreamCodegenTests {

    static final String TEST_OUTPUT = "target/boat-spring-cloud-stream-codegen-tests";
    static final File SHARED_EVENTS =
        new File("../boat-trail-resources/src/main/resources/asyncapi/shared-events/asyncapi.yaml");
    static final File CLOUD_EVENT = new File(
        "../boat-trail-resources/src/main/resources/asyncapi/cloud-event/events/approval-decision-v1/asyncapi.yaml");
    static final File CLOUD_EVENTS = new File(
        "../boat-trail-resources/src/main/resources/asyncapi/cloud-events/approval-events-v1.yaml");
    static final String API_PACKAGE = "com.example.orders.events";
    static final String MODEL_PACKAGE = "com.example.orders.model";

    @BeforeAll
    static void before() throws IOException {
        FileUtils.deleteDirectory(new File(TEST_OUTPUT));
    }

    @Test
    void generatesRolesFromOperationActions() throws Exception {
        File output = new File(TEST_OUTPUT, "actions");
        List<File> files = generate(output, Map.of());
        File api = apiDir(output);

        CompilationUnit ordersHandler = parse(new File(api, "OrdersHandler.java"));
        assertThat(methodNames(ordersHandler), containsInAnyOrder("onOrderStatusChanged", "validateOrder"));
        MethodDeclaration validateOrder = method(ordersHandler, "validateOrder");
        assertThat(validateOrder.getTypeAsString(), equalTo("OrderValidatedPayload"));
        assertThat(parameterTypes(validateOrder), equalTo(List.of("Order", "MessageHeaders")));
        assertThat(parameterTypes(method(ordersHandler, "onOrderStatusChanged")),
            equalTo(List.of("OrderStatusChangedPayload", "MessageHeaders")));

        CompilationUnit ordersPublisher = parse(new File(api, "OrdersPublisher.java"));
        assertThat(methodNames(ordersPublisher), equalTo(List.of("publishOrderPlaced", "publishOrderPlaced")));
        assertThat(parameterTypes(ordersPublisher.findAll(MethodDeclaration.class).get(1)),
            equalTo(List.of("Order", "OrderPlacedHeaders")));

        CompilationUnit ordersFunctions = parse(new File(api, "OrdersFunctionsConfiguration.java"));
        assertThat(methodNames(ordersFunctions),
            containsInAnyOrder("onOrderStatusChanged", "validateOrder", "ordersPublisher"));
        assertThat(method(ordersFunctions, "validateOrder").getTypeAsString(),
            equalTo("Function<Message<Order>,Message<OrderValidatedPayload>>"));
        String functionsSource = ordersFunctions.toString();
        assertThat(functionsSource, containsString("message.getHeaders().get(\"replyTo\")"));
        assertThat(functionsSource, containsString("AsyncApiHeaders.SEND_TO_DESTINATION"));

        CompilationUnit paymentsHandler = parse(new File(api, "PaymentsHandler.java"));
        assertThat(methodNames(paymentsHandler), equalTo(List.of("onPaymentSettled")));
        assertFalse(new File(api, "PaymentsPublisher.java").exists(), "no send operations tagged payments");
        assertThat(fileNames(files), hasItem("PaymentsFunctionsConfiguration.java"));

        File model = new File(output, "src/main/java/" + MODEL_PACKAGE.replace('.', '/'));
        assertThat(List.of(model.list()), hasItem("Order.java"));
        assertThat(List.of(model.list()), hasItem("OrderPlacedHeaders.java"));
        assertThat(List.of(model.list()), hasItem("PaymentSettledPayload.java"));

        assertActionBindings(output);
        compile(output);
    }

    private static void assertActionBindings(File output) throws IOException {
        String bindings = read(new File(output, "src/main/resources/asyncapi-bindings.properties"));
        assertThat(bindings, containsString(
            "spring.cloud.function.definition=onOrderStatusChanged;validateOrder;onPaymentSettled"));
        assertThat(bindings, containsString("spring.cloud.stream.output-bindings=publishOrderPlaced"));
        assertThat(bindings, containsString(
            "spring.cloud.stream.bindings.publishOrderPlaced-out-0.destination=orders.placed"));
        assertThat(bindings, containsString(
            "spring.cloud.stream.bindings.onOrderStatusChanged-in-0.destination=orders.*.status"));
        assertThat(bindings, containsString(
            "spring.cloud.stream.bindings.validateOrder-in-0.destination=orders.validation.requests"));
    }

    @Test
    void inverseOperationsSwapsRoles() throws Exception {
        File output = new File(TEST_OUTPUT, "inverse");
        generate(output, Map.of(BoatSpringCloudStreamCodegen.INVERSE_OPERATIONS, "true"));
        File api = apiDir(output);

        CompilationUnit ordersPublisher = parse(new File(api, "OrdersPublisher.java"));
        assertThat(ordersPublisher.findAll(MethodDeclaration.class).stream()
                .map(MethodDeclaration::getNameAsString).distinct().collect(Collectors.toList()),
            containsInAnyOrder("onOrderStatusChanged", "validateOrder"));
        MethodDeclaration statusChanged = ordersPublisher.findAll(MethodDeclaration.class).stream()
            .filter(m -> m.getNameAsString().equals("onOrderStatusChanged"))
            .findFirst().get();
        assertThat(parameterTypes(statusChanged), equalTo(List.of("String", "OrderStatusChangedPayload")));
        assertThat(statusChanged.getParameter(0).getNameAsString(), equalTo("region"));
        String publisherSource = ordersPublisher.toString();
        assertThat(publisherSource, containsString("\"orders.\" + region + \".status\""));

        CompilationUnit ordersHandler = parse(new File(api, "OrdersHandler.java"));
        assertThat(methodNames(ordersHandler), containsInAnyOrder("publishOrderPlaced", "validateOrderReply"));
        assertThat(parameterTypes(method(ordersHandler, "publishOrderPlaced")),
            equalTo(List.of("Order", "OrderPlacedHeaders")));

        assertTrue(new File(api, "PaymentsPublisher.java").exists());
        assertFalse(new File(api, "PaymentsHandler.java").exists(), "no receive operations tagged payments");

        String bindings = read(new File(output, "src/main/resources/asyncapi-bindings.properties"));
        assertThat(bindings, containsString("spring.cloud.function.definition=publishOrderPlaced;validateOrderReply"));
        assertThat(bindings, containsString("spring.cloud.stream.output-bindings=validateOrder;onPaymentSettled"));

        compile(output);
    }

    @Test
    void interfaceOnlyGeneratesHandlersAndModels() throws Exception {
        File output = new File(TEST_OUTPUT, "interface-only");
        generate(output, Map.of(SpringCodegen.INTERFACE_ONLY, "true",
            BoatSpringCloudStreamCodegen.GENERATE_BINDING_PROPERTIES, "false"));
        File api = apiDir(output);

        assertThat(List.of(api.list()), containsInAnyOrder("OrdersHandler.java", "PaymentsHandler.java"));
        assertFalse(new File(output, "src/main/resources/asyncapi-bindings.properties").exists());
    }

    @Test
    void generatesJsonSchema2020ComponentsUnderTheirKeys() throws Exception {
        File output = new File(TEST_OUTPUT, "cloud-event");
        OpenAPI openAPI = new AsyncApiOpenApiProjector().setOpenApiVersion("3.1.0")
            .project(AsyncApiLoader.load(CLOUD_EVENT)).getOpenAPI();
        generate(output, CLOUD_EVENT, openAPI, Map.of());

        File model = new File(output, "src/main/java/" + MODEL_PACKAGE.replace('.', '/'));
        assertThat(List.of(model.list()), containsInAnyOrder("ApprovalDecisionCloudEvent.java",
            "ApprovalDecisionData.java", "RequestType.java", "Scope.java", "BigDecimalCustomSerializer.java"));
        CompilationUnit publisher = parse(new File(apiDir(output), "ApprovalDecisionPublisher.java"));
        assertThat(parameterTypes(publisher.findAll(MethodDeclaration.class).get(0)),
            hasItem("ApprovalDecisionCloudEvent"));

        compile(output);
    }

    @Test
    void generatesHandlerAndPublisherPerChannelOfUntaggedOperations() throws Exception {
        File output = new File(TEST_OUTPUT, "cloud-events");
        OpenAPI openAPI = new AsyncApiOpenApiProjector().setOpenApiVersion("3.1.0")
            .project(AsyncApiLoader.load(CLOUD_EVENTS)).getOpenAPI();
        generate(output, CLOUD_EVENTS, openAPI, Map.of());
        File api = apiDir(output);

        assertThat(List.of(api.list()), containsInAnyOrder("ApprovalRequestedHandler.java",
            "ApprovalRequestedFunctionsConfiguration.java", "ApprovalDecisionPublisher.java",
            "ApprovalDecisionFunctionsConfiguration.java", "AsyncApiHeaders.java"));
        assertThat(methodNames(parse(new File(api, "ApprovalRequestedHandler.java"))),
            equalTo(List.of("receiveApprovalRequested")));
        assertThat(parameterTypes(method(parse(new File(api, "ApprovalDecisionPublisher.java")),
            "sendApprovalDecision")), hasItem("ApprovalDecisionCloudEvent"));

        File model = new File(output, "src/main/java/" + MODEL_PACKAGE.replace('.', '/'));
        assertThat(List.of(model.list()), containsInAnyOrder("ApprovalRequestedCloudEvent.java",
            "ApprovalRequestedData.java", "ApprovalDecisionCloudEvent.java", "ApprovalDecisionData.java",
            "RequestType.java", "Scope.java", "BigDecimalCustomSerializer.java"));

        String bindings = read(new File(output, "src/main/resources/asyncapi-bindings.properties"));
        assertThat(bindings, containsString("spring.cloud.function.definition=receiveApprovalRequested\n"));
        assertThat(bindings, containsString("spring.cloud.stream.output-bindings=sendApprovalDecision\n"));
        assertThat(bindings, containsString(
            "spring.cloud.stream.bindings.receiveApprovalRequested-in-0.destination=approval-requested"));
        assertThat(bindings, containsString(
            "spring.cloud.stream.bindings.sendApprovalDecision-out-0.destination=approval-decision"));

        compile(output);
    }

    @Test
    void headerName() {
        assertThat(BoatSpringCloudStreamCodegen.headerName("$message.header#/replyTo"), equalTo("replyTo"));
        assertThat(BoatSpringCloudStreamCodegen.headerName("$message.header#/a~1b/c"), equalTo("a/b"));
        assertEquals(null, BoatSpringCloudStreamCodegen.headerName("$message.payload#/replyTo"));
        assertEquals(null, BoatSpringCloudStreamCodegen.headerName(null));
    }

    @Test
    void libraryIsFixed() {
        BoatSpringCloudStreamCodegen codegen = new BoatSpringCloudStreamCodegen();
        codegen.setLibrary("spring-boot");
        assertThat(codegen.getLibrary(), equalTo(BoatSpringCloudStreamCodegen.LIBRARY));
        assertThat(codegen.getName(), equalTo("boat-spring-cloud-stream"));
    }

    private static List<File> generate(File output, Map<String, String> options) throws Exception {
        OpenAPI openAPI = new AsyncApiOpenApiProjector().project(AsyncApiLoader.load(SHARED_EVENTS)).getOpenAPI();
        return generate(output, SHARED_EVENTS, openAPI, options);
    }

    private static List<File> generate(File output, File spec, OpenAPI openAPI, Map<String, String> options) {
        BoatSpringCloudStreamCodegen codegen = new BoatSpringCloudStreamCodegen();
        codegen.setOutputDir(output.getAbsolutePath());
        codegen.setInputSpec(spec.getAbsolutePath());
        codegen.additionalProperties().put(CodegenConstants.API_PACKAGE, API_PACKAGE);
        codegen.additionalProperties().put(CodegenConstants.MODEL_PACKAGE, MODEL_PACKAGE);
        codegen.additionalProperties().put("useSpringBoot3", "true");
        codegen.additionalProperties().put("openApiNullable", "false");
        codegen.additionalProperties().putAll(options);

        ClientOptInput input = new ClientOptInput();
        input.config(codegen);
        input.openAPI(openAPI);
        List<File> files = new DefaultGenerator().opts(input).generate();
        assertThat(files, not(empty()));
        return files;
    }

    private static void compile(File projectDir) {
        MavenProjectCompiler compiler = new MavenProjectCompiler(BoatSpringCloudStreamCodegenTests.class.getClassLoader());
        assertEquals(0, compiler.compile(projectDir),
            "Failed compilation. see " + projectDir.getAbsolutePath() + "/mvn.log for details");
    }

    private static File apiDir(File output) {
        return new File(output, "src/main/java/" + API_PACKAGE.replace('.', '/'));
    }

    private static CompilationUnit parse(File file) throws IOException {
        assertTrue(file.exists(), file + " exists");
        return StaticJavaParser.parse(file);
    }

    private static List<String> methodNames(CompilationUnit unit) {
        return unit.findAll(MethodDeclaration.class).stream()
            .map(MethodDeclaration::getNameAsString)
            .collect(Collectors.toList());
    }

    private static MethodDeclaration method(CompilationUnit unit, String name) {
        return unit.findAll(MethodDeclaration.class).stream()
            .filter(m -> m.getNameAsString().equals(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No method " + name));
    }

    private static List<String> parameterTypes(MethodDeclaration method) {
        return method.getParameters().stream().map(Parameter::getTypeAsString).collect(Collectors.toList());
    }

    private static List<String> fileNames(List<File> files) {
        return files.stream().map(File::getName).collect(Collectors.toList());
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
