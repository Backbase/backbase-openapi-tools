package com.backbase.oss.boat;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

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

        assertThrows(MojoFailureException.class, mojo::execute);


    }



}

