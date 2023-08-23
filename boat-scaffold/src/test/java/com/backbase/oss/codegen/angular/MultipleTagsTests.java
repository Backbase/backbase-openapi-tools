package com.backbase.oss.codegen.angular;

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

        generate("/oas-examples/" + specName + ".yaml", "target/angular/" + specName);
    }

    @Test
    public void testMultipleTags() throws OpenAPILoaderException {

        String specName = "petstore-multiple-tags";

        generate("/oas-examples/" + specName + ".yaml", "target/angular/" + specName);

    }

    private void generate(String specName, String output) throws OpenAPILoaderException {
        File spec = getFile(specName);

        OpenAPI openAPI = OpenAPILoader.load(spec);
        BoatAngularGenerator generator = new BoatAngularGenerator();
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
