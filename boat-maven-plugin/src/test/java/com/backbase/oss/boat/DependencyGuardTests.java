package com.backbase.oss.boat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class DependencyGuardTests {

    @Test
    void shouldExcludeApacheHttpClientFromOpenApiDiffCoreDependency() throws Exception {
        Document pom = readPom(Path.of("pom.xml"));
        XPath xpath = XPathFactory.newInstance().newXPath();

        String dependencySelector =
            "/project/dependencies/dependency[groupId='org.openapitools.openapidiff' and artifactId='openapi-diff-core']";
        Node dependencyNode = (Node) xpath.evaluate(dependencySelector, pom, XPathConstants.NODE);
        assertNotNull(dependencyNode, "openapi-diff-core dependency must exist in boat-maven-plugin/pom.xml");

        NodeList excludedArtifacts = (NodeList) xpath.evaluate(
            dependencySelector + "/exclusions/exclusion[groupId='org.apache.httpcomponents']/artifactId",
            pom,
            XPathConstants.NODESET
        );

        Set<String> exclusions = new HashSet<>();
        for (int i = 0; i < excludedArtifacts.getLength(); i++) {
            exclusions.add(excludedArtifacts.item(i).getTextContent().trim());
        }

        assertEquals(2, exclusions.size(), "Exactly two Apache HttpComponents exclusions are expected");
        assertTrue(exclusions.contains("httpclient"), "httpclient must be excluded to avoid ClassRealm conflicts");
        assertTrue(exclusions.contains("httpcore"), "httpcore must be excluded to avoid ClassRealm conflicts");
    }

    private static Document readPom(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(path.toFile());
    }
}
