package com.backbase.oss.boat.transformers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

@Slf4j
public class BundlerOld implements Transformer {

    private ObjectMapper mapper = new ObjectMapper();

    private Map<String, Map<String, Object>> references = new ConcurrentHashMap<>();
    private String folder = null;
    private Map<String, Object> definitions = null;

    private String dir;
    private String file;
    private String output = "yaml";
    private String outputFile = "openapi.bundled";
    private String outputDir;

    @Override
    public void transform(OpenAPI openAPI, Map<String, Object> options) {

        File inputFile = (File) options.get("input");
        File optionsOutputFile = (File) options.get("output");

        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(false);
        parseOptions.setResolve(false);

        OpenAPI oas = new OpenAPIV3Parser().read(inputFile.getAbsolutePath(), null, parseOptions);
        


        file = inputFile.getName();
        dir = inputFile.getParentFile().getAbsolutePath();
        outputDir = optionsOutputFile.getParentFile().getAbsolutePath();

        log.info("Bundling all references in new OpenAPI with base dir: {}", dir);

        // first mandatory argument is the folder where the YAML files to be bundled are to be found
        // second argument is optional; allows the setting of an input file name; openapi.yaml is the default
        if (dir != null) {
            folder = dir;
            // The input parameter is the folder that contains openapi.yaml and
            // this folder will be the base path to calculate remote references.
            // if the second argument is a different file name, it will be used
            // otherwise, default is "openapi.yaml"
            String fileName = file == null ? "openapi.yaml" : file;

            // if the operation is validate, validate the file, in YAML or JSON format, then exit the process
            validateSpecification(folder, fileName);

            // set output directory.
            // if not set, default it to the input <dir>
            if (outputDir == null) {
                outputDir = folder;
            }

            // bundle the file and validate the resulting file
            log.info("OpenAPI Bundler: Bundling API definition with file name {}, from directory {}", fileName, folder);

            Path path = Paths.get(folder, fileName);
            try (InputStream is = Files.newInputStream(path)) {
                String json = null;
                Yaml yaml = new Yaml();
                Map<String, Object> map = (Map<String, Object>) yaml.load(is);

                // we have to handle components as a separate map, otherwise, we will have
                // concurrent access exception while iterating the map and updating components.
                definitions = new HashMap<>();

                Map<String, Object> components = (Map<String, Object>) map.get("components");
                if ((components != null) && (components.get("schemas") != null)) {
                    definitions.putAll((Map<String, Object>) components.get("schemas"));
                }

                // now let's handle the references.
                resolveMap(map);
                // now the definitions might contains some references that are not in
                // definitions.
                Map<String, Object> def = new HashMap<>(definitions);
                log.info("Start resolving components for the first time ...");
                resolveMap(def);

                def = new HashMap<>(definitions);
                log.info("Start resolving components for the second time ...");
                resolveMap(def);

                def = new HashMap<>(definitions);
                log.info("Start resolving components for the third time ...");
                resolveMap(def);

                // add the resolved components to the main map, before persisting
                Map<String, Object> schemasMap = null;
                Map<String, Object> componentsMap = (Map<String, Object>) map.get("components");
                if ((componentsMap != null) && (componentsMap.get("schemas") != null)) {
                    schemasMap = (Map<String, Object>) componentsMap.get("schemas");
                } else {
                    if (componentsMap == null) {
                        componentsMap = new HashMap<>();
                        map.put("components", componentsMap);
                        schemasMap = new HashMap<>();
                        componentsMap.put("schemas", schemasMap);
                    } else if (componentsMap.get("schemas") == null) {
                        schemasMap = new HashMap<>();
                        componentsMap.put("schemas", schemasMap);
                    }
                }

                schemasMap.putAll(definitions);

                // Convert the map back to JSON and serialize it.
                if (output.equalsIgnoreCase("json") || output.equalsIgnoreCase("both")) {
                    log.info("OpenAPI Bundler: write bundled JSON file to {} ... in directory {}",
                        outputFile, outputDir);
                    json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);

                    // write the output to openapi.json
                    Files.write(Paths.get(outputDir, String.format("%s.%s", outputFile, "json")), json.getBytes());

                    // validate the output file
                    validateSpecification(outputDir, String.format("%s.%s", outputFile, "json"));
                }

                // Convert the map back to YAML and serialize it.
                if (output.equalsIgnoreCase("yaml") || output.equalsIgnoreCase("both")) {
                    log.info("OpenAPI Bundler: write bundled YAML file to {} ... in directory {}", outputFile,
                        outputDir);
                    YAMLFactory yamlFactory = new YAMLFactory();
                    yamlFactory.enable(Feature.MINIMIZE_QUOTES);
                    yamlFactory.disable(Feature.SPLIT_LINES);
                    yamlFactory.disable(Feature.WRITE_DOC_START_MARKER);
                    yamlFactory.disable(Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS);
                    yamlFactory.disable(Feature.LITERAL_BLOCK_STYLE);

                    ObjectMapper objMapper = new ObjectMapper(yamlFactory);
                    String yamlOutput = objMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
                    Files
                        .write(Paths.get(outputDir, String.format("%s.%s", outputFile, "yaml")), yamlOutput.getBytes());

                    // validate the output file
                    validateSpecification(outputDir, String.format("%s.%s", outputFile, "yaml"));
                }
            } catch (Exception e) {
                log.error("Failed to do something: ", e);
            }
        } else {
            log.error("OpenAPI Bundler: ERROR: You must pass in a folder to a yaml file!");
        }

