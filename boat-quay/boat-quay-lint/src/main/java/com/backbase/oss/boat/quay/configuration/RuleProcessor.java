package com.backbase.oss.boat.quay.configuration;

import com.google.auto.service.AutoService;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.zalando.zally.rule.api.Rule;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class RuleProcessor extends AbstractProcessor {

    Set<String> rules = new TreeSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            processComplete();
        } else {
            processRound(roundEnv);
        }
        return true;
    }

    private void processRound(RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(Rule.class).stream()
            .filter(this::isInstanceOfTypeElement)
            .map(TypeElement.class::cast)
            .map(this::getQualifiedName)
            .forEach(ruleClass -> {
                System.out.println("Adding " + ruleClass + " to services");
                rules.add(ruleClass);
            });
    }

    private String getQualifiedName(TypeElement element) {
        return element.getQualifiedName().toString();
    }

    private boolean isInstanceOfTypeElement(javax.lang.model.element.Element element) {
        return element instanceof TypeElement;
    }

    private void processComplete() {
        try {
            FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + Rule.class);
            Writer writer = file.openWriter();
            for (String rule : rules) {
                writer.write(rule);
                writer.write(System.lineSeparator());
            }
            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot complete processing of rules: " + String.join(",", rules));
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Rule.class.getName());

    }
}
