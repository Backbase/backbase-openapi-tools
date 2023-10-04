package com.backbase.oss.boat.transformers.bundler;

import com.backbase.oss.boat.transformers.TransformerException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.examples.Example;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@SuppressWarnings({"rawtypes","java:S3740"})
public abstract class ExampleHolder<T> {

    private static final String FIXING_INVALID_EXAMPLE_WARNING = "%1$s is an invalid example. \n"
        + "You've got:\n"
        + "%1$s:\n"
        + "  value:\n"
        + "    $ref: %2$s\n"
        + "Only the whole example can be a reference object:\n"
        + "%1$s:\n"
        + "  $ref: %2$s";
    private static final String REF_KEY = "$ref";
    public static final String VALUE = "value";

    private static class ExampleExampleHolder extends ExampleHolder<Example> {
        private final boolean componentExample;

        private ExampleExampleHolder(String name, Example example, boolean componentExample) {
            super(name, example);
            this.componentExample = componentExample;
            if (example.get$ref() == null
                && example.getValue() instanceof ObjectNode
                && ((ObjectNode) example.getValue()).get(REF_KEY) != null) {
                String ref = ((ObjectNode) example.getValue()).get(REF_KEY).asText();
                log.warn(String.format(FIXING_INVALID_EXAMPLE_WARNING, name, ref));
                example.set$ref(ref);
                example.setValue(null);
            }
        }

        @Override
        String getRef() {
            return example().get$ref();
        }

        @Override
        void replaceRef(String ref) {
            if (componentExample) {
                example().set$ref(null);
            } else {
                example().set$ref(ref);
            }
        }

    }


    public static class ObjectNodeExampleHolder extends ExampleHolder<ObjectNode> {
        private ObjectNodeExampleHolder(String name, ObjectNode objectNode) {
            super(name, objectNode);
            if (objectNode.get(REF_KEY) == null
                && objectNode.get(VALUE) != null
                && objectNode.get(VALUE).get(REF_KEY) != null) {
                String ref = objectNode.get(VALUE).get(REF_KEY).asText();
                log.warn(String.format(FIXING_INVALID_EXAMPLE_WARNING, "?", ref));
                objectNode.set(REF_KEY, objectNode.get(VALUE).get(REF_KEY));
                objectNode.remove(VALUE);
            }

        }

        @Override
        String getRef() {
            JsonNode jsonNode = example().get(REF_KEY);
            if(jsonNode != null) {
                return jsonNode.asText();
            }
            return null;
        }

        @Override
        void replaceRef(String ref) {
            example().put(REF_KEY, ref);
        }
    }

    public static class ArrayNodeExampleHolder extends ExampleHolder<ArrayNode> {
        private ArrayNodeExampleHolder(String name, ArrayNode arrayNode) {
            super(name, arrayNode);
        }

        @Override
        String getRef() {
            return null;
        }

        @Override
        void replaceRef(String ref) {
            // not applicable
        }
    }

    private static class MapExampleHolder extends ExampleHolder<Map> {

        private MapExampleHolder(String name, Map map) {
            super(name, map);
        }

        @Override
        String getRef() {
            return (String) example().get("$ref");
        }

        @Override
        void replaceRef(String ref) {
            example().put(REF_KEY, ref);
        }
    }

    private static class StringExampleHolder extends ExampleHolder<String> {

        private StringExampleHolder(String name, String value) {
            super(name, value);
            setContent(value);
        }

        @Override
        String getRef() {
            return null;
        }

        @Override
        void replaceRef(String ref) {
            // do nothing
        }
    }

    private final String name;

    private T example;
    private String content;

    private ExampleHolder(String name, T example) {
        super();
        this.name = name;
        this.example = example;
    }

    public T example() {
        return example;
    }

    public void setExample(T example) {
        this.example = example;
    }

    abstract String getRef();

    abstract void replaceRef(String ref);

    public static ExampleHolder<Example> of(String name, Example example, boolean isComponentExample) {
        return new ExampleExampleHolder(name, example, isComponentExample);
    }

    public static ExampleHolder of(String name, Object o) {
        if (o instanceof ObjectNode) {
            return new ObjectNodeExampleHolder(name, (ObjectNode) o);
        } else if (o instanceof Map) {
            return new MapExampleHolder(name, (Map) o);
        } else if (o instanceof Example) {
            boolean isComponentSection = ((Example) o).get$ref() != null && ((Example) o).get$ref().startsWith("#/components");
            try {
                String s = Json.mapper().writeValueAsString(o);
                return new ObjectNodeExampleHolder(name, (ObjectNode) Json.mapper().readTree(s));
            } catch (JsonProcessingException e) {
                return new ExampleExampleHolder(name, (Example) o, isComponentSection);
            }
        } else if( o instanceof ArrayNode) {
            return new ArrayNodeExampleHolder(name, (ArrayNode) o);
        } else if (o instanceof String) {
            return new StringExampleHolder(name, o.toString());
        } else if (o instanceof byte[]) {
            return new StringExampleHolder(name, new String((byte[]) o));
        } else {
            throw new TransformerException(String.format(
                    "Unknown type backing example %s (%s) '%s'", name, o.getClass().getName(), o));
        }
    }

    @Override
    public String toString() {
        return "ExampleHolder{"
            + "name='" + name + '\''
            + ", ref=" + getRef()
            + '}';
    }

    public String getExampleName() {
        return name != null ? name
            : StringUtils.replaceEach(getRef(),
            new String[]{"./", "examples/", ".json", ".", "/"},
            new String[]{"", "", "", "", "-"});
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
