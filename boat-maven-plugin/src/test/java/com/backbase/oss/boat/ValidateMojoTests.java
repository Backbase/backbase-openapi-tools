package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ValidateMojoTests {

    @Test
    public void testValidation() throws MojoFailureException {

        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/petstore.yaml").getFile());

        File input = new File(spec);

        ValidateMojo mojo = new ValidateMojo();
        mojo.setInput(input);
        mojo.setFailOnWarning(true);

        mojo.execute();

    }

    @Test
    public void testValidationCatches() throws MojoFailureException, MojoExecutionException{


        ValidateMojo mojo = new ValidateMojo();
        mojo.setInput(new File("bad.yaml"));
        mojo.setFailOnWarning(true);

        Assert.assertThrows(MojoFailureException.class, ()-> mojo.execute());


    }



}