        // completed bundling
        output = output.equalsIgnoreCase("both") ? "YAML & JSON" : output.toUpperCase();
        log.info(
            "OpenAPI Bundler: Bundling API definition has completed. Output directory {}, in file format {}", dir,
            output);
    }

    private void validateSpecification(String dir, String fileName) {
//        try {
//            @SuppressWarnings("unused")
//            OpenApi3 model = (OpenApi3) new OpenApiParser().parse(new File(dir + "/" + fileName), true);
//            log.info("OpenAPI3 Validation: Definition file {} in directory {}} is valid ....", fileName, dir);
//        } catch (Exception e) {
//            log.error("OpenAPI3 Validation: Definition file {} in directory {} failed with exception {}", fileName, dir,
//                e);
//        }
    }

    private Map<String, Object> handlerPointer(String key, String pointer) {
        Map<String, Object> result = new HashMap<>();
        if (pointer.startsWith("#")) {
            // There are two cases with local reference. 1, original in
            // local reference and it has path of "definitions" or 2, local reference
            // that extracted from reference file with reference to an object directly.
            String refKey = pointer.substring(pointer.lastIndexOf("/") + 1);

            log.info("key: {}, pointer: {}", key, refKey);

            if (pointer.contains("components")) {
                // if the $ref is an object, keep it that way and if $ref is not an object, make
                // it inline
                // and remove it from definitions.
                Map<String, Object> refMap = (Map<String, Object>) definitions.get(refKey);
                if (refMap == null) {
                    log.info("ERROR: Could not find reference in definitions for key {}", refKey);
                    System.exit(0);
                }
                if (isRefMapObject(refMap)) {
                    result.put("$ref", pointer);
                } else {
                    result = refMap;
                }
            } else {
                // This is something extracted from extenal file and the reference is still
                // local.
                // need to look up for all reference files in order to find it.
                Map<String, Object> refMap = null;
                for (Map<String, Object> r : references.values()) {
                    refMap = (Map<String, Object>) r.get(refKey);
                    if (refMap != null) {
                        break;
                    }
                }

                if (refMap == null) {
                    log.error(
                        "ERROR: Could not resolve reference locally in components for key  {}. Please check your components section.",
                        refKey);
                    System.exit(0);
                }
                if (isRefMapObject(refMap)) {
                    definitions.put(refKey, refMap);
                    result.put("$ref", "#/components/schemas/" + refKey);
                } else {
                    result = refMap;
                }
            }
        } else if (pointer.indexOf("#") != -1) {
            log.info("Handling pointer: {}", pointer);
            // external reference and it must be a relative url
            Map<String, Object> refs = loadRef(pointer.substring(0, pointer.indexOf("#")));
            String refKey = pointer.substring(pointer.indexOf("#/") + 2);
            Map<String, Object> refMap = (Map<String, Object>) refs.get(refKey);
            // now need to resolve the internal references in refMap.
            if (refMap == null) {
                log.error("ERROR: Could not find reference in external file for pointer {}", pointer);
                System.exit(0);
            }
            // check if the refMap type is object or not.
            if (isRefMapObject(refMap)) {
                // add to definitions
                definitions.put(refKey, refMap);
                // update the ref pointer to local
                result.put("$ref", "#/components/schemas/" + refKey);
            } else {
                // simple type, inline refMap instead.
                resolveMap(refMap);
                result = refMap;
            }
        } else {


            Path p = Paths.get(folder, pointer);
            try {

                String s =new String(Files.readAllBytes(p), Charset.defaultCharset());
                return Collections.singletonMap(key, s);


            } catch (IOException e) {
                e.printStackTrace();
            }



        }
        return result;
    }

    /**
     * Check if the input map is an json object or not.
     *
     * @param refMap input map
     * @return
     */
    private boolean isRefMapObject(Map<String, Object> refMap) {
        boolean result = false;
        for (Map.Entry<String, Object> entry : refMap.entrySet()) {
            if ("type".equals(String.valueOf(entry.getKey())) && "object".equals(String.valueOf(entry.getValue()))) {
                result = true;
            }
        }
        return result;
    }

    /**
     * load and cache remote reference. folder is a static variable assigned by argv[0] it will check the cache first
     * and only load it if it doesn't exist in cache.
     *
     * @param path the path of remote file
     * @return map of remote references
     */
    private Map<String, Object> loadRef(String path) {
        Map<String, Object> result = references.get(path);
        if (result == null) {
            Path p = Paths.get(folder, path);
            try (InputStream is = Files.newInputStream(p)) {
                Yaml yaml = new Yaml();
                result = (Map<String, Object>) yaml.load(is);
                references.put(path, result);
            } catch (Exception e) {
                log.error("Failed to load ref: {}", path, e);
            }
        }
        return result;
    }

    /**
     * It deep iterate a map object and looking for "$ref" and handle it.
     *
     * @param map the map of openapi.yaml
     */
    private void resolveMap(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            log.debug("resolveMap key = {} value =  {}", key, value);
            if (value instanceof Map) {
                // check if this map is $ref, it should be size = 1
                if (((Map) value).size() == 1) {
                    Set keys = ((Map) value).keySet();
                    for (Object o : keys) {
                        String k = (String) o;
                        if ("$ref".equals(k)) {
                            String pointer = (String) ((Map) value).get(k);
                            log.debug("pointer = {}", pointer);
                            Map refMap = handlerPointer(key, pointer);
                            entry.setValue(refMap);
                        }
                    }
                }
                resolveMap((Map) value);
            } else if (value instanceof List) {
                resolveList(key, (List) value);
            } else {
                continue;
            }
        }
    }

    private void resolveList(String key, List list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof Map) {
                // check if this map is $ref
                if (((Map) list.get(i)).size() == 1) {
                    Set keys = ((Map) list.get(i)).keySet();
                    for (Iterator j = keys.iterator(); j.hasNext(); ) {
                        String k = (String) j.next();
                        if ("$ref".equals(k)) {
                            String pointer = (String) ((Map) list.get(i)).get(k);
                            list.set(i, handlerPointer(key, pointer));
                        }
                    }
                }
                resolveMap((Map<String, Object>) list.get(i));
            } else if (list.get(i) instanceof List) {
                resolveList(key, (List) list.get(i));
            } else {
                continue;
            }
        }
    }

}
