package com.backbase.oss.boat.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import javax.annotation.Nullable;
import org.raml.v2.api.loader.DefaultResourceLoader;
import org.raml.v2.api.loader.ResourceLoader;
import org.raml.v2.api.loader.ResourceLoaderExtended;
import org.raml.v2.api.loader.ResourceUriCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches a resource using the following order.
 * <ul>
 * <li>If the name starts with '/' it is interpreted as an absolute path</li>
 * <li>Else, it is assumed it is path relative to the root</li>
 * <li>Otherwise, a DefaultResourceLoader is used to fall back on the default resource loading
 * behaviour.</li>
 * </ul>
 */
public class RamlResourceLoader implements ResourceLoaderExtended {

    private final Logger log = LoggerFactory.getLogger(RamlResourceLoader.class);

    private final ResourceLoader fallBackResourceLoader = new DefaultResourceLoader();
    private final File baseDir;
    private final File root;

    /**
     * Constructor specifying where to look for resources.
     * @param baseDir Base Directory to search references from
     * @param root The raml file to load
     */
    public RamlResourceLoader(File baseDir, File root) {
        this.root = root;
        this.baseDir = baseDir;
    }

    @Nullable
    @Override
    public InputStream fetchResource(String resourceName, ResourceUriCallback callback) {
        log.debug("Fetching resource: {}", resourceName);
        File file = new File(resourceName);
        if (!file.isAbsolute()) {
            file = new File(this.root, resourceName);
            if (!file.exists()) {
                file = new File(this.baseDir, resourceName);
            }
        }
        if (!file.exists()) {
            log.debug("File {} does not seem to exist, falling back to default resource loader.",
                file.getAbsolutePath());
            if (fallBackResourceLoader instanceof ResourceLoaderExtended && callback != null) {
                return ((ResourceLoaderExtended) fallBackResourceLoader).fetchResource(resourceName, callback);
            } else {
                return this.fallBackResourceLoader.fetchResource(resourceName);
            }

        }
        if (file.isDirectory()) {
            throw new RamlLoaderException("Cannot read " + file + " (Is a directory)");
        }
        try {
            log.debug("Returning {}", file.getAbsolutePath());
            if (callback != null) {
                callback.onResourceFound(file.toURI());
            }
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RamlLoaderException("File not found: " + file, e);
        }
    }

    @Override
    public InputStream fetchResource(String resourceName) {
        return fetchResource(resourceName, null);
    }

    public URI getUriCallBackParam() {
        return this.fallBackResourceLoader instanceof ResourceLoaderExtended ? ((ResourceLoaderExtended)this.fallBackResourceLoader).getUriCallBackParam() : null;
    }
}
