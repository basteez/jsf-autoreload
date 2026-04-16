package it.bstz.jsfautoreload.tomcat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TomcatAdapterTest {

    @Test
    void supports_returnsTrueWhenTomcatOnClasspath() {
        TomcatAdapter adapter = new TomcatAdapter();
        assertTrue(adapter.supports(), "Should detect Tomcat on classpath");
    }

    @Test
    void containerName_returnsApacheTomcat() {
        TomcatAdapter adapter = new TomcatAdapter();
        assertEquals("Apache Tomcat", adapter.containerName());
    }

    @Test
    void priority_returnsDefault() {
        TomcatAdapter adapter = new TomcatAdapter();
        assertEquals(100, adapter.priority());
    }
}
