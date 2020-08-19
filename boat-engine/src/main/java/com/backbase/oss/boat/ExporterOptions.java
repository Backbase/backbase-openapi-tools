package com.backbase.oss.boat;

import com.backbase.oss.boat.transformers.Transformer;
import java.util.LinkedList;
import java.util.List;
import lombok.Data;

@Data
public class ExporterOptions {

    private boolean addJavaTypeExtensions = false;
    private boolean convertExamplesToYaml = true;
    private List<Transformer> transformers = new LinkedList<>();

    public ExporterOptions convertExamplesToYaml(boolean convertExamplesToYaml) {
        this.convertExamplesToYaml = convertExamplesToYaml;
        return this;
    }

    public ExporterOptions transformers(List<Transformer> transformers) {
        this.transformers = transformers;
        return this;
    }

    public ExporterOptions addJavaTypeExtensions(boolean addJavaTypeExtensions) {
        this.addJavaTypeExtensions = addJavaTypeExtensions;
        return this;
    }

}
