package com.backbase.oss.boat;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.XML;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Log
public class XmlSchemaToOpenApi {
    private static DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

    @SneakyThrows
    public static Schema convert(String name, String schemaContent, Components components) {
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(IOUtils.toInputStream(schemaContent, "UTF-8"));

        XML xml = new XML();
        xml.addExtension("x-bb-schema-content", schemaContent);
        Schema root = new Schema();
        root.name(name);
        map(doc.getDocumentElement(), root, components);
        components.addSchemas(name, root);
        return root;

    }

    public static void map(Element node, Schema schema, Components components) {

        NodeList complexTypes = ((Element) node).getElementsByTagName("xs:complexType");
        for (int i = 0; i < complexTypes.getLength(); i++) {
            Element complexType = (Element) complexTypes.item(i);
            String name = complexType.getAttribute("name");

            Schema complexTypeSchema = components.getSchemas().getOrDefault(name, new ObjectSchema());
            map(complexType, complexTypeSchema, components);
            components.addSchemas(name, complexTypeSchema);
        }
        NodeList elements = ((Element) node).getElementsByTagName("xs:element");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            String name = element.getAttribute("name");
            String type = element.getAttribute("type");
            if (StringUtils.isEmpty(type)) {
                type = "string";
            }
            Schema propertySchema = createSchemaFor(name, type, components);
            propertySchema.setName(name);
            schema.addProperties(name, propertySchema);
        }
        NodeList sequences = ((Element) node).getElementsByTagName("xs:sequence");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);

            String name = node.getAttribute("name");
            ArraySchema arraySchema = new ArraySchema();
            arraySchema.setName(name);
            Schema itemSchema = new Schema();
            map(element, itemSchema, components);
            arraySchema.setItems(itemSchema);

            schema.$ref(name);
        }
    }


    private static Schema createSchemaFor(String propertyName, String type, Components components) {
        switch (type) {
            case "xs:string":
                return new StringSchema();
            case "xs:boolean":
                return new BooleanSchema();
            case "xs:int":
                return new IntegerSchema();

        }
        Schema complexType = components.getSchemas().get(type);
        if (complexType == null) {
            complexType = new ObjectSchema();
            components.addSchemas(type, complexType);

        }
        return complexType;

    }
}
