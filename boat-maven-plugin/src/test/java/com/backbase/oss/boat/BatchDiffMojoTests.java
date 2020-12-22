package com.backbase.oss.boat;

import com.backbase.oss.boat.diff.BatchOpenApiDiff;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.util.HashMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
@SuppressWarnings("java:S2699")
class BatchDiffMojoTests {


    @SneakyThrows
    @Test
    void testBatchDiff() {
        File specDirectory = getFile("/oas-examples");
        File tempSpecDirectory = new File(new File("target"), "oas-examples-diff");
        FileUtils.copyDirectory(specDirectory, tempSpecDirectory);
        HashMap<File, OpenAPI> success = new HashMap<>();

        BatchOpenApiDiff.diff(tempSpecDirectory.toPath(), success, new HashMap<>(), true, true);
        assertFalse(success.isEmpty());
    }


    private File getFile(String fileName) {
        return new File(getClass().getResource(fileName).getFile());
    }
}
