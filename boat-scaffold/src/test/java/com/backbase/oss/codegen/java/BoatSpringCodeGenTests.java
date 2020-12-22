package com.backbase.oss.codegen.java;

import com.backbase.oss.codegen.java.BoatSpringCodeGen.NewLineIndent;
import com.samskivert.mustache.Template.Fragment;
import java.io.IOException;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;

import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class BoatSpringCodeGenTests {

    @Test
    void clientOptsUnicity() {
        final BoatSpringCodeGen gen = new BoatSpringCodeGen();
        gen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> assertEquals( 1,v.size(), k + " is described multiple times"));
    }

    @Test
    void uniquePropertyToSet() {
        final BoatSpringCodeGen gen = new BoatSpringCodeGen();
        final CodegenProperty prop = new CodegenProperty();

        gen.useSetForUniqueItems = true;
        prop.isContainer = true;
        prop.setUniqueItems(true);
        prop.items = new CodegenProperty();
        prop.items.dataType = "String";
        prop.baseType = "java.util.List";
        prop.dataType = "java.util.List<String>";

        gen.postProcessModelProperty(new CodegenModel(), prop);

        assertThat(prop.containerType, is("set"));
        assertThat(prop.baseType, is("java.util.Set"));
        assertThat(prop.dataType, is("java.util.Set<String>"));
    }

    @Test
    void uniqueParameterToSet() {
        final BoatSpringCodeGen gen = new BoatSpringCodeGen();
        final CodegenParameter param = new CodegenParameter();

        gen.useSetForUniqueItems = true;
        param.isContainer = true;
        param.setUniqueItems(true);
        param.items = new CodegenProperty();
        param.items.dataType = "String";
        param.baseType = "java.util.List<String>";
        param.dataType = "java.util.List<String>";

        gen.postProcessParameter(param);

        assertThat(param.baseType, is("java.util.Set"));
        assertThat(param.dataType, is("java.util.Set<String>"));
    }

    @Test
    void newLineIndent() throws IOException {
        final NewLineIndent indent = new BoatSpringCodeGen.NewLineIndent(2, "_");
        final StringWriter output = new StringWriter();
        final Fragment frag = mock(Fragment.class);

        when(frag.execute()).thenReturn("\n Good \r\n   morning,  \r\n  Dave ");

        indent.execute(frag, output);

        assertThat(output.toString(), equalTo(String.format("__%n__Good%n__  morning,%n__ Dave%n")));
    }
}
