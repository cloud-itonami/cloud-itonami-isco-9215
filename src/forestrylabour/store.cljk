(ns forestrylabour.store
  "SSoT for the ISCO-08 9215 forestry-site scheduling/logistics
  coordination actor (itonami actor pattern, ADR-2607121000 / CLAUDE.md
  Actors section; README's 'Robotics premise' — a forestry-site scheduling/
  logistics coordination robot performs crew scheduling, task/progress-
  record logging and forestry-supplies procurement coordination for a
  forestry labour crew under this advisor/governor pair, which never
  dispatches hardware itself, never performs forestry work itself, and
  never finalizes a forestry-work-execution decision or a site-safety-
  clearance decision, and never overrides a site safety supervisor's
  judgment — those remain the site safety supervisor's exclusive judgment).
  Modeled closely on cloud-itonami-isco-9212's livestockfarm.store for the
  elementary-occupation outdoor-labour hazard-domain shape, with the
  animal-handling hazard dimension replaced by a falling-tree/branch
  hazard dimension (forestry labourers are elementary-occupation manual
  outdoor forestry-support workers, distinct from the skilled logger/
  forestry-worker classification).

  Domain:

    worker — a registered forestry crew member
             (:worker-id, :name)
    site   — a registered forestry site {:site-id :name
             :max-supply-cost number}. `:max-supply-cost` is an
             informational registered ceiling used only to decide whether
             a `:coordinate-supply-order` proposal escalates to human
             sign-off (the governor never blocks a within-threshold order
             outright; it only decides commit vs. escalate).
    record — a committed operating record (a logged task/progress entry,
             a scheduled crew operation, a flagged safety concern, or a
             coordinated supply order) — written ONLY via commit-record!.
    ledger — append-only audit trail, commit or hold.")

(defprotocol Store
  (worker [s worker-id])
  (site [s site-id])
  (records-of [s worker-id])
  (ledger [s])
  (register-worker! [s worker])
  (register-site! [s site])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (worker [_ worker-id] (get-in @a [:workers worker-id]))
  (site [_ site-id] (get-in @a [:sites site-id]))
  (records-of [_ worker-id] (filter #(= worker-id (:worker-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-worker! [s w]
    (swap! a assoc-in [:workers (:worker-id w)] w) s)
  (register-site! [s f]
    (swap! a assoc-in [:sites (:site-id f)] f) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:workers {} :sites {} :records [] :ledger []}
                                    seed)))))
