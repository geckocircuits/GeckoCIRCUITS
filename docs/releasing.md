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

Update the version in the `pom.xml` files, and document breaking changes in the tag message.

## Tag

```bash
git tag -a vX.Y.Z -m "vX.Y.Z: summary"
git push origin vX.Y.Z
```

A `v*` tag triggers two workflows:

- `.github/workflows/release.yml` builds the platform packages and creates the GitHub release.
- `.github/workflows/sync-to-upstream.yml` mirrors `main` to `geckocircuits/main_gecko2026`.

## After

```bash
python scripts/generate-release-docs.py   # docs/releases/ pages
mkdocs gh-deploy --force
```

Close the milestone.
