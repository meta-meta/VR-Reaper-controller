(ns haptics
  (:use [arcadia.core]
        [arcadia.linear]
        [arcadia.introspection]
        )
  (:import OVRManager
           OVRHaptics
           OVRHapticsClip
           (UnityEngine Mathf))
  )


(def app (object-named "App"))
(cmpt+ app OVRManager)

;(defn game-loop [obj key])
;(hook+ app :update :haptics #'game-loop)

(def clip (OVRHapticsClip. 160))
(set! (. clip Count) (. clip Capacity))


; TODO: logarithmic, move to utils
(defn scale "scales in from inRange -> outRange"
  [inMin inMax outMin outMax in]
  (let [diffIn (- inMax inMin)
        normIn (/ (- in inMin) diffIn)
        diffOut (- outMax outMin)
        ]
    (+ outMin (* normIn diffOut)))
  )

(defn set-samples [fn]
  (doseq [i (range (. clip Capacity))]
    (aset-byte (. clip Samples)
               i
               (fn i (. clip Capacity)))))

(defn sin-samples [i total]
  (scale -1. 1.
         0 255
         (Mathf/Sin (- (* 2 Mathf/PI 2 (/ i total)) (/ Mathf/PI 2)) )))

(set-samples sin-samples)



(defn play-clip []
  (.. OVRHaptics LeftChannel (Preempt clip)))


#_(
    (dir arcadia.core)
    (dir arcadia.introspection)
    (play-clip)
    )














