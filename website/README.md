# vLabeler documentation site

This folder contains the vLabeler user documentation, built with [Docusaurus](https://docusaurus.io/).

## Versioning and deployment

The site uses [Docusaurus versioning](https://docusaurus.io/docs/versioning) and is deployed to GitHub Pages
by `.github/workflows/docs.yml` on every push to `dev` that touches this folder.

- `docs/` is the documentation of the **latest beta**. In the version dropdown it is labeled with the
  exact app version read from `gradle.properties` at build time (e.g. `1.7.0-beta2`).
  Docs changes should be made together with the related feature changes in PRs based on `dev`.
- On each **stable release**, before merging `dev` into `main`, snapshot the docs on `dev` with the
  exact version being released:

  ```bash
  cd website
  npm run docusaurus docs:version <X.Y.Z>   # e.g. 1.7.0
  ```

  and commit the generated `versioned_docs/` / `versioned_sidebars/` / `versions.json` changes.
  The latest snapshot becomes the default version shown to readers, so the site always opens on the
  documentation of the current stable release, while beta users can switch to the beta version in the
  dropdown. Outdated snapshots can be removed from `versions.json` and `versioned_docs/` when they are
  no longer worth keeping.

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

The site is English-only for now. Developer documentation (labeler/plugin development, scripting APIs) stays
in the repository's `docs/` folder.
