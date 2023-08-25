package com.backbase.oss.codegen.swift5;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.languages.Swift5ClientCodegen;
import org.openapitools.codegen.meta.GeneratorMetadata;
import org.openapitools.codegen.meta.Stability;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.CamelizeOption;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.concurrent.TimeUnit;

import static org.openapitools.codegen.utils.StringUtils.camelize;

public class BoatSwift5Codegen extends Swift5ClientCodegen {
    private final Logger LOGGER = LoggerFactory.getLogger(BoatSwift5Codegen.class);

    public static final String PROJECT_NAME = "projectName";
    public static final String RESPONSE_AS = "responseAs";
    public static final String DEPENDENCY_MANAGEMENT = "dependenciesAs";
    public static final String OBJC_COMPATIBLE = "objcCompatible";
    public static final String POD_SOURCE = "podSource";
    public static final String POD_AUTHORS = "podAuthors";
    public static final String POD_SOCIAL_MEDIA_URL = "podSocialMediaURL";
    public static final String POD_LICENSE = "podLicense";
    public static final String POD_HOMEPAGE = "podHomepage";
    public static final String POD_SUMMARY = "podSummary";
    public static final String POD_DESCRIPTION = "podDescription";
    public static final String POD_SCREENSHOTS = "podScreenshots";
    public static final String POD_DOCUMENTATION_URL = "podDocumentationURL";
    public static final String READONLY_PROPERTIES = "readonlyProperties";
    public static final String REMOVE_MIGRATION_PROJECT_NAME_CLASS = "removeMigrationProjectNameClass";
    public static final String SWIFT_USE_API_NAMESPACE = "swiftUseApiNamespace";
    public static final String DEFAULT_POD_AUTHORS = "OpenAPI Generator";
    public static final String LENIENT_TYPE_CAST = "lenientTypeCast";
    public static final String USE_SPM_FILE_STRUCTURE = "useSPMFileStructure";
    public static final String SWIFT_PACKAGE_PATH = "swiftPackagePath";
    public static final String USE_CLASSES = "useClasses";
    public static final String USE_BACKTICK_ESCAPES = "useBacktickEscapes";
    public static final String GENERATE_MODEL_ADDITIONAL_PROPERTIES = "generateModelAdditionalProperties";
    public static final String HASHABLE_MODELS = "hashableModels";
    public static final String USE_JSON_ENCODABLE = "useJsonEncodable";
    public static final String MAP_FILE_BINARY_TO_DATA = "mapFileBinaryToData";
    public static final String USE_CUSTOM_DATE_WITHOUT_TIME = "useCustomDateWithoutTime";
    public static final String VALIDATABLE = "validatable";
    protected static final String LIBRARY_ALAMOFIRE = "alamofire";
    protected static final String LIBRARY_URLSESSION = "urlsession";
    protected static final String LIBRARY_VAPOR = "vapor";
    protected static final String LIBRARY_DBS = "dbsDataProvider";
    protected static final String RESPONSE_LIBRARY_PROMISE_KIT = "PromiseKit";
    protected static final String RESPONSE_LIBRARY_RX_SWIFT = "RxSwift";
    protected static final String RESPONSE_LIBRARY_RESULT = "Result";
    protected static final String RESPONSE_LIBRARY_COMBINE = "Combine";
    protected static final String RESPONSE_LIBRARY_ASYNC_AWAIT = "AsyncAwait";
    protected static final String[] RESPONSE_LIBRARIES = {RESPONSE_LIBRARY_PROMISE_KIT, RESPONSE_LIBRARY_RX_SWIFT, RESPONSE_LIBRARY_RESULT, RESPONSE_LIBRARY_COMBINE, RESPONSE_LIBRARY_ASYNC_AWAIT};
    protected static final String DEPENDENCY_MANAGEMENT_PODFILE = "Podfile";
    protected static final String DEPENDENCY_MANAGEMENT_CARTFILE = "Cartfile";
    protected static final String[] DEPENDENCY_MANAGEMENT_OPTIONS = {DEPENDENCY_MANAGEMENT_CARTFILE, DEPENDENCY_MANAGEMENT_PODFILE};
    protected String projectName = "OpenAPIClient";
    protected boolean nonPublicApi = false;
    protected boolean objcCompatible = false;
    protected boolean lenientTypeCast = false;
    protected boolean readonlyProperties = false;
    protected boolean removeMigrationProjectNameClass = false;
    protected boolean swiftUseApiNamespace = false;
    protected boolean useSPMFileStructure = false;
    protected String swiftPackagePath = "Classes" + File.separator + "OpenAPIs";
    protected boolean useClasses = false;
    protected boolean useBacktickEscapes = false;
    protected boolean generateModelAdditionalProperties = true;
    protected boolean hashableModels = true;
    protected boolean useJsonEncodable = true;
    protected boolean mapFileBinaryToData = false;
    protected boolean useCustomDateWithoutTime = false;
    protected boolean validatable = true;
    protected String[] responseAs = new String[0];
    protected String[] dependenciesAs = new String[0];
    protected String sourceFolder = swiftPackagePath;
    protected HashSet objcReservedWords;
    protected String apiDocPath = "docs/";
    protected String modelDocPath = "docs/";

