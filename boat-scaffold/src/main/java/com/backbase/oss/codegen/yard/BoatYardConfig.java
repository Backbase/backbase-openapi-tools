package com.backbase.oss.codegen.yard;

import java.io.File;
import lombok.Getter;
import lombok.Setter;
import org.openapitools.codegen.DefaultCodegen;
import org.openapitools.codegen.SupportingFile;

@Setter
@Getter
public class BoatYardConfig extends DefaultCodegen {

    private File specsBaseDir;

    public BoatYardConfig() {
        super();
        library = "boat-yard";
        supportingFiles.add(new SupportingFile("index.handlebars", "index.html"));
        supportingFiles.add(new SupportingFile("css/styles.css", "css/styles.css"));
        supportingFiles.add(new SupportingFile("js/index.js", "js/index.js"));
        supportingFiles.add(new SupportingFile("backbase-logo.svg", "backbase-logo.svg"));
    }


}
