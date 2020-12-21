package com.backbase.oss.boat;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.raml.v2.api.model.v10.datamodel.AnyTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.ArrayTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.BooleanTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.DateTimeOnlyTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.DateTimeTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.DateTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.FileTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.IntegerTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.NullTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.NumberTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.StringTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TimeOnlyTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.UnionTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.XMLTypeDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"java:S3776","java:S3740", "rawtypes"})
@UtilityClass
public class RamlSchemaToOpenApi {

    private static final Logger log = LoggerFactory.getLogger(RamlSchemaToOpenApi.class);

    public static Schema convert(String name, TypeDeclaration type, Components components) {
        if (log.isDebugEnabled()){
            log.debug("Creating Schema: {} from RAML type: {}", name, type.type());
        }

        Schema schema;
        if (type instanceof ArrayTypeDeclaration) {
            schema = arrayDeclaration(name,type,components);
        } else if (type instanceof StringTypeDeclaration) {
            schema = stringDeclaration(type);
        } else if (type instanceof BooleanTypeDeclaration) {
            schema = new BooleanSchema();
        } else if ((type instanceof DateTimeOnlyTypeDeclaration)
            || (type instanceof DateTimeTypeDeclaration)
            || (type instanceof TimeOnlyTypeDeclaration)) {
            schema = new DateTimeSchema();
        } else if (type instanceof DateTypeDeclaration) {
            schema = new DateSchema();
        } else if (type instanceof IntegerTypeDeclaration) {
            schema = new IntegerSchema();
        } else if (type instanceof NumberTypeDeclaration) {
            schema = new NumberSchema();
        } else if (type instanceof ObjectTypeDeclaration) {
            schema = objectDeclaration(name,type,components);
        } else if (type instanceof AnyTypeDeclaration) {
            schema = new Schema().type("string").nullable(true);
        } else if (type instanceof NullTypeDeclaration) {
            NullTypeDeclaration nullTypeDeclaration = (NullTypeDeclaration) type;
            schema = new StringSchema();
            schema.setNullable(true);
            schema.setName(nullTypeDeclaration.name());
        } else if (type instanceof FileTypeDeclaration) {
            FileTypeDeclaration fileTypeDeclaration = (FileTypeDeclaration) type;
            schema = new FileSchema();
            schema.setDescription(fileTypeDeclaration.description() != null ? fileTypeDeclaration.description().value() : null);
        } else if (type instanceof XMLTypeDeclaration) {
            XMLTypeDeclaration xmlTypeDeclaration = (XMLTypeDeclaration) type;
            String schemaContent = xmlTypeDeclaration.schemaContent();
            schema = XmlSchemaToOpenApi.convert(name, schemaContent, components);
            schema.setDescription(xmlTypeDeclaration.description() != null ? xmlTypeDeclaration.description().value() : null);
        } else if (type instanceof UnionTypeDeclaration) {

            schema = unionDeclaration(type,components);

        } else {
            throw new UnsupportedOperationException("Not yet implemented");
        }


        return depreciate(schema,name,type);
    }

    private static Schema arrayDeclaration(String name, TypeDeclaration type, Components components){
        Schema schema = new ArraySchema();
        ArrayTypeDeclaration arrayTypeDeclaration = (ArrayTypeDeclaration) type;
        Schema itemSchema = convert(name, arrayTypeDeclaration.items(), components);
        ((ArraySchema) schema).setItems(itemSchema);
        if (arrayTypeDeclaration.maxItems() != null) {
            ((ArraySchema) schema).setMaximum(new BigDecimal(arrayTypeDeclaration.maxItems()));
        }
        if (arrayTypeDeclaration.minItems() != null) {
            ((ArraySchema) schema).setMinimum(new BigDecimal(arrayTypeDeclaration.minItems()));
        }
        return schema;
    }

    private static Schema stringDeclaration( TypeDeclaration type){
        Schema schema = new StringSchema();
        StringTypeDeclaration stringTypeDeclaration = (StringTypeDeclaration) type;
        schema.setPattern(stringTypeDeclaration.pattern());
        schema.setMaxLength(stringTypeDeclaration.maxLength());
        schema.setMinLength(stringTypeDeclaration.minLength());

        if (!stringTypeDeclaration.enumValues().isEmpty()) {
            schema.setEnum(stringTypeDeclaration.enumValues());
        }
        return schema;
    }

    private static Schema objectDeclaration(String name, TypeDeclaration type, Components components){
        Schema schema = new ObjectSchema();
        schema.setName(name);
        ObjectSchema objectSchema = (ObjectSchema) schema;
        ObjectTypeDeclaration objectTypeDeclaration = (ObjectTypeDeclaration) type;
        objectTypeDeclaration.properties().forEach(typeDeclaration -> {
            Schema propertySchmea = convert(typeDeclaration.name(), typeDeclaration, components);
            objectSchema.addProperties(typeDeclaration.name(), propertySchmea);
        });

        return schema;
    }

    private static Schema unionDeclaration(TypeDeclaration type, Components components){
        UnionTypeDeclaration unionTypeDeclaration = (UnionTypeDeclaration) type;

        List<Schema> of = unionTypeDeclaration.of().stream().map(typeDeclaration ->
                convert(typeDeclaration.name(), typeDeclaration, components))
                .collect(Collectors.toList());

        Schema schema = new ComposedSchema();
        ((ComposedSchema) schema).setAnyOf(of);
        schema.setName(unionTypeDeclaration.name());
        schema.setDescription(unionTypeDeclaration.description() != null ? unionTypeDeclaration.description().value() : null);
        return schema;
    }



    private static Schema depreciate(Schema schema, String name, TypeDeclaration type){
        schema.setName(name);
        String description = type.description() != null ? type.description().value() : null;
        if (description != null && (description.contains("deprecated") || (description.contains("@deprecated")))) {
            schema.setDeprecated(true);
        }

        if (type.defaultValue() != null) {
            schema.setDefault(type.defaultValue());
        }
        return schema;
    }


}
