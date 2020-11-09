package com.backbase.oss.codegen;

import com.fasterxml.jackson.databind.util.BeanUtil;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.openapitools.codegen.CodegenOperation;

public class BoatCodegenOperation extends CodegenOperation {


    BoatCodegenOperation(CodegenOperation codegenOperation) throws InvocationTargetException, IllegalAccessException {
        super();
        BeanUtils.copyProperties(this, codegenOperation);
    }
}
