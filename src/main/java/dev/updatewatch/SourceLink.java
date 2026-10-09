package dev.updatewatch;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

record SourceLink(String type, String id, String asset) {
    static SourceLink parse(String value) {
        URI uri = URI.create(value.trim());
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443))
            throw new IllegalArgumentException("Use an HTTPS project page or direct download link");
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        // The official download page renders its platform links in JavaScript.
        // Resolve only its two explicit Bukkit projects; do not crawl its unrelated GitHub footer.
        if ((host.equals("geysermc.org") || host.equals("www.geysermc.org"))
                && uri.getPath().matches("/download/?") && uri.getRawQuery() != null
                && uri.getRawQuery().matches("project=(geyser|floodgate)"))
            return new SourceLink("web", "https://download.geysermc.org/v2/projects/" + uri.getRawQuery().substring(8)
                    + "/versions/latest/builds/latest/downloads/spigot", ".*\\.jar");
        if (java.util.Set.of("modrinth.com", "spigotmc.org", "github.com").stream().anyMatch(h -> host.startsWith(h + ".") || host.startsWith("www." + h + ".")))
            throw new IllegalArgumentException("Provider lookalike hostname rejected");
        boolean known = java.util.Set.of("modrinth.com", "www.modrinth.com", "spigotmc.org", "www.spigotmc.org", "github.com").contains(host);
        if (!known || (host.equals("github.com") && uri.getPath().matches("/[^/]+/[^/]+/wiki(?:/.*)?"))) {
            try { return new SourceLink("web", HttpTransport.webUri(uri.toString().split("#", 2)[0]).toString(), ".*\\.jar"); }
            catch (java.io.IOException e) { throw new IllegalArgumentException("Use a public HTTPS source page or direct download URL", e); }
        }
        // Spigot resource titles commonly contain percent-encoded emoji/punctuation.
        // Permit UTF-8 bytes in the title only, never encoded ASCII separators or route/ID segments.
        String rawPath = uri.getRawPath();
        String[] rawParts = rawPath.split("/");
        boolean encodedSpigotTitle = (host.equals("spigotmc.org") || host.equals("www.spigotmc.org"))
                && rawParts.length == 3 && rawParts[1].equals("resources")
                && !rawParts[2].replaceAll("%[89a-fA-F][0-9a-fA-F]", "").contains("%")
                && !uri.getPath().contains("\uFFFD");
        if ((rawPath.contains("%") && !encodedSpigotTitle) || uri.getPath().contains("\\") || uri.getPath().contains("//")
                || java.util.Arrays.stream(uri.getPath().split("/")).anyMatch(p -> p.equals(".") || p.equals("..")))
            throw new IllegalArgumentException("Source paths must not contain encoded or relative segments");
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
            if (repository.isBlank() || !parts[1].matches("[A-Za-z0-9][A-Za-z0-9-]*")) throw new IllegalArgumentException("Invalid GitHub repository");
            if (parts.length >= 7 && parts[3].equals("releases") && parts[4].equals("download") && !Discovery.validFilename(parts[parts.length - 1]))
                throw new IllegalArgumentException("GitHub asset link must name a valid JAR");
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
