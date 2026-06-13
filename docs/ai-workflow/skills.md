# AI Workflow — Skills & Raw Files

This document lists the AI capabilities ("skills") actually used to build this
submission, and points to the raw files that captured the workflow. It is the
companion to [`AGENTS.md`](../../AGENTS.md), which describes the operating
principles and chronological prompt flow.

## Skills used

The work was done in **VS Code** with the **GitHub Copilot** chat agent in
**agent mode** (Claude-class model). No domain-specific named skill packs
(MISE, SFI, Azure Foundry, etc.) were invoked — those were available in the
environment but are unrelated to a Spring Boot take-home. The skills exercised
were the agent's built-in capabilities:

| Skill / Tool                  | How it was used                                                                 |
|-------------------------------|---------------------------------------------------------------------------------|
| File read / search / glob     | Inspect generated code, locate cross-cutting concerns, verify edits             |
| File create / edit            | Author every Java source file, `pom.xml`, `application.yml`, docs               |
| Terminal execution            | `mvn compile`, `mvn test`, `git`, environment setup (JDK 17 on PATH)            |
| Workspace structural search   | Locate entities/services when wiring controllers and tests                      |
| Iterative test-fix loop       | Run `mvn test`, parse failures, patch, re-run until green                       |

No code was accepted blindly — every meaningful design choice (data model,
concurrency strategy, transaction boundaries, RBAC model, refund-policy
semantics, pricing-tier precedence rules) was decided by the developer and
the agent implemented to spec.

## Raw files

The "raw files used during development" are the artifacts already checked into
this repository:

- [`AGENTS.md`](../../AGENTS.md) — operating principles, prompt-by-prompt
  intent log, and explicit list of decisions not delegated to the agent.
- [`README.md`](../../README.md) — architecture, run/test instructions, and
  the complete **Assumptions** section that pins down every interpretation
  of the open-ended brief.
- Git history — the repository is committed as a sequence of logical commits
  mirroring the development phases described in `AGENTS.md`
  (scaffold → domain → data layer → security → catalog → seat-hold →
  booking/payment/refund → tests → docs).
- `src/test/**` — the test suite is itself a workflow artifact: integration
  test `BookingFlowIntegrationTest` exercises concurrent seat allocation
  (8-thread fan-out on a single seat) and hold-expiry sweep, validating the
  concurrency strategy chosen up front.

No external chat transcripts, prompt template files, or `.prompt.md` /
`.instructions.md` customization files were authored or required for this
build.
