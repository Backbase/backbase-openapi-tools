package com.backbase.oss.codegen.doc;

import java.util.List;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.CodegenResponse;

@Slf4j
@ToString(callSuper = true)
@Data()
public class BoatCodegenResponse extends CodegenResponse {

    private List<BoatExample> examples;


}
