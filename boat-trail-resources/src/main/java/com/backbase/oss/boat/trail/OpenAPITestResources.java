package com.backbase.oss.boat.trail;

import java.io.File;
import java.net.URL;

public class OpenAPITestResources {

    public static File getOpenAPIFile(String name) {

        URL resource = OpenAPITestResources.class.getResource(name);
        assert resource != null;
        return new File(resource.getFile());
    }

}
