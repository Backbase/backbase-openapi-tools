package com.backbase.oss.codegen;

import org.openapitools.codegen.languages.JavaClientCodegen;

public class JavaClientBoatCodeGen extends JavaClientCodegen {
    public static final String NAME = "java-boat";

    public JavaClientBoatCodeGen() {
        this.embeddedTemplateDir = this.templateDir = NAME;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
