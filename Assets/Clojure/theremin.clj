(ns theremin
  (:use [arcadia.core]
        [arcadia.linear]
        [osc :only [send]])
  (:import (UnityEngine GameObject Mathf Vector3)))

(comment "sixense"
         (def left-hand (object-named "Hand - Left"))
         (def right-hand (object-named "Hand - Right")))

(def left-hand (object-named "CapsuleHand_L"))
(def right-hand (object-named "CapsuleHand_R"))
(def ch-right (cmpt right-hand "CapsuleHand"))
(defn get-leap-pos [hand]
  (apply v3 (.. hand GetLeapHand PalmPosition ToFloatArray)))
(get-leap-pos right-hand)

(def amp-obj (object-named "Amplitude"))
(def freq-obj (object-named "Frequency"))

(def sixense-input (cmpt (object-named "SixenseInput") "SixenseInput"))
(.. sixense-input Controllers)

(defn is-docked []
  (< 0
     (->> (SixenseInput/Controllers)s
          (map #(. % Docked))
          (filter identity)
          (count))))

(defn get-pos [^GameObject go] (.. go transform position))
(. (v3 1 1 1) sqrMagnitude)

(v3) (get-pos amp-obj)


(defn send-state []
  (send "/theremin/amp" (if (is-docked) 0
                                        (Vector3/Distance (get-pos amp-obj) (get-pos left-hand))))
  (send "/theremin/freq" (if (is-docked) 0
                                         (Vector3/Distance (get-pos freq-obj) (get-pos right-hand)))))



(defn game-loop [obj key]
  (send-state)
  )

(hook+ (object-named "App") :update :theremin #'game-loop)