package com.backbase.oss.codegen.lint;

import java.io.File;
import lombok.Getter;
import lombok.Setter;
import org.openapitools.codegen.DefaultCodegen;
import org.openapitools.codegen.SupportingFile;

@Setter
@Getter
public class BoatLintConfig extends DefaultCodegen {

    public BoatLintConfig() {
        super();
        library = "boat-lint";


        supportingFiles.add(new SupportingFile("index.handlebars", "index.html"));
        supportingFiles.add(new SupportingFile("css/styles.css", "css/styles.css"));
        supportingFiles.add(new SupportingFile("js/index.js", "js/index.js"));
        supportingFiles.add(new SupportingFile("js/ace.js", "js/ace.js"));
        supportingFiles.add(new SupportingFile("js/mode-yaml.js", "js/mode-yaml.js"));
        supportingFiles.add(new SupportingFile("js/theme-twilight.js", "js/theme-twilight.js"));
        supportingFiles.add(new SupportingFile("js/ext-static_highlight.js", "js/ext-static_highlight.js"));
        supportingFiles.add(new SupportingFile("backbase-logo.svg", "backbase-logo.svg"));
    }


}
