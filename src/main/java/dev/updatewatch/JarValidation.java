package dev.updatewatch;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.CRC32;
import org.bukkit.configuration.file.YamlConfiguration;

final class JarValidation {
    static void validate(Path path, String expectedName) throws Exception {
        try (JarFile jar = new JarFile(path.toFile())) {
            Set<String> names = new HashSet<>();
            long expanded = 0;
            var entries = jar.entries();
            byte[] buffer = new byte[8192];
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement(); String name = entry.getName();
                if (!names.add(name) || names.size() > 100000 || name.startsWith("/") || name.contains("\\")
                        || name.contains(":") || Arrays.asList(name.split("/")).contains(".."))
                    throw new IOException("Unsafe or duplicate JAR entry");
                CRC32 crc = new CRC32(); long size = 0;
                try (var in = jar.getInputStream(entry)) {
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Validation cancelled");
                        expanded += n; size += n;
                        if (expanded > 1024L * 1024 * 1024 || size > 256L * 1024 * 1024)
                            throw new IOException("JAR expanded-size limit exceeded");
                        crc.update(buffer, 0, n);
                    }
                }
                if (entry.getCrc() != crc.getValue() || entry.getSize() != size) throw new IOException("Corrupt JAR entry");
            }
            boolean found = false;
            for (String descriptor : List.of("paper-plugin.yml", "plugin.yml")) {
                var entry = jar.getJarEntry(descriptor);
                if (entry == null) continue;
                found = true;
                try (var in = jar.getInputStream(entry)) {
                    byte[] bytes = in.readNBytes(65537);
                    if (bytes.length > 65536) throw new IOException("Plugin descriptor too large");
                    var yaml = new YamlConfiguration(); yaml.loadFromString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
                    if (!expectedName.equalsIgnoreCase(yaml.getString("name", ""))) throw new IOException("Downloaded JAR belongs to a different plugin");
                    if (yaml.getString("version", "").isBlank()) throw new IOException("Plugin version missing");
                    String main = yaml.getString("main", "");
                    if (!main.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) throw new IOException("Invalid plugin main class");
                    var mainEntry = jar.getJarEntry(main.replace('.', '/') + ".class");
                    if (mainEntry == null) throw new IOException("Plugin main class absent from JAR");
                    try (var data = new DataInputStream(jar.getInputStream(mainEntry))) {
                        if (mainEntry.getSize() < 10 || data.readInt() != 0xCAFEBABE) throw new IOException("Invalid main class file");
                    }
                }
            }
            if (!found) throw new IOException("Downloaded file is not a Paper/Bukkit plugin JAR");
        }
    }
}
