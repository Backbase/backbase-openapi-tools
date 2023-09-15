package com.backbase.oss.codegen.java;

import jakarta.validation.Valid;

/**
 * This pojo is referred in BoatSpringCodeGenTest.shouldGenerateValidations.
 *
 * When API refers to existing class, @Valid annotation ends up on wrong location.
 *
 * When testing generated code can be compiled, this class should be either on the classpath, or it has to be in
 * one of the source locations.
 */
@Valid
public class ValidatedPojo {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
