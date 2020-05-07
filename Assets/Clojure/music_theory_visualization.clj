(ns music-theory-visualization
  (:use [arcadia.core]
        [arcadia.introspection]
        [arcadia.linear]
        [controllers-state :only [get-notes listen]])
  (:import (UnityEngine Color GameObject LineRenderer Material Mathf Renderer Resources Vector3)))

(def mats (->> (range 12)
               (map (fn [n] [n (Resources/Load (str "n" n))]))
               (cons [:node (Resources/Load "node")])
               (flatten)
               (apply hash-map)))

(def interval-line-mats (->> (range 12)
                             (map (fn [n] [n (Resources/Load (str "Intervals/interval-line-" n))]))
                             (flatten)
                             (apply hash-map)))

(def spiral-obj (object-named "Spiral"))
(def note-size-min 0.3)
(def note-size-max 0.55)
(defn note->scale [n] (+ 1 (/ (/ n -12) 12)))
(def seg (/ Mathf/PI 6))

(defn- note->spiral-position [note]
  (let [y (/ note -12)
        scale (note->scale note)
        x (* (Mathf/Sin (* seg note)) scale)
        z (* (Mathf/Cos (* seg note)) scale)]
    (v3 x y z)))

(defn- note->spiral-localScale [note vel]
  (v3 (* (note->scale note)
         (Mathf/Lerp
           (if (= 0 vel) 0.1 note-size-min)
           note-size-max
           (/ vel 128)))))

(def spiral
  (->> (range 21 109)
       (map (fn [n] (let [pos (mod n 12)
                          localPosition (note->spiral-position n)
                          localScale (note->spiral-localScale n 0)
                          scale (note->scale n)
                          node-name (str "node-" n)
                          note-name (str "note-" n)
                          node (create-primitive :sphere node-name)
                          note (create-primitive :sphere note-name)
                          ]
                      (child+ spiral-obj node)
                      (child+ spiral-obj note)
                      (set! (.. node transform localPosition) localPosition)
                      (set! (.. note transform localPosition) localPosition)
                      (set! (.. node transform localScale) (v3 (* note-size-min 0.9 scale)))
                      (set! (.. note transform localScale) (v3 (* 0.1 scale)))
                      (set! (.. (cmpt node Renderer) material) (mats :node))
                      (set! (.. (cmpt note Renderer) material) (mats pos))
                      [(keyword node-name) node
                       (keyword note-name) note]
                      )))
       (flatten)
       (apply hash-map)))

(def ac-pitch-mat (Resources/Load "acoustic-pitch"))
(def ac-pitch-obj
  (let [go (create-primitive :sphere "acoustic-pitch")]
    (child+ spiral-obj go)
    (set! (.. go transform localScale) (v3 0))
    (set! (.. (cmpt go Renderer) material) ac-pitch-mat)
    go
    ))


(defn- update-interval
  "updates the interval given a pair of notes, returns the go. if a go is supplied, it must have a LineRenderer cmpt. If a go is not supplied, one will be created"
  ([pair go]
   (let [lr (cmpt go LineRenderer)
         interval (mod (Math/Abs (apply - pair))
                       12)
         name (str "int-line " pair)]
     (when (not= name (.. go name)) ;optimisation
       (set! (.. go name) name)
       (set! (.. lr material) (interval-line-mats interval))
       (.. lr (SetPositions (into-array
                              Vector3
                              (map #(.. (spiral (keyword (str "node-" %)))
                                        transform
                                        localPosition)
                                   pair)))))
     go))
  ([pair]
   (let [go (GameObject.)
         lr (cmpt+ go LineRenderer)]
     (child+ spiral-obj go)
     (set! (.. lr useWorldSpace) false)
     (.. lr (SetWidth 0.005 0.005))
     (.. lr (SetVertexCount 2))
     (update-interval pair go))))

(def interval-lines
  (doall (->> (range 21 48)
              (map #(vec [% (+ 1 %)]))
              (map update-interval)
              )))

(defn- disable-interval-lines []
  (doall (map
           #(.. % (SetActive false))
           interval-lines)))

(def curr-notes (atom '()))

(defn- update-intervals []
  (let [notes (get-notes :keystation)
        sorted-notes (->> notes
                          (map first)
                          (sort))]
    (when (not= @curr-notes sorted-notes)
      (let [pairs (->> sorted-notes
                       (drop-last)
                       (map-indexed (fn [i n]
                                      (map (fn [a b] [a b])
                                           (repeat n)
                                           (drop (+ 1 i) sorted-notes))))
                       (apply concat))
            ]
        (swap! curr-notes (fn [x] sorted-notes))
        (disable-interval-lines)
        (doall (->> pairs
                    (map-indexed
                      (fn [i pair]
                        (let [go (nth interval-lines i)]
                          (update-interval pair go)
                          (.. go (SetActive true))
                          )))))
        ))))

(defn- on-evt [device-name evt index val]
  (cond (= evt :notes)
        (let [go (spiral (keyword (str "note-" index)))
              scale (note->scale index)]
          (set! (.. go transform localScale)
                (v3 (* scale
                       (Mathf/Lerp
                         (if (= 0 val) 0.1 note-size-min)
                         note-size-max
                         (/ val 128)))))
          (update-intervals)
          )))

(defn- on-midi-evt [device-name]
  (fn [evt index val]
    (on-evt device-name evt index val)))

;(listen :keystation #'on-keystation-evt)
(listen :keystation (on-midi-evt :keystation))

(defn on-acoustic-pitch [[note vel]]
  ;(.SetColor ac-pitch-mat "_Color"                          ;slows down too much. try Color.Lerp?
  ;           (Color/HSVToRGB (/ (mod note 12) 12) 1 1))
  (set! (.. ac-pitch-obj transform localPosition)
        (note->spiral-position note))
  (set! (.. ac-pitch-obj transform localScale)
        (if (= 0.0 vel)
          Vector3/zero
          (note->spiral-localScale note vel))))

(listen :acoustic-pitch #'on-acoustic-pitch)
;(listen :a-300 (on-midi-evt :a-300))
