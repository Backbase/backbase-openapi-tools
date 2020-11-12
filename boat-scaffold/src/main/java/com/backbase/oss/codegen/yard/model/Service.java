package com.backbase.oss.codegen.yard.model;

import java.util.List;
import lombok.Data;

@Data
public class Service {

    private String key;
    private String title;
    private String content;
    private String version;

    private List<Spec> specs;

}
