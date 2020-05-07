(ns scheduler
  (:use [arcadia.core]
        [clojure.set :only [difference union]])
  (:import (UnityEngine Time)))

(def s (atom {
              :t0     0
              :queue  #{}
              :active #{}
              :past   #{}
              }))

;(defn now "current time in seconds" [] (Time/time))
(defn now "current time in seconds" []
  (/ (. (DateTime/Now) Ticks) 10000000)) ;https://msdn.microsoft.com/en-us/library/system.datetime.ticks(v=vs.110).aspx There are 10,000 ticks in a millisecond, or 10 million ticks in a second.

(defn start "sets :t0 to current time" []
  (swap! s assoc :t0 (now)))

(defn elapsed "seconds since start" [] (- (now) (:t0 @s)))


(defn as-set [evts-or-set]
  (if (set? (first evts-or-set))
    (first evts-or-set)
    (set evts-or-set)))

(defn clear-queue "clears :queue" [] (swap! s assoc :queue #{}) nil)

(defn queue+ "adds events to :queue" [& evts]
  (swap! s update-in [:queue]
         #(union % (as-set evts))))

(defn queue- "removes events from :queue" [evts]
  (swap! s update-in [:queue] #(difference % (as-set evts))))


(defn active+ "adds events to the :active" [evts]
  (swap! s update-in [:active] #(union % (as-set evts))))

(defn active- "removes events from :active" [evts]
  (swap! s update-in [:active] #(difference % (as-set evts))))


(defn past+ "adds events to :past" [evts]
  (swap! s update-in [:past] #(union % (as-set evts))))

(defn past- "removes events from :past" [evts]
  (swap! s update-in [:past] #(difference % (as-set evts))))


(defn evt-started? "checks if elapsed > evt start time" [evt]
  (> (elapsed) (:t0 evt)))

(defn evt-ended? "checks if elapsed > evt end time" [evt]
  (> (elapsed) (+ (:t0 evt) (:duration evt))))

(defn evt-ready? "checks if current time falls between event start and end time" [evt]
  (and (evt-started? evt)
       (not (evt-ended? evt))))


(defn dequeue-missed-evts "removes events from :queue which have not been activated but whose end time has passed" []
  (let [missed-evts (filter evt-ended? (:queue @s))]
    (queue- missed-evts)
    (if (> 0 (count missed-evts))
      (log "missed" missed-evts))))

(defn activate-ready-evts
  "identifies queued events with a :t0 < elapsed, adds them to :active, removes them from :queue, executes their :fn" []
  (let [evts (filter evt-ready? (:queue @s))]
    (doall (map #((:start %)) evts))
    (active+ evts)
    (queue- evts)))

(defn deactivate-past-evts
  "identifies active events which have ended, adds them to :past, removes them from :active" []
  (let [evts (filter evt-ended? (:active @s))]
    (doall (map #((:end %)) evts))
    (active- evts)
    (past+ evts)))

(defn update-queued-evts
  "calls :update-active fn of active events" []
  (doseq [evt (:queue @s)]
    (when (contains? evt :update-queued) ((:update-queued evt)))))

(defn update-active-evts
  "calls :update-active fn of active events" []
  (doseq [evt (:active @s)]
    (when (contains? evt :update-active) ((:update-active evt)))))

(defn game-loop [obj key]
  (dequeue-missed-evts)
  (activate-ready-evts)
  (deactivate-past-evts)
  (update-queued-evts)
  (update-active-evts)
  )

; TODO figure out how to run this all on another thread for higher resolution timing
(if-not (hook (object-named "App") :update :scheduler)
  (hook+ (object-named "App") :update :scheduler #'game-loop))

;(hook- (object-named "App") :update :default)

(start)
(elapsed)

(defn loop-queue []
  (queue+ {
           :t0       (+ 1 (elapsed))
           :duration 1/2
           :start    #(do
                        (loop-queue)
                        (log "loopy-start"))
           :end      #(log "loopy-end")
           }))
;(loop-queue)

(queue+
  {:t0 5 :duration 1 :start #(log "a-start") :end #(log "a-end")}
  {:t0 6 :duration 1 :start #(log "b-start") :end #(log "b-end")}
  {:t0 7 :duration 1 :start #(log "c-start") :end #(log "c-end")}
  {:t0 8 :duration 1 :start #(log "d-start") :end #(log "d-end")}
  )

(:queue @s)
(:active @s)
(count (:past @s))
