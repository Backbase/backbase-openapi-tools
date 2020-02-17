package com.backbase.boat.transformers;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.XML;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Deduplicator implements Transformer {

    public void transform(OpenAPI openAPI) {

        Collection<Schema> values = openAPI.getComponents().getSchemas().values();


        Set<Schema> duplicates = values.stream().filter(schema -> Collections.frequency(values, schema) > 1).collect(Collectors.toSet());


    }

    private static class DedupSchema {

        private final Schema schema;


        private DedupSchema(Schema schema) {
            this.schema = schema;
        }

        public String getName() {
            return schema.getName();
        }

        public void setName(String name) {
            schema.setName(name);
        }

        public Schema name(String name) {
            return schema.name(name);
        }

        public Discriminator getDiscriminator() {
            return schema.getDiscriminator();
        }

        public void setDiscriminator(Discriminator discriminator) {
            schema.setDiscriminator(discriminator);
        }

        public Schema discriminator(Discriminator discriminator) {
            return schema.discriminator(discriminator);
        }

        public String getTitle() {
            return schema.getTitle();
        }

        public void setTitle(String title) {
            schema.setTitle(title);
        }

        public Schema title(String title) {
            return schema.title(title);
        }

        public Object getDefault() {
            return schema.getDefault();
        }

        public void setDefault(Object _default) {
            schema.setDefault(_default);
        }

        public List getEnum() {
            return schema.getEnum();
        }

        public void setEnum(List _enum) {
            schema.setEnum(_enum);
        }

        public void addEnumItemObject(Object _enumItem) {
            schema.addEnumItemObject(_enumItem);
        }

        public BigDecimal getMultipleOf() {
            return schema.getMultipleOf();
        }

        public void setMultipleOf(BigDecimal multipleOf) {
            schema.setMultipleOf(multipleOf);
        }

        public Schema multipleOf(BigDecimal multipleOf) {
            return schema.multipleOf(multipleOf);
        }

        public BigDecimal getMaximum() {
            return schema.getMaximum();
        }

        public void setMaximum(BigDecimal maximum) {
            schema.setMaximum(maximum);
        }

        public Schema maximum(BigDecimal maximum) {
            return schema.maximum(maximum);
        }

        public Boolean getExclusiveMaximum() {
            return schema.getExclusiveMaximum();
        }

        public void setExclusiveMaximum(Boolean exclusiveMaximum) {
            schema.setExclusiveMaximum(exclusiveMaximum);
        }

        public Schema exclusiveMaximum(Boolean exclusiveMaximum) {
            return schema.exclusiveMaximum(exclusiveMaximum);
        }

        public BigDecimal getMinimum() {
            return schema.getMinimum();
        }

        public void setMinimum(BigDecimal minimum) {
            schema.setMinimum(minimum);
        }

        public Schema minimum(BigDecimal minimum) {
            return schema.minimum(minimum);
        }

        public Boolean getExclusiveMinimum() {
            return schema.getExclusiveMinimum();
        }

        public void setExclusiveMinimum(Boolean exclusiveMinimum) {
            schema.setExclusiveMinimum(exclusiveMinimum);
        }

        public Schema exclusiveMinimum(Boolean exclusiveMinimum) {
            return schema.exclusiveMinimum(exclusiveMinimum);
        }

        public Integer getMaxLength() {
            return schema.getMaxLength();
        }

        public void setMaxLength(Integer maxLength) {
            schema.setMaxLength(maxLength);
        }

        public Schema maxLength(Integer maxLength) {
            return schema.maxLength(maxLength);
        }

        public Integer getMinLength() {
            return schema.getMinLength();
        }

        public void setMinLength(Integer minLength) {
            schema.setMinLength(minLength);
        }

        public Schema minLength(Integer minLength) {
            return schema.minLength(minLength);
        }

        public String getPattern() {
            return schema.getPattern();
        }

        public void setPattern(String pattern) {
            schema.setPattern(pattern);
        }

        public Schema pattern(String pattern) {
            return schema.pattern(pattern);
        }

        public Integer getMaxItems() {
            return schema.getMaxItems();
        }

        public void setMaxItems(Integer maxItems) {
            schema.setMaxItems(maxItems);
        }

        public Schema maxItems(Integer maxItems) {
            return schema.maxItems(maxItems);
        }

        public Integer getMinItems() {
            return schema.getMinItems();
        }

        public void setMinItems(Integer minItems) {
            schema.setMinItems(minItems);
        }

        public Schema minItems(Integer minItems) {
            return schema.minItems(minItems);
        }

        public Boolean getUniqueItems() {
            return schema.getUniqueItems();
        }

        public void setUniqueItems(Boolean uniqueItems) {
            schema.setUniqueItems(uniqueItems);
        }

        public Schema uniqueItems(Boolean uniqueItems) {
            return schema.uniqueItems(uniqueItems);
        }

        public Integer getMaxProperties() {
            return schema.getMaxProperties();
        }

        public void setMaxProperties(Integer maxProperties) {
            schema.setMaxProperties(maxProperties);
        }

        public Schema maxProperties(Integer maxProperties) {
            return schema.maxProperties(maxProperties);
        }

        public Integer getMinProperties() {
            return schema.getMinProperties();
        }

        public void setMinProperties(Integer minProperties) {
            schema.setMinProperties(minProperties);
        }

        public Schema minProperties(Integer minProperties) {
            return schema.minProperties(minProperties);
        }

        public List<String> getRequired() {
            return schema.getRequired();
        }

        public void setRequired(List required) {
            schema.setRequired(required);
        }

        public Schema required(List required) {
            return schema.required(required);
        }

        public Schema addRequiredItem(String requiredItem) {
            return schema.addRequiredItem(requiredItem);
        }

        public String getType() {
            return schema.getType();
        }

        public void setType(String type) {
            schema.setType(type);
        }

        public Schema type(String type) {
            return schema.type(type);
        }

        public Schema getNot() {
            return schema.getNot();
        }

        public void setNot(Schema not) {
            schema.setNot(not);
        }

        public Schema not(Schema not) {
            return schema.not(not);
        }

        public Map<String, Schema> getProperties() {
            return schema.getProperties();
        }

        public void setProperties(Map properties) {
            schema.setProperties(properties);
        }

        public Schema properties(Map properties) {
            return schema.properties(properties);
        }

        public Schema addProperties(String key, Schema propertiesItem) {
            return schema.addProperties(key, propertiesItem);
        }

        public Object getAdditionalProperties() {
            return schema.getAdditionalProperties();
        }

        public void setAdditionalProperties(Object additionalProperties) {
            schema.setAdditionalProperties(additionalProperties);
        }

        public Schema additionalProperties(Object additionalProperties) {
            return schema.additionalProperties(additionalProperties);
        }

        public String getDescription() {
            return schema.getDescription();
        }

        public void setDescription(String description) {
            schema.setDescription(description);
        }

        public Schema description(String description) {
            return schema.description(description);
        }

        public String getFormat() {
            return schema.getFormat();
        }

        public void setFormat(String format) {
            schema.setFormat(format);
        }

        public Schema format(String format) {
            return schema.format(format);
        }

        public String get$ref() {
            return schema.get$ref();
        }

        public void set$ref(String $ref) {
            schema.set$ref($ref);
        }

        public Schema $ref(String $ref) {
            return schema.$ref($ref);
        }

        public Boolean getNullable() {
            return schema.getNullable();
        }

        public void setNullable(Boolean nullable) {
            schema.setNullable(nullable);
        }

        public Schema nullable(Boolean nullable) {
            return schema.nullable(nullable);
        }

        public Boolean getReadOnly() {
            return schema.getReadOnly();
        }

        public void setReadOnly(Boolean readOnly) {
            schema.setReadOnly(readOnly);
        }

        public Schema readOnly(Boolean readOnly) {
            return schema.readOnly(readOnly);
        }

        public Boolean getWriteOnly() {
            return schema.getWriteOnly();
        }

        public void setWriteOnly(Boolean writeOnly) {
            schema.setWriteOnly(writeOnly);
        }

        public Schema writeOnly(Boolean writeOnly) {
            return schema.writeOnly(writeOnly);
        }

        public Object getExample() {
            return schema.getExample();
        }

        public void setExample(Object example) {
            schema.setExample(example);
        }

        public Schema example(Object example) {
            return schema.example(example);
        }

        public ExternalDocumentation getExternalDocs() {
            return schema.getExternalDocs();
        }

        public void setExternalDocs(ExternalDocumentation externalDocs) {
            schema.setExternalDocs(externalDocs);
        }

        public Schema externalDocs(ExternalDocumentation externalDocs) {
            return schema.externalDocs(externalDocs);
        }

        public Boolean getDeprecated() {
            return schema.getDeprecated();
        }

        public void setDeprecated(Boolean deprecated) {
            schema.setDeprecated(deprecated);
        }

        public Schema deprecated(Boolean deprecated) {
            return schema.deprecated(deprecated);
        }

        public XML getXml() {
            return schema.getXml();
        }

        public void setXml(XML xml) {
            schema.setXml(xml);
        }

        public Schema xml(XML xml) {
            return schema.xml(xml);
        }

        public Map<String, Object> getExtensions() {
            return schema.getExtensions();
        }

        public void addExtension(String name, Object value) {
            schema.addExtension(name, value);
        }

        public void setExtensions(Map extensions) {
            schema.setExtensions(extensions);
        }

        public Schema extensions(Map extensions) {
            return schema.extensions(extensions);
        }
    }


}
