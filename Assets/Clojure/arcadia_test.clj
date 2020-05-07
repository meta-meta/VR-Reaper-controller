(ns arcadia-test
  (:require [arcadia.core :as a]))

(use 'clojure.repl)

(a/object-named "Main Camera")

(a/create-primitive :cube)

(doc a/gobj)

