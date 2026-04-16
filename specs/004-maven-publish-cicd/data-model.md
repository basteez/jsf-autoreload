# Data Model: Maven Central Publishing & CI/CD Pipeline

**Feature**: 004-maven-publish-cicd  
**Date**: 2026-04-16

## Entities

### Publishable Module

A Maven module whose artifacts are deployed to Maven Central.

| Field | Type | Description |
|-------|------|-------------|
| artifactId | string | Module identifier (e.g., `jsf-autoreload-core`) |
| packaging | string | `jar` or `maven-plugin` |
| publishable | boolean | Whether the module is included in the release bundle |

**Instances**:
- `jsf-autoreload-core` — packaging: `jar`, publishable: `true`
- `jsf-autoreload-tomcat` — packaging: `jar`, publishable: `true`
- `jsf-autoreload-maven-plugin` — packaging: `maven-plugin`, publishable: `true`
- `jsf-autoreload-integration-tests` — packaging: `jar`, publishable: `false` (deploy skip already configured)

**Validation rules**:
- Non-publishable modules must have `<maven-deploy-plugin><skip>true</skip>` AND `<maven-javadoc-plugin><skip>true</skip>` AND `<maven-gpg-plugin><skip>true</skip>` (or be excluded from the release profile)

---

### Release Artifact Set

For each publishable module, the complete set of files required by Maven Central.

| Artifact | Pattern | Required |
|----------|---------|----------|
| Main JAR | `{artifactId}-{version}.jar` | Yes |
| POM | `{artifactId}-{version}.pom` | Yes |
| Sources JAR | `{artifactId}-{version}-sources.jar` | Yes |
| Javadoc JAR | `{artifactId}-{version}-javadoc.jar` | Yes |
| PGP signatures | `*.asc` for each artifact above | Yes |
| MD5 checksums | `*.md5` for each artifact | Auto-generated |
| SHA1 checksums | `*.sha1` for each artifact | Auto-generated |

---

### Version Tag

A Git tag that triggers the automated release pipeline.

| Field | Type | Description |
|-------|------|-------------|
| pattern | regex | `v[0-9]+\.[0-9]+\.[0-9]+` |
| version | string | Extracted from tag (strip leading `v`) |
| example | string | `v1.0.0` → version `1.0.0` |

**State transitions**:
1. `X.Y.Z-SNAPSHOT` (development) → `X.Y.Z` (release tag pushed) → `X.Y.(Z+1)-SNAPSHOT` (post-release bump)

---

### CI Secrets

Credentials required by the CI/CD pipeline, stored as GitHub Actions secrets.

| Secret Name | Content | Used By |
|-------------|---------|---------|
| `GPG_PRIVATE_KEY` | Base64-encoded ASCII-armored GPG private key | GPG import step |
| `GPG_PASSPHRASE` | GPG key passphrase | `maven-gpg-plugin` |
| `CENTRAL_USERNAME` | Sonatype Central Portal user token username | `central-publishing-maven-plugin` |
| `CENTRAL_PASSWORD` | Sonatype Central Portal user token password | `central-publishing-maven-plugin` |
| `RELEASE_TOKEN` | GitHub PAT with `contents: write` for post-release version bump push | Git push step |

---

### Maven Profile: `release`

Build profile activated only during release. Contains signing, source/javadoc generation, and Central Portal deployment plugins.

| Plugin | Version | Purpose |
|--------|---------|---------|
| `maven-source-plugin` | 3.3.1 | Generate sources JAR |
| `maven-javadoc-plugin` | 3.11.2 | Generate javadoc JAR |
| `maven-gpg-plugin` | 3.2.7 | Sign all artifacts with PGP |
| `central-publishing-maven-plugin` | 0.10.0 | Deploy to Maven Central |

---

### GitHub Actions Workflows

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `ci.yml` | PR to main, push to main | Build + test on JDK 11 & 21 matrix |
| `release.yml` | Push tag `v*.*.*` | Build, sign, deploy to Central, bump version |
