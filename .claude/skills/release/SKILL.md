---
name: release
description: Release a vLabeler version — verify version and branch, tag via tools/release.sh, monitor CI builds, write release notes, notify related issues, and update the Trello board. Use when the user asks to release, tag, or publish a version.
---

# Release a vLabeler version

Release `$ARGUMENTS` (a version like `1.7.0-beta1` or `1.7.0`). If no version is given, use
`app.version` from `gradle.properties` and confirm it with the user.

A version is **stable** if it has no `-betaN` suffix; otherwise it is a **beta**.

## 0. Pre-flight checks

Stop and report if any of these fail:

1. Working tree is clean, and `dev` is synced with `origin/dev`.
2. `app.version` in `gradle.properties` equals the version being released and is a valid version
   (`X.Y.Z` or `X.Y.Z-betaN`). The tag must equal `app.version` exactly — every release workflow
   validates this and fails otherwise.
3. The tag does not already exist (`git tag -l <version>`, `gh release view <version>`).
4. The latest commit on the release branch passed CI (`gh run list --branch dev --limit 5`).
5. `./gradlew build test` passes locally (this includes `checkLicenseReportUpdate`; if it fails,
   run `./gradlew updateLicenseReport` and commit).
6. No open PRs targeting `dev` that the user wants included. List them and confirm.

## 1. Choose the release branch

- **Beta**: release from `dev`. No docs-site action is needed: `website/docs` on `dev` is always
  the documentation of the latest beta and its version label follows `app.version`
  automatically. Betas are never snapshotted — only the latest beta is documented.
- **Stable**: first snapshot the docs site version on `dev` (see below), then merge `dev` into
  `main` (regular merge, no squash), push `main`, and release from `main`. Verify CI passes on
  `main` before tagging.

  **Docs version snapshot** (stable releases only), on `dev`:

  1. Snapshot the English docs with the exact version being released (e.g. `1.7.0`):

     ```
     cd website && npm run docusaurus docs:version <version>
     ```

  2. Snapshot the translations too, so the stable version stays translated (untranslated
     versions fall back to English):

     ```
     for l in zh-Hans ja ko; do
       cp -r website/i18n/$l/docusaurus-plugin-content-docs/current \
             website/i18n/$l/docusaurus-plugin-content-docs/version-<version>
       cp website/i18n/$l/docusaurus-plugin-content-docs/current.json \
          website/i18n/$l/docusaurus-plugin-content-docs/version-<version>.json
     done
     ```

  3. If a snapshot of the **same minor line** already exists (e.g. `1.7.0` when releasing
     `1.7.1`), delete the superseded snapshot so each minor keeps only its newest patch:
     remove its entry from `website/versions.json`, its `website/versioned_docs/version-<old>/`
     and `website/versioned_sidebars/version-<old>-sidebars.json`, and the corresponding
     `version-<old>*` files under each `website/i18n/<locale>/docusaurus-plugin-content-docs/`.
     Snapshots of **older minors stay** — they are the maintained docs of those versions.

  4. Verify with `cd website && npm run build`, then commit everything to `dev`. This makes the
     released version the default on the docs site. See `website/README.md`.

## 2. Tag via tools/release.sh

From the release branch at the release commit:

```
./tools/release.sh <version>
```

The script validates the tag against `app.version` (offering to update and commit it if they
differ — prefer fixing `gradle.properties` beforehand so the script does not need to), then tags
HEAD and pushes the tag. **Confirm with the user before running it** — pushing the tag starts the
release builds and is outward-facing.

## 3. Monitor the release builds

The tag push triggers 3 workflows: `Release Mac (ARM)`, `Release Ubuntu`, `Release Win`. They
create/update the GitHub release for the tag (as prerelease) and attach artifacts. (Intel Mac
builds were dropped after 1.7.0-beta2.)

- Watch with `gh run list --limit 10` / `gh run watch <id>` until all 3 succeed.
- Verify the release has all expected artifacts: `~win64.zip`, `~mac-arm64.dmg`, `~amd64.deb`
  (+ AppImage if produced).
- If a build fails for a transient reason, re-run it (`gh run rerun <id>`); the tag does not need
  to be moved.

## 4. Write the release notes

Generate the base notes in the repository's established format (What's Changed / New Contributors
/ Full Changelog):

```
gh api repos/sdercolin/vlabeler/releases/generate-notes \
  -f tag_name=<version> -f previous_tag_name=<previous tag>
```

Review and improve: group user-facing changes first, mention notable new features in one or two
plain sentences at the top if the list is long. Then update the release notes:

```
gh release edit <version> --notes-file <file>
```

Show the final notes to the user before saving.

**Prerelease flag**: leave the release as **prerelease = true** (the workflows create it that
way, and it must stay so while builds are running). Do NOT flip it — see step 5.

## 5. Wait for the user to release

The release is considered **released** only when the user manually removes the prerelease label
on GitHub, after they have verified the builds. Note:

- The user removes the prerelease label themselves (this is what makes the in-app auto-update
  system pick the version up). Do NOT do it for them.
- **A beta must never be marked as "latest release"** — for betas, only the prerelease label is
  removed. Marking as latest is only for stable versions (also the user's call).

When all builds have succeeded and the notes are applied, tell the user everything is ready and
wait for them to confirm they have removed the prerelease label. Do not proceed to step 6 before
that.

## 6. Notify related issues (do NOT close)

Only after the user has removed the prerelease label (step 5):

Find issues addressed by this release: check merged PRs since the previous tag
(`gh pr list --state merged --base dev --search "merged:>=<previous tag date>"`) and their
"Closes #N" references, plus `git log <previous tag>..<version> --oneline` for `(#N)` mentions.

For each issue, leave a comment like:

> This should be addressed by [<version>](https://github.com/sdercolin/vlabeler/releases/tag/<version>).
> Please verify with the new build and close this issue if it works for you.

**Never close the issues** — the submitter closes them after verifying.

## 7. Update the Trello board

Board: `vLabeler` (https://trello.com/b/rP1L7rbi/vlabeler). Use the connected Trello MCP tools
(or ask the user if not connected).

- Move all cards in **Waiting for release** to **Beta Released** (beta) or **Released** (stable),
  with a comment naming the version.
- For a stable release, also move cards from **Beta Released** to **Released** if their changes
  are included in this stable version.

## 8. Wrap up

- Post a short summary to the user: version, release URL, artifact status, issues notified,
  Trello moves.
- Remind the user of manual follow-ups that are theirs to do:
  - Announce in the Discord server.
  - Optionally bump `app.version` on `dev` to start the next cycle (e.g. `1.7.1-beta1` after a
    stable `1.7.0`), committed directly to `dev`.
- If anything in steps 2–7 failed halfway, report exactly which steps completed so the release
  can be resumed without re-tagging.
