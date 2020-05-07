(do
  (do ; REPL convenience
    (use 'clojure.repl)
    (use '[clojure.pprint :only (pprint)])
    (defn ffn "find function - pretty prints the result of call apropos with given string"
      [s] (pprint (apropos s)))
    (comment ;usage
      (ffn "con") ;find fns containing "con"
      ))

  (import Spout.SpoutReceiver)
  (import Spout.Spout)

  (defn list-spout-senders []
    (map #(. % name) (.. Spout instance activeSenders)))

  (defn set-spout-sender [go-name i]
    (let [sr (cmpt (object-named go-name) SpoutReceiver)
          senders (list-spout-senders)]
      (set! (.. sr sharingName) (nth senders (mod i (count senders))))))

  ; (set-spout-sender "Cube3" 5)

  ; (methods SpoutReceiver)




  ;; handle OSC
  (def osc (atom {
                  :knobs (zipmap (range 1 33) (repeat 0))
                  :sines {1 {:f 0 :a 0}}
                  }))

  ;(get (:knobs @osc) 1)

  ;(def ob (polygon 5))


  (defn set-fov [camera-name fov]
    (let [cam (cmpt (object-named camera-name) "Camera")]
      (set! (.. cam fieldOfView) fov)))

  ; (set-fov "SpoutCam" 50)
  (defn handle-msg [msg]
    (let [[i v] (vec (.. msg (get_args)))]
      (swap! osc assoc-in [:knobs i] v)
      (when (= i 1) (set-fov "SpoutCam" v))
      (when (= i 2) (set-fov "CameraL" v))
      (when (= i 3) (set-fov "CameraR" v))
      (when (= i 4) (set-spout-sender "Cube" v))
      (when (= i 5) (set-spout-sender "Cube2" v))
      (when (= i 6) (set-spout-sender "Cube3" v))
      ))

  (defn handle-attack [msg]
    (let [[v] (vec (.. msg (get_args)))
          n (:attack @osc)]
      (swap! osc assoc :attack (inc n))
      (set-spout-sender "Cube" n)
      )
    )

  (defn on-sines [msg]
    (let [args (vec (.. msg (get_args)))
          [n freq amp] args]
      (swap! osc assoc-in [:sines n] { :f freq :a amp })))

  (defn on-bcr2000 [msg] (handle-msg msg))
  (defn on-attack [msg] (handle-attack msg))

  (def osc-go (object-named "OSC"))
  (def osc-in (cmpt osc-go "OscIn"))
  (.. osc-in (Map "/bcr2000" on-bcr2000))
  ; (.. osc-in (Map "/attack" on-attack))
  ; (.. osc-in (Map "/sines" on-sines))

  ; OSC out  
  (def osc-go (object-named "OSC"))
  (def osc-out (cmpt osc-go "OscOut"))
  (. osc-out (Open 8000 "127.0.0.1"))

  (defn send-random-notes [n]
    (->> (range 12)
         (map (fn [i]
                (. osc-out
                   (Send
                     (str "/" i)
                     (str (+ 32 (* n i))
                          " "
                          (rand-int 2))))))))

  (defn on-update []
    (send-random-notes 2))

  (hook+ osc-go :update #(on-update))

  (on-update)

(import UnityEngine.Time)
(.. Time time)
(def s (atom { :tick 0 }))



    ; state of inputs
  (def s
    (atom
      {
       :knobs (zipmap (range 1 33) (repeat 0))
       :space-mouse {
                     :go nil ;the GameObject to manipulate
                     :translation (v3 0)
                     :rotation (qt)
                     }
       :keys {:space false}}))

  ; (pprint (:space-mouse @s))


  ; SpaceNavigator
  (import SpaceNavigatorDriver.SpaceNavigator)
  (SpaceNavigator/SetRotationSensitivity 1)
  (SpaceNavigator/SetTranslationSensitivity 1)

  ; Keyboard
  (import UnityEngine.Input)
  (import UnityEngine.KeyCode)


  ; (defn u [go] ;translate/rotate
  ; 	(.. go transform (Translate (.. SpaceNavigator Translation)))
  ; 	(.. go transform (Rotate (.. SpaceNavigator Rotation eulerAngles))))

  (defn u [go]
    (swap! s assoc :space-mouse {
                                 :translation (.. SpaceNavigator Translation)
                                 :rotation (.. SpaceNavigator Rotation)})
    (swap! s assoc :keys {
                          :space (Input/GetKey (. KeyCode Space))
                          :a (Input/GetKey (. KeyCode A))
                          :b (Input/GetKey (. KeyCode B))
                          :c (Input/GetKey (. KeyCode C))
                          }))
  ; Track keyboard and spacemouse state
  ; (def state-obj (GameObject. "StateObj"))
  ; (hook+ state-obj :update #(u %))



  ; spawn a cube for each key
  ; move the cube if corresponding key is down while manipulating spacemouse
  ; (->> [:a :b :c :space]
  ;      (map 
  ;        #(let 
  ;           [
  ;            go (create-primitive :cube)
  ;            u (fn [go] 
  ;                (let 
  ;                  [
  ;                   {t :translation r :rotation} 
  ;                   (:space-mouse @s)

  ;                   k (get-in @s [:keys %])]
  ;                  (if k 
  ;                    (do  
  ;                      (set! 
  ;                        (.. go transform position) 
  ;                        t)
  ;                      (set! 
  ;                        (.. go transform rotation) 
  ;                        r)))))
  ;            ]
  ;           (hook+ go :update u))))


  ; (defn update-sphere [go] 
  ;   (let [{{f :f a :a} 1} (:sines @osc)]
  ;     (set! (.. go transform position) (v3 5 (* 100000 a) 0))
  ;     ))

  ; (let [go (create-primitive :sphere)]
  ;   (hook+ go :update #(update-sphere %))
  ;   )


  (defn mk-spheres [num]
    (let [keys (range 1 (+ num 1))]
      (zipmap keys (map (fn [k] (create-primitive :sphere)) keys))))

  (def spheres (mk-spheres 20))

  (defn on-sines [msg]
    (let [args (vec (.. msg (get_args)))
          [n freq amp] args
          sphere (spheres n)]
      (set! (.. sphere transform position)
              (v3 (* freq 0.01) (* 100 amp) 0))
      ))

  ; (Mathf/Pow 2 10)
  ; (->> (range 15) (map #(Mathf/Pow 2 %)) (map #(Mathf/Log % 2)))
  ; (methods Mathf)

  (.. osc-in (Map "/sines" #(on-sines %)))



      )







; TODO: sketch geometry for some spec types and functions
; (defnvr some-func "defines a fn and adds a geometric
; representation of it to the scene"
;  [args] (body))
; (def fnvrs (atom {"fn1" {:fn fn1 :go gameobj} ...}) )

; {:fn-vrs {"fn1" {:fn fn1 :go GameObject}}
;  :val-vrs {"val1" {:v 123 :go GameObject}}
;  :controls #{[:number-dial "val1"]}
;  :connections #{["val1" ["fn1" 0]]}}

; do this for vals (state) too. it all goes in one atom probably


; goal is to teach someone to program in a shared VR
; one shared camera position, a set of hands for each player
