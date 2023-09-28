package com.backbase.oss.codegen.java;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;

@RequiredArgsConstructor
@Slf4j
class VerificationRunner {

    private final ClassLoader classLoader;

    public void runVerification(Verification verification) throws InterruptedException {

        log.info("Running verification '{}'", verification.displayName);

        var thread = new ExceptionInterceptingThread(
            verification.runnable,
            "verify-classes-" + verification.displayName,
            classLoader
        );
        thread.start();
        thread.join();

        log.info("Verification '{}' completed", verification.displayName);

        thread.getUncaughtExceptions().stream()
            .findFirst()
            .ifPresent(Assertions::fail);
    }

    @Builder
    public static class Verification {
        private String displayName;
        private Runnable runnable;
    }
}
