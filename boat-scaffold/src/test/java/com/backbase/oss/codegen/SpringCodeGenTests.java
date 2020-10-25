package com.backbase.oss.codegen;

import org.junit.Assert;
import org.junit.Test;
import org.openapitools.codegen.CliOption;

import static java.util.stream.Collectors.groupingBy;

public class SpringCodeGenTests {

    @Test
    public void clientOptsUnicity() {
        SpringCodeGen gen = new SpringCodeGen();
        gen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> Assert.assertEquals(k + " is described multiple times", v.size(), 1));
    }
}
