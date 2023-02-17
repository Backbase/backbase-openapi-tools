package com.backbase.oss.boat;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class ValidateMojoTests {

    @Test
    void testValidation() throws MojoFailureException {

        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/petstore.yaml").getFile());

        File input = new File(spec);

        ValidateMojo mojo = new ValidateMojo();
        mojo.setInput(input);
        mojo.setFailOnWarning(true);

        assertDoesNotThrow(mojo::execute);

    }

    @Test
    void testValidatingDirectory() throws MojoFailureException {
        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/").getFile());

        File input = new File(spec);

        ValidateMojo mojo = new ValidateMojo();
        mojo.setInput(input);
        mojo.setFailOnWarning(true);


        assertThrows(MojoFailureException.class, mojo::execute);


    }


}

