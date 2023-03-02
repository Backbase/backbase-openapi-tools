package com.backbase.oss.codegen.javascript;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;

import java.io.File;
import java.net.URL;

public class MultipleTagsTests {

    @Test
    public void testSingleTags() throws OpenAPILoaderException {

        String specName = "petstore";
        generate("/oas-examples/" + specName + ".yaml", "target/javascript/" + specName);
    }

    @Test
    public void testMultipleTags() throws OpenAPILoaderException {

        String specName = "petstore-multiple-tags";

        generate("/oas-examples/" + specName + ".yaml", "target/javascript/" + specName);

    }

    @Test
    public void testCustomSpec() throws OpenAPILoaderException {
        String spec = System.getProperty("spec");
        String output = System.getProperty("output", "target/javascript/");
        if (spec != null) {
            generate(output, new File(spec));
        } else {
            generate(output, getFile("/psd2/psd2-api-1.3.5-20191216v1.yaml"));
        }
    }

    private void generate(String specName, String output) throws OpenAPILoaderException {
        File spec = getFile(specName);

        generate(output, spec);
    }

    private static void generate(String output, File spec) throws OpenAPILoaderException {
        OpenAPI openAPI = OpenAPILoader.load(spec);
        BoatJavascriptGenerator generator = new BoatJavascriptGenerator();
        generator.setSkipOverwrite(false);
        generator.setOutputDir(output);

        ClientOptInput input = new ClientOptInput();
        input.config(generator);
        input.openAPI(openAPI);

        new DefaultGenerator().opts(input).generate();
    }

    protected File getFile(String name) {
        URL resource = getClass().getResource(name);
        assert resource != null;
        return new File(resource.getFile());
    }
}
