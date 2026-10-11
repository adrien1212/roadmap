# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

SaaS where an Owner maintains dated Roadmaps and publishes them at a public URL for anonymous Readers. @SPEC.md is the source of truth for scope, requirements and the domain Language — use its terms for all code, API and UI naming and respect its _Avoid_ lists.

Monorepo with two independent apps; no root build:

- `frontend/` — TanStack Start (React 19, SSR) + TanStack Router (file-based) + TanStack Query, Tailwind v4, shadcn/ui.
- `backend/` — Spring Boot 4 (Web MVC + Data JPA), Java 27, MariaDB, Lombok. Package root `fr.adriencaubel.roadmap`.

The browser only talks to the frontend origin (port 3000 in dev): a proxy (Vite `server.proxy` in dev, reverse proxy in prod) forwards `/api/**`, `/oauth2/**`, `/login/**` to Spring (8080). Spring is the OAuth2 client and owns the session cookie; use relative `/api/...` URLs, no CORS, no tokens in the frontend.

## Commands

Frontend (run in `frontend/`, use **npm** — `package-lock.json` is canonical):
- `npm run dev` — dev server on port 3000
- `npm run build` / `npm run preview`
- `npm run generate-routes` — regenerate `src/routeTree.gen.ts` (also done automatically by the Vite plugin; never edit that file by hand)
- Add shadcn components: `npx shadcn@latest add <component>`

Backend (run in `backend/`):
- `docker compose up -d` (repo root) — local MariaDB on 3306
- `./mvnw spring-boot:run` — run the API
- `./mvnw test` — unit tests only (no Spring context, no DB); single test: `./mvnw test -Dtest=ClassName#method`
- `./mvnw package` — build jar

## Frontend architecture notes

- UI mockups live in `design/`: `roadmap-ui.html` is the reference for every screen; `ui-design.html` holds the landing page and UI kit (tokens, type, components). `design/archive/` is outdated (old Workspace model) — ignore it.
- Routes live in `src/routes/`; layout/HTML shell is `src/routes/__root.tsx`.
- `src/router.tsx` builds the router with a shared `QueryClient` in router context (`src/integrations/tanstack-query/root-provider.tsx`) and wires SSR query dehydration — use route `loader`s with `queryClient.ensureQueryData` for data fetching.
- Import alias `#/*` → `src/*` (package.json `imports`); shadcn components go to `#/components/ui`, `cn()` helper in `#/lib/utils`.
