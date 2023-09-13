package org.openapitools.codegen.languages;

import org.openapitools.codegen.SupportingFile;

public class BoatAndroidClientCodegen extends KotlinClientCodegen {

    public static final String NAME = "boat-android";
    public static final String DBS_DATA_PROVIDER = "DBSDataProvider";

    public BoatAndroidClientCodegen() {
        super();
        supportedLibraries.put(DBS_DATA_PROVIDER, "Backbase: client");
        library = DBS_DATA_PROVIDER;
        embeddedTemplateDir = NAME;
        templateDir = NAME;
        propertyAdditionalKeywords.remove("size");
        specialCharReplacements.put("-", "");
        supportingFiles.add(new SupportingFile("manifest.mustache", "", "src/main/AndroidManifest.xml"));
        supportingFiles.add(new SupportingFile("gradle.properties.mustache", "", "gradle.properties"));
    }


    @Override
    public void setLibrary(String library) {
        this.library = library;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
