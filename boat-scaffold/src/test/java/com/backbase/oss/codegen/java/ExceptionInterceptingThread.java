package com.backbase.oss.codegen.java;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class ExceptionInterceptingThread extends Thread {

    @Getter
    private final List<Throwable> uncaughtExceptions = new ArrayList<>();

    public ExceptionInterceptingThread(Runnable target, String name, ClassLoader classLoader) {
        super(target, name);
        setContextClassLoader(classLoader);
        setUncaughtExceptionHandler((t, e) -> {
            log.warn("Uncaught exception in classes verifier: ", e);
            uncaughtExceptions.add(e);
        });
    }
}
