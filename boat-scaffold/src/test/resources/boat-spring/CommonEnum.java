package com.backbase.oss.codegen.java;

import jakarta.validation.Valid;

/**
 * This enum is referred in BoatSpringCodeGenTest.shouldGenerateValidations.
 *
 * When API refers to existing class, @Valid annotation ends up on wrong location.
 *
 * When testing generated code can be compiled, this class should be either on the classpath, or it has to be in
 * one of the source locations.
 */
public enum CommonEnum {

    YES,
    NO,
    INVALID
}
