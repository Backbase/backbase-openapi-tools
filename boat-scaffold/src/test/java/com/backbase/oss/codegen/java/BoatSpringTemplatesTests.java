package com.backbase.oss.codegen.java;

import static java.util.Arrays.stream;
import static org.junit.jupiter.api.DynamicContainer.*;
import static org.junit.jupiter.api.DynamicTest.*;
import static java.util.stream.Stream.*;
import com.backbase.oss.codegen.java.BoatSpringTemplatesRun.Combination;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static com.backbase.oss.codegen.java.BoatSpringTemplatesRun.*;

/**
 * Factory for that the code generation tests.
 * <p>
 * With Junit4, the test hierarchy was {@code root-> combination -> method}. The code relied on that
 * structure to speedup the suite execution.
 * </p>
 * <p>
 * With Jupiter, the hierarchy is {@code root -> method -> combination}, that's why the suite is
 * created dynamically and the actual testing code has been moved to {@link BoatSpringTemplatesRun}.
 * </p>
 */
class BoatSpringTemplatesTests {

    @BeforeAll
    static public void setUpClass() throws IOException {
        Files.createDirectories(Paths.get(TEST_OUTPUS));
        FileUtils.deleteDirectory(new File(TEST_OUTPUS, "src"));
    }

    @TestFactory
    Stream<DynamicNode> withCombinations() {
        return Combination
            .combinations(PROP_FAST)
            .map(param -> dynamicContainer(param.name, testStream(param)));
    }

    private Stream<DynamicTest> testStream(Combination param) {
        final BoatSpringTemplatesRun test = new BoatSpringTemplatesRun(param);

        return concat(
            Stream.of(dynamicTest("generate", () -> test.generate())),
            stream(BoatSpringTemplatesRun.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Test.class))
                .map(m -> dynamicTest(m.getName(), () -> invoke(test, m))));
    }

    @SneakyThrows
    private void invoke(BoatSpringTemplatesRun test, Method m) {
        m.invoke(test);
    }

}
