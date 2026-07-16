# vLabeler documentation site

This folder contains the vLabeler user documentation, built with [Docusaurus](https://docusaurus.io/).

The site is deployed to GitHub Pages by `.github/workflows/docs.yml` on every push to `main` that touches this
folder, so the published site always matches the released version. Docs changes should be made together with the
related feature changes in PRs based on `dev`.

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
- `static/img/` — images and other static assets.
- `docusaurus.config.ts` — site configuration.

## Translations

The site is English-only for now. Developer documentation (labeler/plugin development, scripting APIs) stays
in the repository's `docs/` folder.