    /**
     * Constructor for the swift5 language codegen module.
     */
    public BoatSwift5Codegen() {
        super();
        this.useOneOfInterfaces = true;

        generatorMetadata = GeneratorMetadata.newBuilder(generatorMetadata)
                .stability(Stability.STABLE)
                .build();

        outputFolder = "generated-code" + File.separator + "swift";
        modelTemplateFiles.put("model.mustache", ".swift");
        apiTemplateFiles.put("api.mustache", ".swift");
        embeddedTemplateDir = templateDir = "boat-swift5";
        apiPackage = File.separator + "APIs";
        modelPackage = File.separator + "Models";
        modelDocTemplateFiles.put("model_doc.mustache", ".md");
        apiDocTemplateFiles.put("api_doc.mustache", ".md");

        languageSpecificPrimitives = new HashSet<>(
                Arrays.asList(
                        "Int",
                        "Int32",
                        "Int64",
                        "Float",
                        "Double",
                        "Bool",
                        "Void",
                        "String",
                        "Data",
                        "Date",
                        "OpenAPIDateWithoutTime",
                        "Character",
                        "UUID",
                        "URL",
                        "AnyObject",
                        "Any",
                        "Decimal",
                        "AnyCodable") // from AnyCodable dependency
        );
        defaultIncludes = new HashSet<>(
                Arrays.asList(
                        "Data",
                        "Date",
                        "URL", // for file
                        "UUID",
                        "Array",
                        "Dictionary",
                        "Set",
                        "Any",
                        "Empty",
                        "AnyObject",
                        "Any",
                        "Decimal")
        );

        objcReservedWords = new HashSet<>(
                Arrays.asList(
                        // Added for Objective-C compatibility
                        "id", "description", "NSArray", "NSURL", "CGFloat", "NSSet", "NSString", "NSInteger", "NSUInteger",
                        "NSError", "NSDictionary",
                        // 'Property 'hash' with type 'String' cannot override a property with type 'Int' (when objcCompatible=true)
                        "hash",
                        // Cannot override with a stored property 'className'
                        "className"
                )
        );

        reservedWords = new HashSet<>(
                Arrays.asList(
                        // name used by swift client
                        "ErrorResponse", "Response",

                        // Swift keywords. This list is taken from here:
                        // https://developer.apple.com/library/content/documentation/Swift/Conceptual/Swift_Programming_Language/LexicalStructure.html#//apple_ref/doc/uid/TP40014097-CH30-ID410
                        //
                        // Keywords used in declarations
                        "associatedtype", "class", "deinit", "enum", "extension", "fileprivate", "func", "import", "init",
                        "inout", "internal", "let", "open", "operator", "private", "protocol", "public", "static", "struct",
                        "subscript", "typealias", "var",
                        // Keywords uses in statements
                        "break", "case", "continue", "default", "defer", "do", "else", "fallthrough", "for", "guard", "if",
                        "in", "repeat", "return", "switch", "where", "while",
                        // Keywords used in expressions and types
                        "as", "Any", "catch", "false", "is", "nil", "rethrows", "super", "self", "Self", "throw", "throws", "true", "try",
                        // Keywords used in patterns
                        "_",
                        // Keywords that begin with a number sign
                        "#available", "#colorLiteral", "#column", "#else", "#elseif", "#endif", "#file", "#fileLiteral", "#function", "#if",
                        "#imageLiteral", "#line", "#selector", "#sourceLocation",
                        // Keywords reserved in particular contexts
                        "associativity", "convenience", "dynamic", "didSet", "final", "get", "infix", "indirect", "lazy", "left",
                        "mutating", "none", "nonmutating", "optional", "override", "postfix", "precedence", "prefix", "Protocol",
                        "required", "right", "set", "Type", "unowned", "weak", "willSet",

                        //
                        // Swift Standard Library types
                        // https://developer.apple.com/documentation/swift
                        //
                        // Numbers and Basic Values
                        "Bool", "Int", "Double", "Float", "Range", "ClosedRange", "Error", "Optional",
                        // Special-Use Numeric Types
                        "UInt", "UInt8", "UInt16", "UInt32", "UInt64", "Int8", "Int16", "Int32", "Int64", "Float80", "Float32", "Float64",
                        // Strings and Text
                        "String", "Character", "Unicode", "StaticString",
                        // Collections
                        "Array", "Dictionary", "Set", "OptionSet", "CountableRange", "CountableClosedRange",

                        // The following are commonly-used Foundation types
                        "URL", "Data", "Codable", "Encodable", "Decodable",

                        // The following are other words we want to reserve
                        "Void", "AnyObject", "Class", "dynamicType", "COLUMN", "FILE", "FUNCTION", "LINE"
                )
        );

        typeMapping = new HashMap<>();
        typeMapping.put("array", "Array");
        typeMapping.put("map", "Dictionary");
        typeMapping.put("set", "Set");
        typeMapping.put("Date", "Date");
        typeMapping.put("DateTime", "Date");
        typeMapping.put("boolean", "Bool");
        typeMapping.put("string", "String");
        typeMapping.put("char", "Character");
        typeMapping.put("short", "Int");
        typeMapping.put("int", "Int");
        typeMapping.put("long", "Int64");
        typeMapping.put("integer", "Int");
        typeMapping.put("Integer", "Int");
        typeMapping.put("float", "Float");
        typeMapping.put("number", "Double");
        typeMapping.put("double", "Double");
        typeMapping.put("file", "URL");
        typeMapping.put("binary", "URL");
        typeMapping.put("ByteArray", "Data");
        typeMapping.put("UUID", "UUID");
        typeMapping.put("URI", "String");
        typeMapping.put("decimal", "Decimal");
        typeMapping.put("object", "AnyCodable");
        typeMapping.put("AnyType", "AnyCodable");

        importMapping = new HashMap<>();

        cliOptions.add(new CliOption(PROJECT_NAME, "Project name in Xcode"));
        cliOptions.add(new CliOption(RESPONSE_AS,
                "Optionally use libraries to manage response.  Currently "
                        + StringUtils.join(RESPONSE_LIBRARIES, ", ")
                        + " are available."));
        cliOptions.add(new CliOption(CodegenConstants.NON_PUBLIC_API,
                CodegenConstants.NON_PUBLIC_API_DESC
                        + "(default: false)"));
        cliOptions.add(new CliOption(OBJC_COMPATIBLE,
                "Add additional properties and methods for Objective-C "
                        + "compatibility (default: false)"));
        cliOptions.add(new CliOption(POD_SOURCE, "Source information used for Podspec"));
        cliOptions.add(new CliOption(CodegenConstants.POD_VERSION, "Version used for Podspec"));
        cliOptions.add(new CliOption(POD_AUTHORS, "Authors used for Podspec"));
        cliOptions.add(new CliOption(POD_SOCIAL_MEDIA_URL, "Social Media URL used for Podspec"));
        cliOptions.add(new CliOption(POD_LICENSE, "License used for Podspec"));
        cliOptions.add(new CliOption(POD_HOMEPAGE, "Homepage used for Podspec"));
        cliOptions.add(new CliOption(POD_SUMMARY, "Summary used for Podspec"));
        cliOptions.add(new CliOption(POD_DESCRIPTION, "Description used for Podspec"));
        cliOptions.add(new CliOption(POD_SCREENSHOTS, "Screenshots used for Podspec"));
        cliOptions.add(new CliOption(POD_DOCUMENTATION_URL,
                "Documentation URL used for Podspec"));
        cliOptions.add(new CliOption(READONLY_PROPERTIES, "Make properties "
                + "readonly (default: false)"));
        cliOptions.add(new CliOption(REMOVE_MIGRATION_PROJECT_NAME_CLASS, "Make properties "
                + "removeMigrationProjectNameClass (default: false)"));
        cliOptions.add(new CliOption(SWIFT_USE_API_NAMESPACE,
                "Flag to make all the API classes inner-class "
                        + "of {{projectName}}API"));
        cliOptions.add(new CliOption(CodegenConstants.HIDE_GENERATION_TIMESTAMP,
                CodegenConstants.HIDE_GENERATION_TIMESTAMP_DESC)
                .defaultValue(Boolean.TRUE.toString()));
        cliOptions.add(new CliOption(LENIENT_TYPE_CAST,
                "Accept and cast values for simple types (string->bool, "
                        + "string->int, int->string)")
                .defaultValue(Boolean.FALSE.toString()));
        cliOptions.add(new CliOption(USE_BACKTICK_ESCAPES,
                "Escape reserved words using backticks (default: false)")
                .defaultValue(Boolean.FALSE.toString()));
        cliOptions.add(new CliOption(GENERATE_MODEL_ADDITIONAL_PROPERTIES,
                "Generate model additional properties (default: true)")
                .defaultValue(Boolean.TRUE.toString()));

        cliOptions.add(new CliOption(CodegenConstants.API_NAME_PREFIX, CodegenConstants.API_NAME_PREFIX_DESC));
        cliOptions.add(new CliOption(USE_SPM_FILE_STRUCTURE, "Use SPM file structure"
                + " and set the source path to Sources" + File.separator + "{{projectName}} (default: false)."));
        cliOptions.add(new CliOption(SWIFT_PACKAGE_PATH, "Set a custom source path instead of "
                + projectName + File.separator + "Classes" + File.separator + "OpenAPIs" + "."));
        cliOptions.add(new CliOption(USE_CLASSES, "Use final classes for models instead of structs (default: false)")
                .defaultValue(Boolean.FALSE.toString()));

        cliOptions.add(new CliOption(HASHABLE_MODELS,
                "Make hashable models (default: true)")
                .defaultValue(Boolean.TRUE.toString()));

        cliOptions.add(new CliOption(USE_JSON_ENCODABLE,
                "Make models conform to JSONEncodable protocol (default: true)")
                .defaultValue(Boolean.TRUE.toString()));

        cliOptions.add(new CliOption(MAP_FILE_BINARY_TO_DATA,
                "[WARNING] This option will be removed and enabled by default in the future once we've enhanced the code to work with `Data` in all the different situations. Map File and Binary to Data (default: false)")
                .defaultValue(Boolean.FALSE.toString()));

        cliOptions.add(new CliOption(USE_CUSTOM_DATE_WITHOUT_TIME,
                "Uses a custom type to decode and encode dates without time information to support OpenAPIs date format (default: false)")
                .defaultValue(Boolean.FALSE.toString()));

        cliOptions.add(new CliOption(VALIDATABLE,
                "Make validation rules and validator for model properies (default: true)")
                .defaultValue(Boolean.TRUE.toString()));

        supportedLibraries.put(LIBRARY_URLSESSION, "[DEFAULT] HTTP client: URLSession");
        supportedLibraries.put(LIBRARY_ALAMOFIRE, "HTTP client: Alamofire");
        supportedLibraries.put(LIBRARY_VAPOR, "HTTP client: Vapor");
        supportedLibraries.put(LIBRARY_DBS, "HTTP client: DBSDataProvider");

        CliOption libraryOption = new CliOption(CodegenConstants.LIBRARY, "Library template (sub-template) to use");
        libraryOption.setEnum(supportedLibraries);
        libraryOption.setDefault(LIBRARY_URLSESSION);
        cliOptions.add(libraryOption);
        setLibrary(LIBRARY_DBS);
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    public String getName() {
        return "boat-swift5";
    }

    @Override
    public String getHelp() {
        return "Generates a BOAT Swift 5.x client library.";
    }


    @Override
    public void processOpts() {
        super.processOpts();

        if (StringUtils.isEmpty(System.getenv("SWIFT_POST_PROCESS_FILE"))) {
            LOGGER.info("Environment variable SWIFT_POST_PROCESS_FILE not defined so the Swift code may not be properly formatted. To define it, try 'export SWIFT_POST_PROCESS_FILE=/usr/local/bin/swiftformat' (Linux/Mac)");
            LOGGER.info("NOTE: To enable file post-processing, 'enablePostProcessFile' must be set to `true` (--enable-post-process-file for CLI).");
        }

        // Setup project name
        if (additionalProperties.containsKey(PROJECT_NAME)) {
            setProjectName((String) additionalProperties.get(PROJECT_NAME));
        } else {
            additionalProperties.put(PROJECT_NAME, projectName);
        }
        sourceFolder = projectName + File.separator + sourceFolder;

        // Setup nonPublicApi option, which generates code with reduced access
        // modifiers; allows embedding elsewhere without exposing non-public API calls
        // to consumers
        if (additionalProperties.containsKey(CodegenConstants.NON_PUBLIC_API)) {
            setNonPublicApi(convertPropertyToBooleanAndWriteBack(CodegenConstants.NON_PUBLIC_API));
        }
        additionalProperties.put(CodegenConstants.NON_PUBLIC_API, nonPublicApi);

        // Setup objcCompatible option, which adds additional properties
        // and methods for Objective-C compatibility
        if (additionalProperties.containsKey(OBJC_COMPATIBLE)) {
            setObjcCompatible(convertPropertyToBooleanAndWriteBack(OBJC_COMPATIBLE));
        }
        additionalProperties.put(OBJC_COMPATIBLE, objcCompatible);

        // add objc reserved words
        if (Boolean.TRUE.equals(objcCompatible)) {
            reservedWords.addAll(objcReservedWords);
        }

        if (additionalProperties.containsKey(RESPONSE_AS)) {
            Object responseAsObject = additionalProperties.get(RESPONSE_AS);
            if (responseAsObject instanceof String) {
                setResponseAs(((String) responseAsObject).split(","));
            } else {
                setResponseAs((String[]) responseAsObject);
            }
        }
        additionalProperties.put(RESPONSE_AS, responseAs);
        if (ArrayUtils.contains(responseAs, RESPONSE_LIBRARY_PROMISE_KIT)) {
            additionalProperties.put("usePromiseKit", true);
        }
        if (ArrayUtils.contains(responseAs, RESPONSE_LIBRARY_RX_SWIFT)) {
            additionalProperties.put("useRxSwift", true);
        }
        if (ArrayUtils.contains(responseAs, RESPONSE_LIBRARY_RESULT)) {
            additionalProperties.put("useResult", true);
        }
        if (ArrayUtils.contains(responseAs, RESPONSE_LIBRARY_COMBINE)) {
            additionalProperties.put("useCombine", true);
        }
        if (ArrayUtils.contains(responseAs, RESPONSE_LIBRARY_ASYNC_AWAIT)) {
            additionalProperties.put("useAsyncAwait", true);
        }

        // Setup readonlyProperties option, which declares properties so they can only
        // be set at initialization
        if (additionalProperties.containsKey(READONLY_PROPERTIES)) {
            setReadonlyProperties(convertPropertyToBooleanAndWriteBack(READONLY_PROPERTIES));
        }
        additionalProperties.put(READONLY_PROPERTIES, readonlyProperties);

        // Setup removeMigrationProjectNameClass option, which keeps or remove the projectName class
        if (additionalProperties.containsKey(REMOVE_MIGRATION_PROJECT_NAME_CLASS)) {
            setRemoveMigrationProjectNameClass(convertPropertyToBooleanAndWriteBack(REMOVE_MIGRATION_PROJECT_NAME_CLASS));
        }
        additionalProperties.put(REMOVE_MIGRATION_PROJECT_NAME_CLASS, removeMigrationProjectNameClass);

        // Setup swiftUseApiNamespace option, which makes all the API
        // classes inner-class of {{projectName}}API
        if (additionalProperties.containsKey(SWIFT_USE_API_NAMESPACE)) {
            setSwiftUseApiNamespace(convertPropertyToBooleanAndWriteBack(SWIFT_USE_API_NAMESPACE));
        }

        if (!additionalProperties.containsKey(POD_AUTHORS)) {
            additionalProperties.put(POD_AUTHORS, DEFAULT_POD_AUTHORS);
        }

        if (additionalProperties.containsKey(USE_SPM_FILE_STRUCTURE)) {
            setUseSPMFileStructure(convertPropertyToBooleanAndWriteBack(USE_SPM_FILE_STRUCTURE));
            sourceFolder = "Sources" + File.separator + projectName;
        }

        if (additionalProperties.containsKey(SWIFT_PACKAGE_PATH) && ((String)additionalProperties.get(SWIFT_PACKAGE_PATH)).length() > 0) {
            setSwiftPackagePath((String)additionalProperties.get(SWIFT_PACKAGE_PATH));
            sourceFolder = swiftPackagePath;
        }

        if (additionalProperties.containsKey(USE_BACKTICK_ESCAPES)) {
            setUseBacktickEscapes(convertPropertyToBooleanAndWriteBack(USE_BACKTICK_ESCAPES));
        }

        if (additionalProperties.containsKey(GENERATE_MODEL_ADDITIONAL_PROPERTIES)) {
            setGenerateModelAdditionalProperties(convertPropertyToBooleanAndWriteBack(GENERATE_MODEL_ADDITIONAL_PROPERTIES));
        }
        additionalProperties.put(GENERATE_MODEL_ADDITIONAL_PROPERTIES, generateModelAdditionalProperties);

        if (additionalProperties.containsKey(HASHABLE_MODELS)) {
            setHashableModels(convertPropertyToBooleanAndWriteBack(HASHABLE_MODELS));
        }
        additionalProperties.put(HASHABLE_MODELS, hashableModels);

        if (additionalProperties.containsKey(USE_JSON_ENCODABLE)) {
            setUseJsonEncodable(convertPropertyToBooleanAndWriteBack(USE_JSON_ENCODABLE));
        }
        additionalProperties.put(USE_JSON_ENCODABLE, useJsonEncodable);

        if (additionalProperties.containsKey(MAP_FILE_BINARY_TO_DATA)) {
            setMapFileBinaryToData(convertPropertyToBooleanAndWriteBack(MAP_FILE_BINARY_TO_DATA));
        }
        additionalProperties.put(MAP_FILE_BINARY_TO_DATA, mapFileBinaryToData);
        if (mapFileBinaryToData) {
            typeMapping.put("file", "Data");
            typeMapping.put("binary", "Data");
        }

        if (additionalProperties.containsKey(USE_CUSTOM_DATE_WITHOUT_TIME)) {
            setUseCustomDateWithoutTime(convertPropertyToBooleanAndWriteBack(USE_CUSTOM_DATE_WITHOUT_TIME));
        }
        additionalProperties.put(USE_CUSTOM_DATE_WITHOUT_TIME, useCustomDateWithoutTime);
        if (useCustomDateWithoutTime) {
            typeMapping.put("date", "OpenAPIDateWithoutTime");
        } else {
            typeMapping.put("date", "Date");
        }

        if (additionalProperties.containsKey(USE_CLASSES)) {
            setUseClasses(convertPropertyToBooleanAndWriteBack(USE_CLASSES));
        }
        additionalProperties.put(USE_CLASSES, useClasses);

        if (additionalProperties.containsKey(VALIDATABLE)) {
            setValidatable(convertPropertyToBooleanAndWriteBack(VALIDATABLE));
        }
        additionalProperties.put(VALIDATABLE, validatable);

        setLenientTypeCast(convertPropertyToBooleanAndWriteBack(LENIENT_TYPE_CAST));

        // make api and model doc path available in mustache template
        additionalProperties.put("apiDocPath", apiDocPath);
        additionalProperties.put("modelDocPath", modelDocPath);

        supportingFiles.add(new SupportingFile("Podspec.mustache",
                "",
                projectName + ".podspec"));
        if (additionalProperties.containsKey(DEPENDENCY_MANAGEMENT)) {
            Object dependenciesAsObject = additionalProperties.get(DEPENDENCY_MANAGEMENT);
            if (dependenciesAsObject instanceof String) {
                setDependenciesAs(((String) dependenciesAsObject).split(","));
            } else {
                setDependenciesAs((String[]) dependenciesAsObject);
            }
        }
        additionalProperties.put(DEPENDENCY_MANAGEMENT, dependenciesAs);
        if (ArrayUtils.contains(dependenciesAs, DEPENDENCY_MANAGEMENT_PODFILE)) {
            supportingFiles.add(new SupportingFile("Podfile.mustache",
                    "",
                    "Podfile"));
        }
        if (ArrayUtils.contains(dependenciesAs, DEPENDENCY_MANAGEMENT_CARTFILE)) {
            supportingFiles.add(new SupportingFile("Cartfile.mustache",
                    "",
                    "Cartfile"));
        }
        supportingFiles.add(new SupportingFile("Package.swift.mustache",
                "",
                "Package.swift"));
        supportingFiles.add(new SupportingFile("APIHelper.mustache",
                sourceFolder,
                "APIHelper.swift"));
        supportingFiles.add(new SupportingFile("Configuration.mustache",
                sourceFolder,
                "Configuration.swift"));
        supportingFiles.add(new SupportingFile("Extensions.mustache",
                sourceFolder,
                "Extensions.swift"));
        supportingFiles.add(new SupportingFile("Models.mustache",
                sourceFolder,
                "Models.swift"));
        supportingFiles.add(new SupportingFile("CodableHelper.mustache",
                sourceFolder,
                "CodableHelper.swift"));
        supportingFiles.add(new SupportingFile("OpenISO8601DateFormatter.mustache",
                sourceFolder,
                "OpenISO8601DateFormatter.swift"));
        supportingFiles.add(new SupportingFile("SynchronizedDictionary.mustache",
                sourceFolder,
                "SynchronizedDictionary.swift"));
        supportingFiles.add(new SupportingFile("gitignore.mustache",
                "",
                ".gitignore"));
        supportingFiles.add(new SupportingFile("README.mustache",
                "",
                "README.md"));
        supportingFiles.add(new SupportingFile("XcodeGen.mustache",
                "",
                "project.yml"));
        supportingFiles.add(new SupportingFile("AnyCodable.swift.mustache",
                sourceFolder,
                "AnyCodable.swift"));

        if (!getLibrary().equals(LIBRARY_VAPOR)) {
            supportingFiles.add(new SupportingFile("Podspec.mustache",
                    "",
                    projectName + ".podspec"));
            supportingFiles.add(new SupportingFile("Cartfile.mustache",
                    "",
                    "Cartfile"));
            supportingFiles.add(new SupportingFile("CodableHelper.mustache",
                    sourceFolder,
                    "CodableHelper.swift"));
            supportingFiles.add(new SupportingFile("JSONDataEncoding.mustache",
                    sourceFolder,
                    "JSONDataEncoding.swift"));
            supportingFiles.add(new SupportingFile("JSONEncodingHelper.mustache",
                    sourceFolder,
                    "JSONEncodingHelper.swift"));
            supportingFiles.add(new SupportingFile("git_push.sh.mustache",
                    "",
                    "git_push.sh"));
            supportingFiles.add(new SupportingFile("SynchronizedDictionary.mustache",
                    sourceFolder,
                    "SynchronizedDictionary.swift"));
            supportingFiles.add(new SupportingFile("XcodeGen.mustache",
                    "",
                    "project.yml"));
            supportingFiles.add(new SupportingFile("APIHelper.mustache",
                    sourceFolder,
                    "APIHelper.swift"));
            supportingFiles.add(new SupportingFile("Models.mustache",
                    sourceFolder,
                    "Models.swift"));
        }
        supportingFiles.add(new SupportingFile("Package.swift.mustache",
                "",
                "Package.swift"));
        supportingFiles.add(new SupportingFile("Configuration.mustache",
                sourceFolder,
                "Configuration.swift"));
        supportingFiles.add(new SupportingFile("Extensions.mustache",
                sourceFolder,
                "Extensions.swift"));
        supportingFiles.add(new SupportingFile("OpenISO8601DateFormatter.mustache",
                sourceFolder,
                "OpenISO8601DateFormatter.swift"));
        if (useCustomDateWithoutTime) {
            supportingFiles.add(new SupportingFile("OpenAPIDateWithoutTime.mustache",
                    sourceFolder,
                    "OpenAPIDateWithoutTime.swift"));
        }
        supportingFiles.add(new SupportingFile("APIs.mustache",
                sourceFolder,
                "APIs.swift"));
        if (validatable) {
            supportingFiles.add(new SupportingFile("Validation.mustache",
                    sourceFolder,
                    "Validation.swift"));
        }
        supportingFiles.add(new SupportingFile("gitignore.mustache",
                "",
                ".gitignore"));
        supportingFiles.add(new SupportingFile("README.mustache",
                "",
                "README.md"));
        supportingFiles.add(new SupportingFile("swiftformat.mustache",
                "",
                ".swiftformat"));

        switch (getLibrary()) {
            case LIBRARY_ALAMOFIRE:
                additionalProperties.put("useAlamofire", true);
                supportingFiles.add(new SupportingFile("AlamofireImplementations.mustache",
                        sourceFolder,
                        "AlamofireImplementations.swift"));
                break;
            case LIBRARY_URLSESSION:
                additionalProperties.put("useURLSession", true);
                supportingFiles.add(new SupportingFile("URLSessionImplementations.mustache",
                        sourceFolder,
                        "URLSessionImplementations.swift"));
                break;
            case LIBRARY_VAPOR:
                additionalProperties.put("useVapor", true);
                break;
            case LIBRARY_DBS:
                additionalProperties.put("useDBSDataProvider", true);
            default:
                break;
        }

    }

    public void setDependenciesAs(String[] dependenciesAs) {
        this.dependenciesAs = dependenciesAs;
    }

    @Override
    public void postProcess() {
        System.out.println("################################################################################");
        System.out.println("# Thanks for using BOAT Swift Generator.                                          #");
        System.out.println("################################################################################");
    }

}
