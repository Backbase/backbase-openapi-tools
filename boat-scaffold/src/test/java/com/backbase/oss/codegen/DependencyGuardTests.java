package com.backbase.oss.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

class DependencyGuardTests {

    @Test
    void shouldKeepResolverHttpTransportAsTestScope() throws Exception {
        Document pom = readPom(Path.of("pom.xml"));
        XPath xpath = XPathFactory.newInstance().newXPath();

        String dependencySelector =
            "/project/dependencies/dependency[groupId='org.apache.maven.resolver' and artifactId='maven-resolver-transport-http']";

        Node dependencyNode = (Node) xpath.evaluate(dependencySelector, pom, XPathConstants.NODE);
        assertNotNull(dependencyNode, "maven-resolver-transport-http dependency must exist in boat-scaffold/pom.xml");

        String scope = xpath.evaluate(dependencySelector + "/scope/text()", pom);
        assertEquals("test", scope.trim(), "maven-resolver-transport-http must remain test-scoped");
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
