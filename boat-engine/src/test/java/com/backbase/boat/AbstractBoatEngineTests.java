package com.backbase.boat;

import java.io.File;
import java.net.URL;

public class AbstractBoatEngineTests {


    protected File getFile(String name) {
        URL resource = getClass().getResource(name);
        assert resource != null;
        return new File(resource.getFile());
    }
}
