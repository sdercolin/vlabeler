import * as fs from 'fs';
import * as path from 'path';
import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

// The exact version of the current beta docs, e.g. "1.7.0-beta2", taken from the app itself.
const appVersion = fs
  .readFileSync(path.join(__dirname, '..', 'gradle.properties'), 'utf8')
  .match(/^app\.version=(.+)$/m)![1];

const config: Config = {
  title: 'vLabeler',
  tagline: 'Open-source voice labeling application',
  favicon: 'img/favicon.ico',

  future: {
    v4: true,
  },

  url: 'https://sdercolin.github.io',
  baseUrl: '/vlabeler/',

  // GitHub pages deployment config.
  organizationName: 'sdercolin',
  projectName: 'vlabeler',

  onBrokenLinks: 'throw',

  // The site is English-only for now.
  // When translations are ready, add 'zh-Hans', 'ja' and 'ko' to `locales`.
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          routeBasePath: '/',
          sidebarPath: './sidebars.ts',
          // Docs PRs are based on the dev branch.
          editUrl: 'https://github.com/sdercolin/vlabeler/tree/dev/website/',
          // `docs/` on dev is the documentation of the latest beta, labeled with the
          // exact app version from gradle.properties (e.g. "1.7.0-beta2").
          // On each stable release, it is snapshotted into `versioned_docs/` via
          // `npm run docusaurus docs:version <X.Y.Z>` (see website/README.md), and the
          // latest snapshot becomes the default version shown to readers.
          versions: {
            current: {
              label: appVersion,
            },
          },
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    image: 'img/logo.png',
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: 'vLabeler',
      logo: {
        alt: 'vLabeler logo',
        src: 'img/logo.png',
      },
      items: [
        {
          type: 'docsVersionDropdown',
          position: 'right',
        },
        {
          href: 'https://vlabeler.com',
          label: 'Homepage',
          position: 'right',
        },
        {
          href: 'https://discord.gg/yrTqG2SrRd',
          label: 'Discord',
          position: 'right',
        },
        {
          href: 'https://github.com/sdercolin/vlabeler',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Docs',
          items: [
            {
              label: 'Getting Started',
              to: '/getting-started/installation',
            },
            {
              label: 'Troubleshooting',
              to: '/troubleshooting',
            },
          ],
        },
        {
          title: 'Community',
          items: [
            {
              label: 'Discord',
              href: 'https://discord.gg/yrTqG2SrRd',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'Homepage',
              href: 'https://vlabeler.com',
            },
            {
              label: 'GitHub',
              href: 'https://github.com/sdercolin/vlabeler',
            },
            {
              label: 'Releases',
              href: 'https://github.com/sdercolin/vlabeler/releases',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} sdercolin. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['json'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
