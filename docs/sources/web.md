# Web pages and direct downloads

Available in **1.4.0 and later**.

The **1.4.1 preview** also recognizes the official `https://geysermc.org/download?project=geyser` and `https://geysermc.org/download?project=floodgate` pages (with or without the trailing slash before `?`). These resolve directly to the corresponding latest Spigot download endpoint. They remain web inspection downloads: finding a file alone does not establish that its version is newer. Existing checksum and JAR checks still apply.

Manual entries still need only the exact installed JAR filename and an HTTPS source URL. For example:

```yaml
updates:
  - jar: "Geyser-Spigot.jar"
    source: "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot"
  - jar: "floodgate-spigot.jar"
    source: "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot"
  - jar: "EssentialsX-2.22.1-dev+27-e70bdb8.jar"
    source: "https://ci.ender.zone/job/EssentialsX/lastSuccessfulBuild/artifact/jars/EssentialsX-2.22.1-dev+27-e70bdb8.jar"
```

Replace each `jar` value with the actual installed filename. Keep one `updates:` list, save and run `/pu reload`. Explicit entries take precedence over older disabled legacy settings for that plugin.

The Geyser/Floodgate endpoints follow the publisher's latest build and do not need a filename extension. The EssentialsX example names one particular artifact: if the next successful build changes that filename, update the URL or supply its artifact listing page. A fixed file URL cannot discover a differently named future file automatically.

## Page handling

A public HTTPS wiki/project page can be used instead. The plugin reads normal HTML links without executing JavaScript. It prefers recognized Modrinth, GitHub or Spigot project links, then direct JAR links, then download-labelled landing pages/endpoints. Relative links are resolved against the final page URL after redirects.

Discovery is bounded: at most three pages, three linked provider requests, 512 links per page and 2 MiB per page. Within the same priority, the first usable link in page order is chosen. It does not guess which of several web files is newest or correct for your server. If a page needs scripts, login or a special interaction, use a direct download URL. An expired URL, wrong plugin, HTML response or invalid JAR fails with an error; it is not installed.

For direct files without release metadata, reports show **UNKNOWN**: a download is available, but version order and Minecraft compatibility have not been verified. Downloading is still an explicit admin command. After validation, the JAR descriptor's actual version is shown. Development/snapshot, alpha, beta, prerelease and unknown types carry warnings. Provider-backed links retain their provider's release metadata and checksum when available.

## Integrity policy and filenames

An unknown web version does not trigger an update notification. Its button is labelled **Download for inspection**. When the installed original JAR can be matched, an identical download is rejected. Numerically older files are rejected as well. A changed snapshot with an unchanged version label may be saved for inspection, but the completion message explicitly says that a newer release has not been established.

```yaml
downloads:
  require-checksum: true
  allow-unverified-web: true
```

Explicit web entries allow a missing checksum by default, with warnings before and after download. Set `allow-unverified-web: false` to apply the standard checksum requirement to them too. Existing GitHub/Spigot/Modrinth entries retain their current policy. Any supplied mismatched checksum rejects the file even when missing checksums are allowed.

HTTPS, public-address checks on each connection, redirects, size/time limits, expected plugin identity and archive validation remain enforced. These checks do not prove publisher authenticity or runtime compatibility. Arbitrary website metadata is not automatically adopted as a web source.

A safe filename at the end of the URL is preserved, including spaces, `+` and build identifiers. Extensionless downloads use `<PluginName>.jar`. Downloads are saved under PluginUpdateWatch's `downloads` folder; replace the installed JAR while the server is stopped. No download is installed or executed automatically.
