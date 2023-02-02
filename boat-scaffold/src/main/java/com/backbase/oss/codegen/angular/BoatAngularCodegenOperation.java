package com.backbase.oss.codegen.angular;

import com.backbase.oss.codegen.CodegenException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.openapitools.codegen.CodegenOperation;

@EqualsAndHashCode(callSuper = true)
@Getter
public class BoatAngularCodegenOperation extends CodegenOperation {

    public final String pattern;

    @SuppressWarnings("java:S3011")
    public BoatAngularCodegenOperation(CodegenOperation o) {
        for (Field field : o.getClass().getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                Arrays.stream(this.getClass().getFields()).filter(ff -> ff.getName().equals(field.getName()))
                    .findFirst().ifPresent(p -> {
                        try {
                            if (p.canAccess(this)) {
                                p.set(this, field.get(o));
                            }
                        } catch (IllegalAccessException e) {
                            throw new CodegenException(e);
                        }
                    });
            }
        }
        this.responseHeaders.addAll(o.responseHeaders);
        this.pattern = o.path;

    }
}
