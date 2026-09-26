package dev.updatewatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.jar.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class DownloadValidationTest {
    @TempDir Path temp;
    Path jar(String descriptor, String contents) throws Exception {
        Path p = temp.resolve("test.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(p))) {
            out.putNextEntry(new JarEntry(descriptor));
            out.write(contents.getBytes(StandardCharsets.UTF_8)); out.closeEntry();
            out.putNextEntry(new JarEntry("example/Main.class"));
            try (var in = DownloadValidationTest.class.getResourceAsStream("/dev/updatewatch/DownloadValidationTest.class")) { in.transferTo(out); }
            out.closeEntry();
        }
        return p;
    }
    @Test void acceptsMatchingBukkitJar() throws Exception {
        Remote.validate(jar("plugin.yml", "name: Example\nmain: example.Main\nversion: 2.0\n"), "Example");
    }
    @Test void acceptsMatchingPaperJar() throws Exception {
        Remote.validate(jar("paper-plugin.yml", "name: Example\nmain: example.Main\nversion: 2.0\n"), "Example");
    }
    @Test void rejectsWrongPlugin() throws Exception {
        Path p = jar("plugin.yml", "name: Wrong\nmain: wrong.Main\n");
        assertThrows(Exception.class, () -> Remote.validate(p, "Example"));
    }
    @Test void rejectsNonPluginJar() throws Exception {
        Path p = jar("hello.txt", "hello");
        assertThrows(Exception.class, () -> Remote.validate(p, "Example"));
    }
    @Test void rejectsHtmlResponse() throws Exception {
        Path p = temp.resolve("page.jar"); Files.writeString(p, "<html>login</html>");
        assertThrows(Exception.class, () -> Remote.validate(p, "Example"));
    }
}
