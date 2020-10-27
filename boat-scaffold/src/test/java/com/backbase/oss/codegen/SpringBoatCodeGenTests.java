package com.backbase.oss.codegen;

import org.junit.Assert;
import org.junit.Test;
import org.openapitools.codegen.CliOption;

import static java.util.stream.Collectors.groupingBy;

public class SpringBoatCodeGenTests {

    @Test
    public void clientOptsUnicity() {
        SpringBoatCodeGen gen = new SpringBoatCodeGen();
        gen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> Assert.assertEquals(k + " is described multiple times", v.size(), 1));
    }
}
