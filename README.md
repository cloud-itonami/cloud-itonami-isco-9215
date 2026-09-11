# cloud-itonami-isco-9215

Open Occupation Blueprint for **ISCO-08 9215**: Forestry Labourers.

This repository designs a forkable OSS business for a forestry-site
scheduling and logistics coordination practice: a site scheduling and
supply-coordination robot manages crew/task records under a governor-gated
actor, so a forestry labour crew keeps its own operating records instead of
renting a closed workforce-management SaaS.

**Maturity: `:implemented`.** `src/forestrylabour/` implements the
`ForestrySiteActor` as a `langgraph.graph/state-graph`
(`forestrylabour.actor`) wired to a `Forestry Labourer Advisor`
(`forestrylabour.advisor`) and an independent `ForestrySiteGovernor`
(`forestrylabour.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt) +->
:hold (:hard?)`. 24 tests / 52 assertions green (`kbb -M:test`). HARD
invariants (always hold, never overridable): worker provenance, site
provenance, no-actuation (`:effect` must be `:propose`), a closed
op-allowlist (`:log-work-record`, `:schedule-crew-operation`,
`:flag-safety-concern`, `:coordinate-supply-order` — nothing else may
ever be proposed), and a permanent, unconditional block on any proposal
that would directly finalize a forestry-work-execution decision (e.g.
authorizing a felling or clearing operation to proceed) *or* a
site-safety-clearance decision (e.g. declaring a site cleared for
safety), or that would override a site safety supervisor's judgment.
Always-escalate paths (human sign-off regardless of confidence, mapping
this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (always) and `:coordinate-supply-order` above the
registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a forestry-site scheduling/
logistics coordination robot performs crew scheduling, task/progress-
record logging and forestry-supplies procurement coordination for a
forestry labour crew, under an actor that proposes actions and an
independent **ForestrySiteGovernor** that gates them. The governor never
dispatches hardware itself, never performs forestry work on site itself,
and never finalizes a forestry-work-execution decision or a site-safety-
clearance decision, and never overrides a site safety supervisor's
judgment; `:high`/`:safety-critical` actions (such as a flagged
falling-tree/branch hazard, uneven-terrain hazard, equipment-condition or
weather-exposure concern, or an above-threshold supply order) require
human sign-off. **This actor coordinates SITE SCHEDULING/LOGISTICS
ONLY — it never performs forestry work itself and never makes a
site-safety-clearance decision itself.**

Forestry Labourers (ISCO-08 9215) is an elementary occupation performing
manual outdoor forestry-support work — distinct from the skilled logger/
forestry-worker classification. The real-world safety stakes are still
high: falling-tree/branch hazard, uneven-terrain hazard and outdoor
weather exposure stack on top of one another, so the governance shape
(independent governor, closed allowlist, hard/escalate split) is not
simplified relative to the higher-skill-tier trades/plant-operator actors
already landed in this track.

## Core Contract

```text
worker roster + site registration + safety-reporting policy
        |
        v
Forestry Labourer Advisor -> ForestrySiteGovernor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
finalize a forestry-work-execution decision, finalize a site-safety-
clearance decision (e.g. declaring a site cleared for safety), override a
site safety supervisor's judgment, suppress an operating record, or
disclose sensitive data without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `9215`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
