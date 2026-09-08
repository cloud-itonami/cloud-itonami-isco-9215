(ns forestrylabour.governor
  "ForestrySiteGovernor — the independent safety/scope layer gating every
  forestry-site scheduling/logistics proposal an advisor may make for a
  forestry labour crew. The governor never dispatches hardware itself,
  never performs forestry work on site itself, and never finalizes a
  forestry-work-execution decision (e.g. deciding to authorize a felling
  or clearing operation to proceed) or a site-safety-clearance decision
  (e.g. declaring a site cleared for safety), and never overrides a site
  safety supervisor's judgment — those are permanently out of this
  actor's scope and remain a site safety supervisor's exclusive judgment
  (README's 'Robotics premise': this actor coordinates SITE SCHEDULING/
  LOGISTICS ONLY — it never performs forestry work or makes site-safety-
  clearance decisions itself). Modeled closely on cloud-itonami-isco-
  9212's livestockfarm.governor for the elementary-occupation outdoor-
  labour hazard-domain shape (forestry labourers perform manual outdoor
  forestry-support work — an elementary occupation distinct from the
  skilled logger/forestry-worker classification — in uneven-terrain
  conditions under weather exposure, so falling-tree/branch hazard and
  uneven-terrain/equipment-condition/weather-exposure stakes stack on top
  of each other), with the animal-handling hazard dimension replaced by a
  falling-tree/branch hazard dimension.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. worker provenance     — the crew member must be independently
                                verified/registered before any action.
    2. site provenance       — the forestry site must be independently
                                verified/registered before any action.
    3. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never performs forestry work itself; it
                                only gates what the advisor may
                                coordinate).
    4. closed op-allowlist    — only :log-work-record,
                                :schedule-crew-operation,
                                :flag-safety-concern and
                                :coordinate-supply-order may ever be
                                proposed; anything else is refused.
    5. scope-excluded action  — any proposal to directly finalize a
                                forestry-work-execution decision (e.g.
                                authorizing a felling or clearing
                                operation to proceed), or to directly
                                finalize a site-safety-clearance decision
                                (e.g. declaring a site cleared for
                                safety), or to override a site safety
                                supervisor's judgment, is a hard,
                                permanent block (checked both against the
                                proposed :op and, defense-in-depth,
                                against the proposal's :rationale text —
                                matched as full finalization/execution
                                ACTION phrases such as \"authorize the
                                felling operation to proceed\" /
                                \"declare the site safety cleared\" /
                                \"override the site safety supervisor's
                                judgment\", never as bare nouns like
                                \"tree\", \"branch\" or \"chainsaw\", so
                                the check can never self-trip on the
                                advisor's own routine rationale text,
                                e.g. \"logged work record for worker …\"
                                or \"scheduled crew operation for
                                forestry support task …\" or \"…routed
                                for site safety supervisor review\" — all
                                three legitimately mention this actor's
                                domain vocabulary but none is a
                                finalization action, and all are
                                exercised by
                                `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off
  regardless of confidence):
    6. :op :flag-safety-concern (a falling-tree/branch hazard, uneven-
                                terrain hazard, equipment-condition
                                concern or weather-exposure concern
                                always escalates to a human, never
                                auto-commits).
    7. :op :coordinate-supply-order above `supply-cost-threshold`.
    8. low confidence (< `confidence-floor`)."
  (:require [kotoba.lang.text :as str]
            [forestrylabour.store :as store]))

(def confidence-floor 0.6)
(def supply-cost-threshold 2000)

(def allowed-ops
  #{:log-work-record :schedule-crew-operation
    :flag-safety-concern :coordinate-supply-order})

;; Defense-in-depth: none of these ops are ever in `allowed-ops` above,
;; so they are already refused by the closed-allowlist check below; they
;; are named again here — as explicit finalization/execution ACTIONS,
;; never bare nouns — so a future allowlist edit cannot silently re-open
;; either of these two independent out-of-scope paths (forestry-work-
;; execution finalization, site-safety-clearance finalization) without
;; also touching this list.
(def ^:private scope-excluded-ops
  #{:authorize-felling-operation :authorize-clearing-operation
    :finalize-felling-operation :finalize-forestry-work-execution-decision
    :declare-site-safety-cleared :finalize-site-safety-clearance
    :clear-site-for-safety
    :override-safety-supervisor-judgment :override-site-safety-supervisor-judgment})

;; Full finalization/execution ACTION phrases only — never bare nouns
;; ("tree", "branch", "chainsaw", "felling", "site", "safety", "site
;; safety supervisor") — so this can never match inside the mock
;; advisor's own default rationale text (which legitimately contains
;; those bare nouns, e.g. "forestry support task" / "site safety
;; supervisor review"). See
;; `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`.
(def ^:private scope-excluded-phrases
  ["authorize the felling operation to proceed"
   "authorize the clearing operation to proceed"
   "finalize the felling operation"
   "finalize the clearing operation"
   "finalize the forestry work execution decision"
   "finalize the forestry-work decision"
   "declare the site safety cleared" "declare the site cleared for safety"
   "finalize the site safety clearance" "clear the site for safety"
   "override the site safety supervisor's judgment"
   "override the safety supervisor's judgment"
   "override site safety supervisor judgment"
   "override safety supervisor judgment"])

(defn- contains-excluded-phrase? [s]
  (let [s (str/lower (or s ""))]
    (boolean (some #(str/includes? s %) scope-excluded-phrases))))

(defn- hard-violations [proposal worker-record site-record]
  (let [{:keys [op rationale]} proposal]
    (cond-> []
      (nil? worker-record)
      (conj {:rule :no-worker
             :detail "未登録 worker への提案は不可（worker record は独立して検証・登録済みでなければならない）"})

      (nil? site-record)
      (conj {:rule :no-site
             :detail "未登録 site への提案は不可（site record は独立して検証・登録済みでなければならない）"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は現場作業を直接実行しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :unknown-op
             :detail (str op " は closed op-allowlist に無い — 提案不可")})

      (or (contains? scope-excluded-ops op) (contains-excluded-phrase? rationale))
      (conj {:rule :scope-excluded-action
             :detail "forestry-work-execution 判断（伐採・伐開作業の許可を含む）の確定、site-safety-clearance 判断の確定、site safety supervisor の判断の上書きは、この actor の権限外 — 常に永続ブロック"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a `store`
  implementing `forestrylabour.store/Store`. Pure — never mutates the
  store, never dispatches a site-floor operation, never finalizes a
  site-safety-clearance decision."
  [request _context proposal store]
  (let [worker-record (store/worker store (:worker-id request))
        site-record (some->> (:site-id proposal) (store/site store))
        hard (hard-violations proposal worker-record site-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        supply-order-over-threshold?
        (and (= :coordinate-supply-order (:op proposal))
             (number? (:cost proposal))
             (> (:cost proposal) supply-cost-threshold))
        always-risky? (or (= :flag-safety-concern (:op proposal))
                           supply-order-over-threshold?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
