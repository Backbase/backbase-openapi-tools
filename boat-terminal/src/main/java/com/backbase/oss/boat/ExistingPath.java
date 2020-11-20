package com.backbase.oss.boat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

public class ExistingPath implements ITypeConverter<Path> {

    @Override
    public Path convert(String value) throws Exception {
        final Path path = Paths.get(value);

        if (!Files.exists(path)) {
            throw new TypeConversionException("The path " + value + " doesn't exist.");
        }

        return path;
    }
}


