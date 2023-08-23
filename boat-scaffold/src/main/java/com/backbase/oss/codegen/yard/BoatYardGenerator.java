package com.backbase.oss.codegen.yard;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.codegen.AbstractDocumentationGenerator;
import com.backbase.oss.codegen.CodegenException;
import com.backbase.oss.codegen.doc.BoatDocsGenerator;
import com.backbase.oss.codegen.yard.model.Portal;
import com.backbase.oss.codegen.yard.model.Spec;
import com.backbase.oss.codegen.yard.model.YardModel;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.Generator;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


@Slf4j
public class BoatYardGenerator extends AbstractDocumentationGenerator {

    public BoatYardGenerator(BoatYardConfig config) {
        this.opts(new ClientOptInput().config(config));
    }

    private BoatYardConfig getBoatYardConfig() {
        return (BoatYardConfig) config;
    }

    public List<File> generate() {
        return getYardModel().getPortals().stream()
            .flatMap(portal -> generate(portal).stream())
            .collect(Collectors.toList());
    }

    @SneakyThrows
    private List<File> generate(Portal portal) {
        log.info("Generating BOAT Yard for portal: {}", portal.getTitle());

        portal.getCapabilities().forEach(
            capability -> {
                String capabilityKey = capability.getKey();
                capability.getServices().forEach(service -> {
                    String serviceKey = service.getKey();

                    service.getSpecs().forEach(spec -> {

                        String boatDocOutput = capabilityKey + "/" + serviceKey + "/" + spec.getKey();
                        String boatDocUrl = boatDocOutput + "/index.html";

                        spec.setBoatDocUrl(boatDocUrl);

                        generateBoatDoc(spec, boatDocOutput, getBoatYardConfig());
                        if (portal.getDefaultSpecUrl() == null) {
                            portal.setDefaultSpecUrl(boatDocUrl);
                        }


                        spec.setBoatDocUrl(boatDocUrl);

                    });
                });
            }
        );
        // After processing our model, convert it into a map
        Map<String, Object> bundle = convertToBundle(portal);
        List<File> files = processTemplates(bundle);
        log.info("Finished creating BOAT Yard for portal: {}", portal.getTitle());

        return files;
    }

    @SneakyThrows
    private void generateBoatDoc(Spec spec, String boatDocOutput, BoatYardConfig config) {
        log.info("Generating Boat Doc for spec: {} in: {}", spec.getTitle(), boatDocOutput);
        BoatDocsGenerator codegenConfig = new BoatDocsGenerator();

        codegenConfig.setOutputDir(new File(config.getOutputDir(), boatDocOutput).toString());
        codegenConfig.setSkipOverwrite(false);
        codegenConfig.setInputSpec(spec.getOpenApiUrl());

        File specBaseDir = config.getSpecsBaseDir();
        if (specBaseDir == null) {
            specBaseDir = new File(config.getInputSpec()).getParentFile();
        }

        File file = new File(specBaseDir, spec.getOpenApiUrl());
        log.info("Generating OpenAPI Doc for: {}", file);
        OpenAPI openAPI = OpenAPILoader.load(file);

        ClientOptInput input = new ClientOptInput();
        input.config(codegenConfig);
        input.openAPI(openAPI);
        new DefaultGenerator().opts(input).generate();
    }

    private YardModel getYardModel() {
        YardModel yardModel;

        Constructor constructor = new Constructor(YardModel.class, new LoaderOptions());

        Yaml yaml = new Yaml(constructor);
        try {
            yardModel = yaml.loadAs(new FileInputStream(input), YardModel.class);
        } catch (IOException e) {
            throw new CodegenException("Cannot create BOAT Yard from input: " + input, e);
        }
        return yardModel;
    }


}
