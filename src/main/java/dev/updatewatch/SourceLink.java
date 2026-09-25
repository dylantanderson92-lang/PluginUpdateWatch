package dev.updatewatch;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

record SourceLink(String type, String id, String asset) {
    static SourceLink parse(String value) {
        URI uri = URI.create(value.trim());
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443))
            throw new IllegalArgumentException("Use an HTTPS Modrinth, Spigot or GitHub project link");
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String[] parts = uri.getPath().split("/");
        if ((host.equals("modrinth.com") || host.equals("www.modrinth.com")) && parts.length >= 3
                && (parts[1].equals("plugin") || parts[1].equals("mod") || parts[1].equals("project"))
                && parts[2].matches("[A-Za-z0-9_-]+")) return new SourceLink("modrinth", parts[2], ".*\\.jar");
        if ((host.equals("spigotmc.org") || host.equals("www.spigotmc.org")) && parts.length >= 3 && parts[1].equals("resources")) {
            String id = parts[2].substring(parts[2].lastIndexOf('.') + 1);
            if (id.matches("[1-9][0-9]*")) return new SourceLink("spigot", id, ".*\\.jar");
        }
        if (host.equals("github.com") && parts.length >= 3 && parts[1].matches("[A-Za-z0-9_.-]+") && parts[2].matches("[A-Za-z0-9_.-]+")) {
            String repository = parts[2].replaceFirst("\\.git$", "");
            String asset = parts.length >= 7 && parts[3].equals("releases") && parts[4].equals("download")
                    ? Pattern.quote(parts[parts.length - 1]) : ".*\\.jar";
            return new SourceLink("github", parts[1] + "/" + repository, asset);
        }
        throw new IllegalArgumentException("Unsupported source link; use a Modrinth, Spigot or GitHub project page");
    }
    Remote.Source source(String name, String installed, String minecraft) {
        return new Remote.Source(name, installed, type, id, asset, minecraft);
    }
}
