(ns studio
  (:use [arcadia.core]
        [arcadia.introspection]
        [arcadia.linear]
        [controllers-state :only [get-notes listen]])
  (:import (UnityEngine GameObject Material Renderer Resources)
           ;(UnityEditor EditorApplication)
           ))

;(set! EditorApplication/isPlaying true)

(def notes-in-c                                             ;TODO: import from music-notation
  (->> (cycle [2 2 1 2 2 2 1])
       (reductions + (- (mod 0 12) 12))
       (drop-while #(< % 0))
       (take-while #(< % 128))))

(def notes-in-c-set (set notes-in-c))

(def mat-black (Resources/Load "black-keys"))
(def mat-white (Resources/Load "white-keys"))

(defn- key+ [parent x-offset w note]
  (let [pivot (GameObject. (str "pivot-" note))
        key (create-primitive :cube "key")
        [h d] [(* 1.25 w) (* 6.5 w)]]
    (child+ pivot key)
    (child+ parent pivot)
    (set! (.. pivot transform localPosition) (v3 (* w x-offset) 0 0))
    (set! (.. key transform localPosition) (v3 0 0 (- (/ d 2))))
    (set! (.. key transform localScale) (v3 (* 0.95 w) h d))
    (set! (.. (cmpt key Renderer) material)
          (if (contains? notes-in-c-set note)
            mat-white
            mat-black))
    pivot))

(defn- index-of [e coll] (first (keep-indexed #(if (= e %2) %1) coll)))

(defn- notes-in-c-range [low high]
  (filter #(and (>= % low) (<= % high)) notes-in-c))

(defn- keys+ "creates gameobjects for keys on a keyboard, returns a hashmap of note->gameobject"
  [container-obj low high keyboard-width]
  (let [white-notes (notes-in-c-range low high)
        black-notes (filter #(not (contains? notes-in-c-set %)) (range low (+ 1 high)))
        idx-of-middle-c (index-of 60 white-notes)
        key-width (/ keyboard-width (count white-notes))

        white-keys
        (->> white-notes
             (map-indexed
               (fn [index note]
                 [note
                  (key+ container-obj                       ;parent
                        (- index idx-of-middle-c)           ;x-offset
                        key-width                           ;width
                        note)]))                            ;note
             )

        black-keys
        (->> black-notes
             (map-indexed
               (fn [index note]
                 [note
                  (let [pivot (key+ container-obj           ;parent
                                    (- (index-of (+ note 1) white-notes)
                                       idx-of-middle-c
                                       0.5)                 ;x-offset
                                    key-width               ;width
                                    note                    ;note
                                    )
                        key (first (children pivot))]
                    (.Translate (.. key transform) (v3 0 (/ key-width 2) 0.02))
                    (set! (.. key transform localScale)
                          (v3 (/ key-width 2) key-width 0.1))
                    pivot)])))
        ]

    (->> (concat white-keys black-keys)
         (flatten)
         (apply hash-map))
    ))

(def keystation-obj (object-named "Keystation"))
(def a-300-obj (object-named "A-300"))
(def bcr-2000-obj (object-named "BCR-2000"))

(def object-state
  (atom {
         :keystation {
                      :buttons {}
                      :faders  {}
                      :keys    (keys+ keystation-obj 21 108 1.225)
                      :knobs   {}
                      }
         :a-300      {
                      :buttons {}
                      :faders  {}
                      :keys    (keys+ a-300-obj 41 72 0.44)
                      :knobs   {}
                      }
         :bcr-2000 {
                    :buttons {}
                    :knobs {}
                    }
         }))



(defn find-child "returns the first child of go with name"
  [go name] (some #(when (= name (.name %)) %)
                  (children go)))

(def spacenav-obj (object-named "Spacenav"))
(def spacenav-puck (find-child spacenav-obj "Puck"))
(defn- on-spacenav [{translation :translation rotation :rotation}]
  (set! (.. spacenav-puck transform localPosition) translation)
  (set! (.. spacenav-puck transform localRotation) rotation)
  )

(some #(when (= name (.name %)) spacenav-obj)
      (children spacenav-obj))
(children spacenav-obj)
spacenav-puck

(listen :spacenav #'on-spacenav)


;(defn- on-keystation-evt [evt index val]
;  (cond (= evt :note)
;        (let [go (keystation-keys index)]
;          (when-not (nil? go)
;            (set! (.. go transform localRotation)
;                  (euler (v3 (if (= val 0) 0 -4) 0 0))))))
;  (log "got " evt " " index " " val))

(defn- on-evt [device-name evt index val]
  (cond (= evt :notes)
        (let [go (get-in @object-state [device-name :keys index])]
          (when-not (nil? go)
            (set! (.. go transform localRotation)
                  (euler (v3 (if (= val 0) 0 -4) 0 0)))))))

(defn- on-midi-evt [device-name]
  (fn [evt index val]
    (on-evt device-name evt index val)
    (log index)
    ;(log "got " device-name " " evt " " index " " val)
    ))

;(listen :keystation #'on-keystation-evt)
(listen :keystation (on-midi-evt :keystation))
(listen :a-300 (on-midi-evt :a-300))



