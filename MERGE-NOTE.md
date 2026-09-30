# Merge Note for PR #5: Add bStats Usage Reporting

## Summary
This PR adds optional bStats usage metrics (plugin ID 34400) with comprehensive opt-out controls and clear admin diagnostics. The integration is production-ready for the 1.3.3 snapshot.

## Live-test findings
The server console shows HTTP 429 warnings from multiple unrelated plugins (FancyHolograms, ChunkyBorder, GravesX, etc.), all hitting bStats at the same time. This is a shared bStats rate-limiting event, not a PluginUpdateWatch config issue. Our plugin's metrics client initializes normally and respects local/global opt-outs.

## What's included
- Bundled and relocated bStats 3.2.1 (no external JAR required)
- Local opt-out: `metrics.enabled: false` in config + `/pu reload`
- Global opt-out respected: shared `plugins/bStats/config.yml`
- Clear startup logging and `/pu stats` diagnostics
- Six new test cases covering enable/disable/reload/failure scenarios
- Full documentation with public dashboard/API access and reporting delays
- Release notes updated; Spigot listing wording change required before 1.3.3 stable publish

## Default behavior
- Enabled by default, including for older configs without a `metrics` section
- Existing configs are NOT rewritten; admins can add the section manually to opt out
- Initialization failures are logged without disrupting update checks

## No blockers
All functionality tested locally and live on Paper 1.21.11 and 26.3. Safe to merge as snapshot; ready for stable release after Spigot listing update.
