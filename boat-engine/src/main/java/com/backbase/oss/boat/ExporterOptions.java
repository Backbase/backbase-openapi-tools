package com.backbase.oss.boat;

import com.backbase.oss.boat.transformers.Transformer;
import java.util.LinkedList;
import java.util.List;

public class ExporterOptions {

    private boolean addJavaTypeExtensions;

    private  boolean convertExamplesToYaml = true;

    private List<Transformer> transformers = new LinkedList<>();

    public boolean isAddJavaTypeExtensions() {
        return addJavaTypeExtensions;
    }

    public void setAddJavaTypeExtensions(boolean addJavaTypeExtensions) {
        this.addJavaTypeExtensions = addJavaTypeExtensions;
    }

    public ExporterOptions addJavaTypeExtensions(boolean addJavaTypeExtensions) {
        this.addJavaTypeExtensions = addJavaTypeExtensions;
        return this;
    }

    public boolean isConvertExamplesToYaml() {
        return convertExamplesToYaml;
    }

    public void setConvertExamplesToYaml(boolean convertExamplesToYaml) {
        this.convertExamplesToYaml = convertExamplesToYaml;
    }

    public ExporterOptions convertExamplesToYaml(boolean convertExamplesToYaml) {
        this.convertExamplesToYaml = convertExamplesToYaml;
        return this;
    }

    public List<Transformer> getTransformers() {
        return transformers;
    }

    public void setTransformers(List<Transformer> transformers) {
        this.transformers = transformers;
    }

    public ExporterOptions transformers(List<Transformer> transformers) {
        this.transformers = transformers;
        return this;
    }
}
