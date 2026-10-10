# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

SaaS where Members of a Workspace maintain a dated Roadmap and publish it at a public URL for anonymous Readers. Use the domain vocabulary in @CONTEXT.md for all code, API and UI naming — respect its _Avoid_ lists and rules (single flat role, Status never inferred from dates, no draft state, Internal Items stripped at serve time).

Monorepo with two independent apps; no root build:

- `frontend/` — TanStack Start (React 19, SSR) + TanStack Router (file-based) + TanStack Query, Tailwind v4, shadcn/ui.
- `backend/` — Spring Boot 4 (Web MVC + Data JPA), Java 27, MariaDB, Lombok. Package root `fr.adriencaubel.roadmap`.

The frontend calls the Spring REST API directly from the browser via TanStack Query.

## Commands

Frontend (run in `frontend/`, use **npm** — `package-lock.json` is canonical):
- `npm run dev` — dev server on port 3000
- `npm run build` / `npm run preview`
- `npm run generate-routes` — regenerate `src/routeTree.gen.ts` (also done automatically by the Vite plugin; never edit that file by hand)
- Add shadcn components: `npx shadcn@latest add <component>`

Backend (run in `backend/`):
- `./mvnw spring-boot:run` — run the API (needs a reachable MariaDB configured in `src/main/resources/application.properties`)
- `./mvnw test` — all tests; single test: `./mvnw test -Dtest=ClassName#method`
- `./mvnw package` — build jar

## Frontend architecture notes

- Routes live in `src/routes/`; layout/HTML shell is `src/routes/__root.tsx`.
- `src/router.tsx` builds the router with a shared `QueryClient` in router context (`src/integrations/tanstack-query/root-provider.tsx`) and wires SSR query dehydration — use route `loader`s with `queryClient.ensureQueryData` for data fetching.
- Import alias `#/*` → `src/*` (package.json `imports`); shadcn components go to `#/components/ui`, `cn()` helper in `#/lib/utils`.
