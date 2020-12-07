package com.backbase.oss.codegen.angular;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static com.backbase.oss.codegen.angular.BoatAngularTemplatesRun.PROP_FAST;
import static com.backbase.oss.codegen.angular.BoatAngularTemplatesRun.TEST_OUTPUTS;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * These tests verifies that the code generation works for various combinations of configuration
 * parameters; the projects that are generated are later compiled in the integration test phase.
 */
public class BoatAngularTemplatesTests {

    @BeforeAll
    static public void setUpClass() throws IOException {
        Files.createDirectories(Paths.get(TEST_OUTPUTS));
        FileUtils.deleteDirectory(new File(TEST_OUTPUTS));
    }

    @TestFactory
    Stream<DynamicNode> withCombinations() {
        return BoatAngularTemplatesRun.Combination
            .combinations(PROP_FAST)
            .map(param -> dynamicContainer(param.name, testStream(param)));
    }




    private Stream<DynamicTest> testStream(BoatAngularTemplatesRun.Combination param) {
        final BoatAngularTemplatesRun test = new BoatAngularTemplatesRun(param);

        return concat(
            Stream.of(dynamicTest("generate", () -> test.generate())),
            stream(BoatAngularTemplatesRun.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Test.class))
                .map(m -> dynamicTest(m.getName(), () -> invoke(test, m))));
    }

    @SneakyThrows
    private void invoke(BoatAngularTemplatesRun test, Method m) {
        m.invoke(test);
    }



}
