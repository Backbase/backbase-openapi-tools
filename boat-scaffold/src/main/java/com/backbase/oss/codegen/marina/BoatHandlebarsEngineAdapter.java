package com.backbase.oss.codegen.marina;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Jackson2Helper;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.FieldValueResolver.FieldWrapper;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.AbstractTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.api.TemplatingExecutor;
import org.openapitools.codegen.templating.HandlebarsEngineAdapter;

@Slf4j
public class BoatHandlebarsEngineAdapter extends HandlebarsEngineAdapter {

    @Override
    public String compileTemplate(TemplatingExecutor executor, Map<String, Object> bundle, String templateFile)
        throws IOException {
        TemplateLoader loader = new AbstractTemplateLoader() {
            @Override
            public TemplateSource sourceAt(String location) {
                return findTemplate(executor, location);
            }
        };

        var MY_FIELD_VALUE_RESOLVER = new FieldValueResolver() {
            @Override
            protected Set<FieldWrapper> members(
                Class<?> clazz) {
                var members = super.members(clazz);
                return members.stream()
                    .filter(fw -> isValidField(fw))
                    .collect(Collectors.toSet());
            }

            boolean isValidField(
                FieldWrapper fw) {
                if (fw instanceof AccessibleObject) {
                    if (isUseSetAccessible(fw)) {
                        return true;
                    }
                    return false;
                }
                return true;
            }
        };
        Context context = Context
                .newBuilder(bundle)
                .resolver(
                        MapValueResolver.INSTANCE,
                        JavaBeanValueResolver.INSTANCE,
                        MethodValueResolver.INSTANCE,
                        MY_FIELD_VALUE_RESOLVER)
                .build();

        Handlebars handlebars = new Handlebars(loader);
        handlebars.registerHelperMissing((obj, options) -> {
            log.warn(String.format(Locale.ROOT, "Unregistered helper name '%s', processing template:%n%s", options.helperName, options.fn.text()));
            return "";
        });
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Helper<Object> instance = new Jackson2Helper(objectMapper);
        handlebars.registerHelper("json", instance);
        StringHelpers.register(handlebars);
        handlebars.registerHelpers(ConditionalHelpers.class);
        handlebars.registerHelpers(org.openapitools.codegen.templating.handlebars.StringHelpers.class);
        Template tmpl = handlebars.compile(templateFile);
        return tmpl.apply(context);
    }

}
