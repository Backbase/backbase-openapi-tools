/*
 * Copyright 2013 (c) MuleSoft, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.raml.v2.internal.utils;

import java.io.File;

/**
 * This utility has been modified by Backbase to enable this to work with windows file paths.
 */
public class ResourcePathUtils {

    private ResourcePathUtils() {
        throw new AssertionError("Private constructor");
    }

    /**
     * Returns the absolute resource location using the basePath basePath and relativePath must have
     * forward slashes(/) as path separators.
     *
     * @param basePathParam     The base path of the relative path
     * @param relativePathParam the relative path
     * @return The Absolute path
     */
    public static String toAbsoluteLocation(String basePathParam, String relativePathParam) {
        // This has been added by Backbase to enable this to work with windows file paths
        // It also enforces the condition mentioned in the original method Javadoc.
        // This shouldn't change anything on a UNIX like system as File.separator is / already
        // The same for URL based path
        // The returned value can be used by 'UNIX' and Windows
        String basePath = basePathParam.replace("\\", "/");
        String relativePath = relativePathParam.replace("\\", "/");

        String result = relativePath;
        if (!isAbsolute(relativePath)) {
            // This is for file based path
            int lastSlash = basePath.lastIndexOf('/');
            if (lastSlash != -1) {
                result = basePath.substring(0, lastSlash + 1) + relativePath;
            }

        }
        if (result.contains("#")) {
            return result.split("#")[0];
        }
        return result;
    }

    public static boolean isAbsolute(String includePath) {
        return includePath.startsWith("http:") || includePath.startsWith("https:") || includePath.startsWith("file:")
            || new File(includePath).isAbsolute();
    }
}
