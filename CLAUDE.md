# CLAUDE.md

All project context, rules, and conventions are defined in [AGENTS.md](AGENTS.md). This file
extends it with Claude Code-specific notes. Read AGENTS.md first.

## Documentation

- `docs/` — raw product and system documentation (Markdown)
- `docs-site/` — Astro/Starlight documentation site that renders the above into a browsable website
  - `cd docs-site && npm run dev` — dev server with hot reload
  - `cd docs-site && npm run build` — static build to `dist/`
