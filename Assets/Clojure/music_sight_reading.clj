(ns music-sight-reading
  (:use [arcadia.core]
        [arcadia.linear]
        [controllers-state :only [get-notes]]
        [music-live-compose]
        [music-notation :only [clear-notes
                               set-played-notes!
                               set-keysig!
                               +note
                               -note]]
        [scheduler :only [now start clear-queue queue+ elapsed]]
        )
  (:import (UnityEngine Color GameObject Mathf TextMesh Time)))

(def perf-state (atom {
                       :history []
                       :exercises {}
                       }))


(defn prep-stats-for-exercise [exercise timestamp]
  (swap! perf-state assoc-in [:exercises exercise timestamp] []))

(defn set-stat [exercise timestamp idx val]
  (swap! perf-state assoc-in [:exercises exercise timestamp idx] val))

(defn get-stat [exercise timestamp idx]
  (get-in @perf-state [:exercises exercise timestamp idx]))

(defn save-perf-state []
  (spit "perf-data.edn" (pr-str @perf-state)))

(defn load-perf-state []
  (reset! perf-state (read-string (slurp "perf-data.edn")))
  nil)

(comment
  (prep-stats-for-exercise :123 1)
  (set-stat :123 1 0 1.3)
  (get-stat :123 1 0)
  @perf-state
  (save-perf-state)
  (load-perf-state)
  )


(defn game-loop [obj key]
  (set-played-notes! (get-notes)))

(hook+ (object-named "App") :update :sight-reading #'game-loop)

(defn scroll-go "scrolls :note event on staff by x"
  [^GameObject go x]
  (set! (.. go transform position)
        (v3 (+ music-notation/x0 (* 3 x)) 0 -0.1)))

(defn set-go-color "sets color of :note event"
  [^GameObject go r g b a]
  (doseq [child (children go)]
    (set! (. (cmpt child TextMesh) color)
          (Color. r g b a))))

(defn update-queued "invoked on every :queued event every frame"
  [note go t0]
  (let [seconds-left (- t0 (elapsed))]
    (scroll-go go seconds-left)))

(defn update-active "invoked on every :active event every frame"
  [note go t0 duration get-stat-fn set-stat-fn]
  (let [secs-til-active (- t0 (elapsed))
        score (* (Time/deltaTime) ;TODO high res time
                 duration ; TODO  check math
                 (if (contains? (get-notes) note) 1/2 -1/2))
        next-stat-val (+ (or (get-stat-fn) 0.5) score)
        [r g b] [(- 1 next-stat-val) next-stat-val 0]]
    (set-stat-fn next-stat-val)
    (set-go-color go r g b (+ 1 (Mathf/Pow secs-til-active 3)))
    (scroll-go go (+ secs-til-active
                     (Mathf/Pow secs-til-active 2)))))

(defn queue-event
  "queues an event"
  [get-stat-fn set-stat-fn idx t duration note]
  (let [go (if (= note :r) (GameObject.) ;TODO: +rest
                           (+note note t))
        t0 (+ (elapsed) t)]
    (queue+ {
             :t0            t0
             :duration      duration
             :start         #(do
                               (when (not= note :r) (send-note note 63))
                               ;(set-stat-fn idx 0.5) ; not needed here if we guard in update-active
                               )
             :end           #(do
                               (when (not= note :r) (send-note note 0))
                               (-note go))
             :update-queued (if (= note :r) #() #(update-queued note go t0))
             :update-active #(update-active note go t0 duration
                                            (partial get-stat-fn idx)
                                            (partial set-stat-fn idx))
             }))
  nil)


(defn clear []
  (clear-notes)
  (clear-queue))


; TODO: put all notes in a sequence as children of container object, scroll container instead of individual notes
;(doseq [glyph-go (filter identity (flatten glyph-gos))]
;  (child+ go glyph-go true))

(comment

  (defn key->keyword [n] (nth [:c :db :d :eb :e :f :gb :g :ab :a :bb :b]
                              n))
  (set-keysig! :c)

  (queue-program
    (->> keys-by-fourths
         (map (fn [key]
                (->> (range 1 7)
                     (map (fn [step]
                            (let [timestamp (now) ;TODO use time when actually played instead of queued?
                                  exercise (keyword (str "range-" key "-by-" step)) ;TODO hash the pattern for uid
                                  ]
                              (fn []
                                (set-keysig! (key->keyword key))
                                (prep-stats-for-exercise exercise timestamp)
                                (queue-pattern
                                  (concat [:r] (range-exercise-diatonic (+ 48 key) 48 84 step)) ;notes
                                  (clojure.core/repeat 1) ;rhythm
                                  60 ;bpm
                                  (partial queue-event ;queuer fn per note
                                           (partial get-stat exercise timestamp) ; to be called with idx
                                           (partial set-stat exercise timestamp) ; to be called with idx, val
                                           ))
                                )))))))
         (apply concat)))

  @perf-state

  (clear)


  (doall (map (fn [n t] (queue-event (+ 1 t) 1 n))
              (concat
                (range-exercise-diatonic 60 48 72 1)
                (range-exercise-diatonic 60 48 72 2 0)
                (range-exercise-diatonic 60 48 72 2 1)
                (range-exercise-diatonic 60 48 72 3 0)
                (range-exercise-diatonic 60 48 72 3 1)
                (range-exercise-diatonic 60 48 72 3 2)
                (range-exercise-diatonic 60 48 72 4 0)
                (range-exercise-diatonic 60 48 72 4 1)
                (range-exercise-diatonic 60 48 72 4 2)
                (range-exercise-diatonic 60 48 72 4 3)
                )
              (reductions + (clojure.core/repeat 2))))

  (clear)

  (set-keysig! :db)


  )



