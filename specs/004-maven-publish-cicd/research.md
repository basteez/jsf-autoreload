# Research: Maven Central Publishing & CI/CD Pipeline

**Feature**: 004-maven-publish-cicd  
**Date**: 2026-04-16

## R-001: Sonatype Central Portal Deployment Mechanics

**Decision**: Use the **Central Portal** (central.sonatype.com) exclusively.

**Rationale**: Legacy OSSRH (oss.sonatype.org) was shut down on June 30, 2025. All namespaces were auto-migrated to the Central Portal. There is no legacy path remaining. The project must register at central.sonatype.com and claim the `it.bstz` namespace (or fall back to `io.github.basteez` if domain verification is not possible).

**Alternatives considered**:
- Legacy OSSRH / `nexus-staging-maven-plugin` — no longer available
- JitPack — doesn't meet the "standard Maven Central coordinates" requirement

---

## R-002: Central Publishing Maven Plugin

**Decision**: Use `org.sonatype.central:central-publishing-maven-plugin:0.10.0`.

**Rationale**: This is the only supported plugin for the new Central Portal. It replaces the legacy `nexus-staging-maven-plugin` entirely. Key features:
- `<extensions>true</extensions>` — overrides the default deploy mechanism
- `autoPublish=true` — automatically publishes after validation passes
- `waitUntil=published` — blocks until artifacts are available on Central
- Natively bundles all modules into a single `central-bundle.zip` (atomic deployment)
- Auto-generates MD5/SHA1/SHA-256/SHA-512 checksums

**Configuration**:
```xml
<plugin>
  <groupId>org.sonatype.central</groupId>
  <artifactId>central-publishing-maven-plugin</artifactId>
  <version>0.10.0</version>
  <extensions>true</extensions>
  <configuration>
    <publishingServerId>central</publishingServerId>
    <autoPublish>true</autoPublish>
    <waitUntil>published</waitUntil>
  </configuration>
</plugin>
```

Credentials are stored in `~/.m2/settings.xml` under `<server id="central">` using a Portal user token.

**Alternatives considered**:
- `maven-deploy-plugin` + manual staging — doesn't support the new Central Portal
- `nexus-staging-maven-plugin` — legacy, no longer maintained for the new portal

---

## R-003: Multi-Module Atomic Deployment

**Decision**: Rely on `central-publishing-maven-plugin`'s native bundling.

**Rationale**: The plugin stages artifacts from every module into a single directory, zips them into one `central-bundle.zip`, and uploads a single deployment unit. Either all modules validate and publish, or none do. No additional tooling or configuration needed for atomicity.

The `integration-tests` module already has `<maven-deploy-plugin><skip>true</skip>` which will exclude it from the bundle.

**Alternatives considered**:
- Manual multi-step deploy with staging profile — unnecessary complexity
- Separate per-module deploys — violates atomicity requirement (FR-014)

---

## R-004: GPG Signing in GitHub Actions

**Decision**: Manual GPG key import with `maven-gpg-plugin:3.2.7`.

**Rationale**: The manual 3-line approach (decode + import) is simpler than third-party actions, has zero supply-chain risk, and is what Sonatype's official docs recommend.

**Secrets required**:
- `GPG_PRIVATE_KEY` — base64-encoded ASCII-armored private key
- `GPG_PASSPHRASE` — key passphrase

**CI import step**:
```yaml
- name: Import GPG key
  run: echo "${{ secrets.GPG_PRIVATE_KEY }}" | base64 --decode | gpg --batch --import
```

**Plugin configuration** (in a `release` profile to avoid requiring GPG for local builds):
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-gpg-plugin</artifactId>
  <version>3.2.7</version>
  <executions>
    <execution>
      <id>sign-artifacts</id>
      <phase>verify</phase>
      <goals><goal>sign</goal></goals>
      <configuration>
        <gpgArguments>
          <arg>--pinentry-mode</arg>
          <arg>loopback</arg>
        </gpgArguments>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Passphrase is passed via `MAVEN_GPG_PASSPHRASE` environment variable.

**Alternatives considered**:
- `crazy-max/ghaction-import-gpg` — adds third-party dependency risk
- Signing via Sigstore/cosign — not yet supported by Maven Central

**Key pitfalls to avoid**:
- Must use `--batch` on import, `--pinentry-mode loopback` on sign
- Wrap signing in a Maven profile (`release`) so local builds don't require GPG
- CI GPG keys should have no expiry or be rotated before expiry

---

## R-005: Post-Release Version Bump Automation

**Decision**: Use `mvn versions:set` to bump to next SNAPSHOT, commit with `[skip ci]`, push directly to main.

**Rationale**: `versions:set` handles multi-module projects correctly — updates parent POM, all child modules, and cross-module dependency references atomically. `sed` is fragile with XML and multi-module `<version>` tags.

**Version calculation**: Parse the tag (`v1.0.0` → `1.0.0`), increment patch, append `-SNAPSHOT` → `1.0.1-SNAPSHOT`.

**Push strategy**: Use a GitHub PAT or GitHub App installation token with `contents: write` permission. For simplicity and since this is a personal project, a PAT stored as a secret is sufficient. The commit message includes `[skip ci]` to prevent CI loops.

**Alternatives considered**:
- Open a PR for the version bump — safer for branch protection but adds friction for a single-maintainer project
- GitHub App token — better for org/team projects, overkill for a personal repo
- `sed` for version replacement — fragile, doesn't handle inter-module references

---

## R-006: POM Metadata Gap Analysis

**Decision**: Add `organization` and `organizationUrl` to `<developer>` in the parent POM.

**Rationale**: The Central Portal requires each `<developer>` entry to include `name`, `email`, `organization`, and `organizationUrl`. The current parent POM has `name` and `email` but is missing `organization` and `organizationUrl`.

All other required metadata (groupId, artifactId, version, name, description, url, licenses, scm) is already present in the parent POM and inherited by child modules.

---

## R-007: Required Artifacts Per Module

**Decision**: Configure `maven-source-plugin` and `maven-javadoc-plugin` in the `release` profile.

**Rationale**: Each publishable module must produce:
- Main JAR (default)
- Sources JAR (`maven-source-plugin`)
- Javadoc JAR (`maven-javadoc-plugin`)
- PGP `.asc` signatures for each file including POM (`maven-gpg-plugin`)
- Checksums (auto-generated by `central-publishing-maven-plugin`)

These plugins should be in a `release` profile to avoid slowing down local development builds.
