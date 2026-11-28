# Releasing

Versions follow [Semantic Versioning](https://semver.org/). A MAJOR release breaks the REST API or the `.ipes` format, a MINOR release adds features compatibly, and a PATCH release fixes bugs.
Planned scope lives in the [GitHub milestones](https://github.com/tinix84/GeckoCIRCUITS/milestones).

## Before tagging

```bash
mvn -f pom-reactor.xml test        # all modules
xvfb-run -a mvn test -Pregression  # 21 Golden-reference circuits
mvn spotbugs:check
mkdocs build --strict
```

Set `app.version` in `src/modules/gecko-rest-api/src/main/resources/application.properties` (reported by `/api/health`), and add `docs/releases/XYZ0.md` with its `mkdocs.yml` nav entry. The GitHub release is named after the tag, but the package files still carry the `pom.xml` version (`GeckoCIRCUITS-1.0-*.zip`). Document breaking changes in the release notes.

## Tag

```bash
git tag -a vX.Y.Z -m "vX.Y.Z: summary"
git push origin vX.Y.Z
```

A `v*` tag triggers two workflows:

- `.github/workflows/release.yml` builds the platform packages and creates the GitHub release.
- `.github/workflows/sync-to-upstream.yml` opens a PR from `main` to `geckocircuits/main_gecko2026`. It needs the `UPSTREAM_PAT` repository secret: a token that can open PRs on the upstream repository.

## After

```bash
python scripts/generate-release-docs.py   # docs/releases/ pages
mkdocs gh-deploy --force
```

Close the milestone.
