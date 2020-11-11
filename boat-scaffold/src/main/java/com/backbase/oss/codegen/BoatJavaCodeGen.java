package com.backbase.oss.codegen;

import org.openapitools.codegen.languages.JavaClientCodegen;

public class BoatJavaCodeGen extends JavaClientCodegen {
    public static final String NAME = "boat-java";

    public BoatJavaCodeGen() {
        this.embeddedTemplateDir = this.templateDir = NAME;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
