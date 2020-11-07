package com.backbase.oss.codegen;

import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backbase.oss.codegen.SpringBoatCodeGen.NewLineIndent;
import com.samskivert.mustache.Template.Fragment;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;

public class SpringBoatCodeGenTests {

    @Test
    public void clientOptsUnicity() {
        final SpringBoatCodeGen gen = new SpringBoatCodeGen();
        gen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> Assert.assertEquals(k + " is described multiple times", v.size(), 1));
    }

    @Test
    public void uniquePropertyToSet() {
        final SpringBoatCodeGen gen = new SpringBoatCodeGen();
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
    public void uniqueParameterToSet() {
        final SpringBoatCodeGen gen = new SpringBoatCodeGen();
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
    public void newLineIndent() throws IOException {
        final NewLineIndent indent = new SpringBoatCodeGen.NewLineIndent(2, "_");
        final StringWriter output = new StringWriter();
        final Fragment frag = mock(Fragment.class);

        when(frag.execute()).thenReturn("\n Good \r\n   morning,  \r\n  Dave ");

        indent.execute(frag, output);

        assertThat(output.toString(), equalTo("__\n__Good\n__  morning,\n__ Dave\n"));
    }
}
