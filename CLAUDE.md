# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project
Learning project: an expense tracker built with Spring Boot 3.5 (Java 21, Maven), PostgreSQL and a plain HTML/CSS/JS frontend served from `src/main/resources/static/`. The user is a beginner: work stage by stage, explain each step in Ukrainian, and wait for confirmation before moving to the next stage. The development plan is `PLAN.md` (in Ukrainian): follow its stages and steps, and mark steps `[x]` (and the stage ✅) as they are completed and verified.

## Running
- Start the DB first: `docker compose up -d` (container `expense-tracker-db`), then `mvn spring-boot:run`.
- PostgreSQL is on host port **5433**, not 5432 (5432 is taken by an SSH tunnel on this machine). JDBC URL: `jdbc:postgresql://localhost:5433/expenses`, user/password `postgres`/`postgres`.
- SQL console: `docker exec -it expense-tracker-db psql -U postgres -d expenses`.

## Commits
Follow [Conventional Commits](https://www.conventionalcommits.org/): `<type>(<optional scope>): <description>`.
- Types: `feat` (new functionality), `fix` (bug fix), `docs`, `test`, `refactor`, `style` (formatting only), `build` (pom.xml, dependencies), `chore` (config, .gitignore, tooling).
- Scopes for this repo: `db`, `backend`, `api`, `frontend`, `tests`.
- Subject in English, imperative mood, lowercase, no trailing period, ≤ 72 chars (e.g. `feat(api): add expense CRUD endpoints`). Add a body explaining *why* when it isn't obvious.
- One commit per completed PLAN.md stage (or a smaller logical step); commit only verified, working code. Never commit `target/` or IDE files.

## Gotchas
- Keep `TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kyiv"))` at the start of `main` in `ExpenseTrackerApplication`: macOS reports the legacy id `Europe/Kiev`, which PostgreSQL 16 rejects on connect.
- The schema is created by Hibernate (`spring.jpa.hibernate.ddl-auto=update`); there are no migrations yet.
