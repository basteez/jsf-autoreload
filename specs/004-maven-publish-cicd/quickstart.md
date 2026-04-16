# Quickstart: Maven Central Publishing & CI/CD

**Feature**: 004-maven-publish-cicd

## Prerequisites (one-time setup by maintainer)

1. **Sonatype Central Portal account**: Register at [central.sonatype.com](https://central.sonatype.com) and claim the `it.bstz` namespace (verify domain ownership for `bstz.it`, or fall back to `io.github.basteez`).

2. **Generate a Portal user token**: In the Central Portal UI, go to Account → User Token → Generate. Save the username and password.

3. **Generate a GPG key**:
   ```bash
   gpg --full-generate-key   # RSA 4096, no expiry recommended for CI
   gpg --keyserver keys.openpgp.org --send-keys <KEY_ID>
   ```

4. **Export GPG key for CI**:
   ```bash
   gpg --armor --export-secret-keys <KEY_ID> | base64 > gpg-key-base64.txt
   ```

5. **Configure GitHub Secrets** (Settings → Secrets → Actions):
   | Secret | Value |
   |--------|-------|
   | `GPG_PRIVATE_KEY` | Contents of `gpg-key-base64.txt` |
   | `GPG_PASSPHRASE` | GPG key passphrase |
   | `CENTRAL_USERNAME` | Portal user token username |
   | `CENTRAL_PASSWORD` | Portal user token password |
   | `RELEASE_TOKEN` | GitHub PAT with `contents: write` scope |

## Workflow: Making a Release

1. **Ensure main is ready**: All tests pass, version is `X.Y.Z-SNAPSHOT`.

2. **Tag the release**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

3. **CI handles everything**:
   - Sets version to `1.0.0` (strips SNAPSHOT)
   - Builds all modules
   - Generates sources + javadoc JARs
   - Signs all artifacts with GPG
   - Bundles and deploys to Maven Central
   - Bumps version to `1.0.1-SNAPSHOT` and pushes to main

4. **Verify on Maven Central**: Artifacts appear at `https://central.sonatype.com/artifact/it.bstz/jsf-autoreload-core`.

## Workflow: CI on Pull Requests

Automatic. Opening a PR targeting `main` triggers:
- Build + test on JDK 11 and JDK 21
- Results reported on the PR checks

## Local Development

No changes to local development workflow. The `release` profile is only activated with `-P release` and is not needed for local builds.

To verify release artifacts locally (dry-run):
```bash
mvn -B -P release verify -DskipTests
# Check target/ directories for -sources.jar, -javadoc.jar, .asc files
```
