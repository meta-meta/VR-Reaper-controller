(ns osc
  (:use [arcadia.core]))

(def osc-in (cmpt (object-named "OSC") "OscIn"))
(def osc-out (cmpt (object-named "OSC") "OscOut"))

(. osc-in (Open 7000 ""))
(. osc-out (Open 8000 "127.0.0.1"))

(defn send [addr msg]
  (let [msg (cond
              (coll? msg) (to-array (map int msg)) ; TODO other types in array
              (int? msg) (int msg)
              (float? msg) (float msg)
              :else msg)]
    (. osc-out (Send addr msg))))

(defn listen
  "registers fn as a listener for OSC msgs at addr. fn will be invoked with a single argument of type OscMessage"
  [addr fn]
  (. osc-in (Map addr fn)))

(comment
  (send "/organ" [60 0])
  (send "/drawbar" [0 60])
  (send "/drawbar" [1 127])

  (map #(send "/drawbar" [% (* 127 (- 1 (/ % 9)))]) (range 9)))

