









(ns monitors
  (:use [arcadia.core]
        [arcadia.linear]
        [arcadia.introspection]
        )
  (:import (UnityEngine Mathf))
  )

(def mon (object-named "StaticMonitor"))


(def mx (object-named "MonitorX"))
(def my (object-named "MonitorY"))
(def head (object-named "Main Camera"))


(defn r []
  (let [x (.. head transform rotation x)
        y (.. head transform rotation y)]
    (set! (.. my transform localEulerAngles)
          (v3 0 (* 100 y) 0))
    (set! (.. my transform localPosition)
          (v3 0 (* -10 x) 0))
    ))

(defn p []
  (let [x (.. head transform position x)
        y (.. head transform position y)
        z (.. head transform position z)]
    (set! (.. mx transform localPosition)
          (v3 x (- y 3) z))
    ))
(p)


(defn game-loop [obj key]
  (r))

(hook+ main :update :monitor #'game-loop)














