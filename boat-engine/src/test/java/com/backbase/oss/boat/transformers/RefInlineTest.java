package com.backbase.oss.boat.transformers;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RefInlineTest {

    @Parameterized.Parameters(name = "{0}")
    static public Object data() {
        return new Object[] {
            new Object[] {"DEST"},
            new Object[] {"DEST/schemas"},
            new Object[] {"DEST/shared/schemas"},
        };
    }

    @BeforeClass
    static public void cleanUp() {
        FileUtils.deleteQuietly(new File("target/ref-inline"));
    }

    private final String targetPath;
    private final Path schemasPath;

    public RefInlineTest(String testName) {
        this.targetPath = testName.toLowerCase().replace('/', '-');
        this.schemasPath = Paths.get(testName.replaceFirst("^DEST/?", ""));
    }

    private RefInline transform(String sample) throws OpenAPILoaderException {
        final File file = new File(format("src/test/resources/openapi/ref-inline/%s.yaml", sample));
        final OpenAPI openAPI = OpenAPILoader.load(file);
        final Path output = Paths.get(format("target/ref-inline/%1$s/%2$s/%2$s-out.yaml", this.targetPath, sample));
        final RefInline trn = new RefInline(output, this.schemasPath);

        trn.transform(openAPI);
        trn.export(true);

        return trn;
    }

    @Test
    public void simple() throws OpenAPILoaderException {
        final RefInline trn = transform("simple");
        final OpenAPI openAPI = trn.getOpenAPI();

        final Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        final Map<Path, Schema> files = trn.getFiles();

        assertThat(files.values(), hasSize(3));
        assertThat(openAPI.getComponents().getSchemas(), hasKey("One"));
        assertThat(openAPI.getComponents().getSchemas(), hasKey("Two"));
        assertThat(openAPI.getComponents().getSchemas(), hasKey("Three"));

        schemas.forEach((name, s) -> assertThat(isNotBlank(s.get$ref()), is(true)));
        files.forEach((file, s) -> assertThat(isBlank(s.get$ref()), is(true)));

        final Schema s1 = files.get(this.schemasPath.resolve("one.yaml"));
        final Schema s2 = files.get(this.schemasPath.resolve("two.yaml"));
        final Schema s3 = files.get(this.schemasPath.resolve("three.yaml"));

        assertThat(s1, is(notNullValue()));
        assertThat(s2, is(notNullValue()));
        assertThat(s3, is(notNullValue()));

        assertRefsUpdated(s1);
    }

    @Test
    public void nestedWithRefs() throws OpenAPILoaderException {
        final RefInline trn = transform("nested-with-refs");
        final OpenAPI openAPI = trn.getOpenAPI();

        final Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        final Map<Path, Schema> files = trn.getFiles();

        assertThat(files.values(), hasSize(3));
        assertThat(openAPI.getComponents().getSchemas(), hasKey("One"));
        assertThat(openAPI.getComponents().getSchemas(), hasKey("OnePropTwo"));
        assertThat(openAPI.getComponents().getSchemas(), hasKey("Three"));

        schemas.forEach((name, s) -> assertThat(isNotBlank(s.get$ref()), is(true)));
        files.forEach((file, s) -> assertThat(isBlank(s.get$ref()), is(true)));

        final Schema s1 = files.get(this.schemasPath.resolve("one.yaml"));
        final Schema s2 = files.get(this.schemasPath.resolve("one/prop-two.yaml"));
        final Schema s3 = files.get(this.schemasPath.resolve("three.yaml"));

        assertThat(s1, is(notNullValue()));
        assertThat(s2, is(notNullValue()));
        assertThat(s3, is(notNullValue()));

        assertRefsUpdated(s1);
    }

    @Test
    public void nestedFully() throws OpenAPILoaderException {
        final RefInline trn = transform("nested-fully");
        final OpenAPI openAPI = trn.getOpenAPI();

        final Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        final Map<Path, Schema> files = trn.getFiles();

        assertThat(files.values(), hasSize(4));
        assertThat(openAPI.getComponents().getSchemas(), hasKey("One"));
        assertThat(openAPI.getComponents().getSchemas(), hasKey("OnePropTwo"));
        assertThat(openAPI.getComponents().getSchemas(), hasKey("OnePropTwoPropThree"));
        assertThat(openAPI.getComponents().getSchemas(), hasKey("OnePropTwoPropThreePropFour"));

        schemas.forEach((name, s) -> assertThat(isNotBlank(s.get$ref()), is(true)));
        files.forEach((file, s) -> assertThat(isBlank(s.get$ref()), is(true)));

        final Schema s1 = files.get(this.schemasPath.resolve("one.yaml"));
        final Schema s2 = files.get(this.schemasPath.resolve("one/prop-two.yaml"));
        final Schema s3 = files.get(this.schemasPath.resolve("one/prop-two/prop-three.yaml"));
        final Schema s4 = files.get(this.schemasPath.resolve("one/prop-two/prop-three/prop-four.yaml"));

        assertThat(s1, is(notNullValue()));
        assertThat(s2, is(notNullValue()));
        assertThat(s3, is(notNullValue()));
        assertThat(s4, is(notNullValue()));

        assertRefsUpdated(s1);
    }

    private void assertRefsUpdated(Schema<?> s) {
        // s.getProperties()
        // .entrySet().stream()
        // .filter(e -> isNotBlank(e.getValue().get$ref()))
        // .forEach(e -> assertThat(
        // format("%s.%s", s.getTitle(), e.getKey()), e.getValue().get$ref(),
        // startsWith("../openapi.yaml#/")));
    }

}


