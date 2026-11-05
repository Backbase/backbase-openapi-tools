package com.backbase.oss.boat.asyncapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import lombok.Value;

/**
 * Follows JSON Reference ({@code $ref}) chains across the root AsyncAPI document and external files.
 *
 * <p>Only structural AsyncAPI references (channels, operations, messages, parameters, traits, replies) are
 * followed here. Schema references are left for the OpenAPI bundler, see {@link AsyncApiOpenApiProjector}.
 */
public class RefResolver {

    static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private static final String REF = "$ref";
    private static final int MAX_HOPS = 20;

    private final URI rootUri;
    private final Map<URI, JsonNode> documents = new HashMap<>();

    public RefResolver(URI rootUri, JsonNode root) {
        this.rootUri = withoutFragment(rootUri);
        documents.put(this.rootUri, root);
    }

    public URI getRootUri() {
        return rootUri;
    }

    public static JsonNode read(URI uri) throws IOException {
        try (InputStream in = uri.toURL().openStream()) {
            return YAML_MAPPER.readTree(in);
        }
    }

    public boolean isRoot(URI uri) {
        return rootUri.equals(withoutFragment(uri));
    }

    /**
     * Follows {@code node} while it is a reference object.
     */
    public Target resolve(JsonNode node, URI baseUri) throws AsyncApiLoaderException {
        Target target = new Target(node, baseUri, null, null);
        int hops = 0;
        while (isRef(target.getNode())) {
            if (hops++ > MAX_HOPS) {
                throw new AsyncApiLoaderException("Did not resolve " + node.get(REF).asText() + " after "
                    + MAX_HOPS + " references");
            }
            target = resolveRef(target.getNode().get(REF).asText(), target.getBaseUri());
        }
        return target;
    }

    public Target resolveRef(String ref, URI baseUri) throws AsyncApiLoaderException {
        int hash = ref.indexOf('#');
        String filePart = hash >= 0 ? ref.substring(0, hash) : ref;
        String fragment = hash >= 0 ? decode(ref.substring(hash + 1)) : "";
        URI docUri = filePart.isEmpty() ? withoutFragment(baseUri) : withoutFragment(baseUri.resolve(filePart));
        JsonNode document = document(docUri);
        JsonNode node = fragment.isEmpty() ? document : document.at(fragment);
        if (node == null || node.isMissingNode()) {
            throw new AsyncApiLoaderException("Cannot resolve $ref '" + ref + "' from " + baseUri);
        }
        String name = fragment.isEmpty() ? baseName(docUri) : lastSegment(fragment);
        return new Target(node, docUri, fragment, name);
    }

    /**
     * Returns a copy of {@code schema} in which every {@code $ref} is made relative to the root document, so that
     * a schema found in an external file can be embedded in the root document.
     */
    public JsonNode rebase(JsonNode schema, URI fromBase) {
        if (schema == null || isRoot(fromBase)) {
            return schema;
        }
        JsonNode copy = schema.deepCopy();
        rebaseRefs(copy, withoutFragment(fromBase));
        return copy;
    }

    public static boolean isRef(JsonNode node) {
        return node != null && node.isObject() && node.has(REF);
    }

    public static String lastSegment(String pointer) {
        String segment = pointer.substring(pointer.lastIndexOf('/') + 1);
        return segment.replace("~1", "/").replace("~0", "~");
    }

    private void rebaseRefs(JsonNode node, URI fromBase) {
        if (node instanceof ObjectNode) {
            ObjectNode object = (ObjectNode) node;
            JsonNode ref = object.get(REF);
            if (ref != null && ref.isTextual()) {
                object.set(REF, TextNode.valueOf(rebaseRef(ref.asText(), fromBase)));
            }
            Iterator<JsonNode> values = object.elements();
            while (values.hasNext()) {
                rebaseRefs(values.next(), fromBase);
            }
        } else if (node instanceof ArrayNode) {
            node.forEach(child -> rebaseRefs(child, fromBase));
        }
    }

    private String rebaseRef(String ref, URI fromBase) {
        int hash = ref.indexOf('#');
        String filePart = hash >= 0 ? ref.substring(0, hash) : ref;
        String fragment = hash >= 0 ? ref.substring(hash) : "";
        URI target = filePart.isEmpty() ? fromBase : fromBase.resolve(filePart);
        if (isRoot(target)) {
            return fragment.isEmpty() ? "#" : fragment;
        }
        return relativeToRoot(target) + fragment;
    }

    private String relativeToRoot(URI target) {
        if ("file".equals(target.getScheme()) && "file".equals(rootUri.getScheme())) {
            Path rootDir = Paths.get(rootUri).getParent();
            return rootDir.relativize(Paths.get(target)).toString().replace('\\', '/');
        }
        return target.toString();
    }

    private JsonNode document(URI uri) throws AsyncApiLoaderException {
        JsonNode document = documents.get(uri);
        if (document == null) {
            try {
                document = read(uri);
            } catch (IOException e) {
                throw new AsyncApiLoaderException("Cannot read referenced document " + uri, e);
            }
            documents.put(uri, document);
        }
        return document;
    }

    private static URI withoutFragment(URI uri) {
        if (uri.getFragment() == null) {
            return uri.normalize();
        }
        try {
            return new URI(uri.getScheme(), uri.getSchemeSpecificPart(), null).normalize();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI " + uri, e);
        }
    }

    private static String baseName(URI uri) {
        String path = uri.getPath();
        String file = path.substring(path.lastIndexOf('/') + 1);
        int dot = file.lastIndexOf('.');
        return dot > 0 ? file.substring(0, dot) : file;
    }

    private static String decode(String fragment) {
        try {
            return URLDecoder.decode(fragment.replace("+", "%2B"), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The node a reference points to, the document it lives in, its JSON pointer and the name of the last
     * pointer segment (or the file base name for whole-document references).
     */
    @Value
    public static class Target {
        JsonNode node;
        URI baseUri;
        String pointer;
        String name;
    }
}
