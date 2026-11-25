# vLabeler documentation site

This folder contains the vLabeler user documentation, built with [Docusaurus](https://docusaurus.io/).

## Versioning and deployment

The site uses [Docusaurus versioning](https://docusaurus.io/docs/versioning) and is deployed to GitHub Pages
by `.github/workflows/docs.yml` on every push to `dev` that touches this folder.

- `docs/` is the documentation of the **latest beta**. In the version dropdown it is labeled with the
  exact app version read from `gradle.properties` at build time (e.g. `1.7.0-beta2`).
  Docs changes should be made together with the related feature changes in PRs based on `dev`.
  Betas are never snapshotted — only the latest beta is documented.
- On each **stable release**, before merging `dev` into `main`, snapshot the docs on `dev` with the
  exact version being released:

  ```bash
  cd website
  npm run docusaurus docs:version <X.Y.Z>   # e.g. 1.7.0
  for l in zh-Hans ja ko; do
    cp -r i18n/$l/docusaurus-plugin-content-docs/current i18n/$l/docusaurus-plugin-content-docs/version-<X.Y.Z>
    cp i18n/$l/docusaurus-plugin-content-docs/current.json i18n/$l/docusaurus-plugin-content-docs/version-<X.Y.Z>.json
  done
  ```

  and commit the generated `versioned_docs/` / `versioned_sidebars/` / `versions.json` / `i18n` changes.
  The latest snapshot becomes the default version shown to readers, so the site always opens on the
  documentation of the current stable release, while beta users can switch to the beta version in the
  dropdown.

  When releasing a **patch of an already-snapshotted minor** (e.g. `1.7.1` after `1.7.0`), delete the
  superseded snapshot (its entries in `versions.json`, `versioned_docs/`, `versioned_sidebars/` and
  `i18n/*/docusaurus-plugin-content-docs/`) so each minor keeps only its newest patch. Snapshots of
  older minors are kept as the maintained documentation of those versions.

## Local development

Requires Node.js 18+.

```bash
cd website
npm install
npm start          # dev server with hot reload
npm run build      # production build (also validates links)
```

## Structure

- `docs/` — the documentation content (Markdown). The sidebar is generated from the folder structure;
  use `sidebar_position` in the front matter and `_category_.json` files to control ordering.
- `versioned_docs/`, `versioned_sidebars/`, `versions.json` — generated snapshots of stable versions;
  do not edit by hand except to fix mistakes in already-released docs.
- `static/img/` — images and other static assets.
- `docusaurus.config.ts` — site configuration.

## Translations

The site is available in English, Simplified Chinese (`zh-Hans`), Japanese (`ja`) and Korean (`ko`).
Translated pages live under `i18n/<locale>/docusaurus-plugin-content-docs/`, mirroring the structure of
`docs/`; a page without a translated file automatically falls back to English. Docusaurus UI strings are
translated via the JSON files under `i18n/<locale>/` (`npm run write-translations -- --locale <locale>`
regenerates missing keys).

Developer documentation (labeler/plugin development, scripting APIs) stays in the repository's `docs/`
folder and is English-only.
