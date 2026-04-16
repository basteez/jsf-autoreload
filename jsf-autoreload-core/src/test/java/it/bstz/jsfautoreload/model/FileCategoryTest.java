package it.bstz.jsfautoreload.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class FileCategoryTest {

    @ParameterizedTest
    @ValueSource(strings = {".xhtml", ".jspx", ".jsp"})
    void fromExtension_viewFiles_returnsView(String ext) {
        assertEquals(FileCategory.VIEW, FileCategory.fromExtension(ext));
    }

    @ParameterizedTest
    @ValueSource(strings = {".css", ".js", ".png", ".jpg", ".gif", ".svg", ".ico", ".woff", ".woff2"})
    void fromExtension_staticFiles_returnsStatic(String ext) {
        assertEquals(FileCategory.STATIC, FileCategory.fromExtension(ext));
    }

    @Test
    void fromExtension_classFile_returnsClass() {
        assertEquals(FileCategory.CLASS, FileCategory.fromExtension(".class"));
    }

    @Test
    void fromExtension_javaFile_returnsSource() {
        assertEquals(FileCategory.SOURCE, FileCategory.fromExtension(".java"));
    }

    @Test
    void fromExtension_unknownExtension_returnsOther() {
        assertEquals(FileCategory.OTHER, FileCategory.fromExtension(".txt"));
        assertEquals(FileCategory.OTHER, FileCategory.fromExtension(".xml"));
        assertEquals(FileCategory.OTHER, FileCategory.fromExtension(""));
    }

    @Test
    void fromExtension_caseInsensitive() {
        assertEquals(FileCategory.VIEW, FileCategory.fromExtension(".XHTML"));
        assertEquals(FileCategory.STATIC, FileCategory.fromExtension(".CSS"));
        assertEquals(FileCategory.CLASS, FileCategory.fromExtension(".CLASS"));
    }
}
