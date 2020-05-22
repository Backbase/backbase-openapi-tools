package com.backbase.oss.boat.transformers;

import com.backbase.oss.boat.example.NamedExample;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class CommonExtractorsTest {

    @Test
    public void test_headerExamples_noExample() {
        Header header = new Header();
        List<NamedExample> examples = CommonExtractors.headerExamples("some-name", header);
        assertTrue(examples.isEmpty());
    }

    @Test
    public void test_headerExamples_nullExamplesMap() {
        Header header = new Header().examples(null);
        List<NamedExample> examples = CommonExtractors.headerExamples("some-name", header);
        assertTrue(examples.isEmpty());
    }

    @Test
    public void test_headerExamples_examplesMapNotEmpty() {
        Schema<?> schema = new Schema<>().name("MySchema");
        String key = "example-1";
        Example example = new Example().value("1234");
        Header header = new Header()
                .addExample(key, example)
                .schema(schema);
        List<NamedExample> examples = CommonExtractors.headerExamples("Dummy-name", header);
        assertFalse(examples.isEmpty());
        assertEquals(1, examples.size());
        assertEquals(example, examples.get(0).getExample());
        assertEquals(schema.getName() + "-" + key, examples.get(0).getName());
    }

    @Test
    public void test_headerExamples_ownExamplePresent() {
        String name = "X-Items-Count";
        Header header = new Header().example("1234");
        List<NamedExample> examples = CommonExtractors.headerExamples(name, header);
        assertFalse(examples.isEmpty());
        assertEquals(1, examples.size());
        assertEquals(name, examples.get(0).getName());
    }

    @Test
    public void test_headerExamples_ownExamplePresentAndExamplesMapNotEmpty() {
        Schema<?> schema = new Schema<>().$ref("#/components/schemas/MySchema");
        String name = "X-Items-Count";
        String key = "example-1";
        Example example = new Example().value("dummy-val");
        Header header = new Header()
                .example("1234")
                .schema(schema)
                .addExample(key, example);
        List<NamedExample> examples = CommonExtractors.headerExamples(name, header);
        assertFalse(examples.isEmpty());
        assertEquals(2, examples.size());
        assertEquals(1, examples.stream()
                .filter(namedExample -> namedExample.getExample() == example
                        && namedExample.getName().equals("MySchema-" + key))
                .count()
        );
        assertEquals(1, examples.stream()
                .filter(namedExample -> namedExample.getExample().getValue() == header.getExample()
                        && namedExample.getName().equals(name))
                .count()
        );
    }
}