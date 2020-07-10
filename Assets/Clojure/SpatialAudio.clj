(ns spatial-audio
  (:use [arcadia.core] [arcadia.linear])
  (:import (UnityEngine GameObject ParticleSystem Resources))
  (:import AudioObjectIEM))



(if (nil? (object-named "Speakers")) (GameObject. "Speakers"))

(defn scene-clear []
  (doall (map #(GameObject/Destroy %) 
              (children (object-named "Speakers")))))

(scene-clear)

(defn speaker- [name]
  (speaker- (GameObject/Destroy (object-named name))) )


;(defn child++ [parent-path go]) ; TODO add go as child to parent-path, creating ancestor objects if they do not exist

(defn speaker+ [name color pos osc-port]
  (let [speak (GameObject/Instantiate (Resources/Load "Speaker"))
        [x y z] pos
        iemCmpt (cmpt speak AudioObjectIEM)]    
    (child+ (object-named "Speakers") speak)
    (set! (.. speak name) name)
    (.. iemCmpt (SetColor color))
    (set! (.. iemCmpt PortOut) (int osc-port))
    (set! (.. speak transform localPosition) (v3 x y z))
    speak))




(def scenes {:ch2
             {:sc1 {:sceneRotator { :oscPort 8050}
                    :tracks {:leigh {:oscPort 8051
                                     :room 1 }
                             :roger {:oscPort 8052
                                     :room 1}
                             :zeb {:oscPort 8053
                                   :room 1}}
                    }}})

(defn scene-load [chapter scene-key]
  (let [tracks (get-in scenes [chapter scene-key :tracks])]
    (->>
     (seq tracks) 
     (map-indexed (fn [i [track-key {osc-port :oscPort}]]
                    (let [theta (* i (/ (* 2 Mathf/PI) (count tracks)))
                          r 0.4]
                      (speaker+ (name track-key)
                                i
                                [(* r (Mathf/Cos theta)) 1 (* r (Mathf/Sin theta))]
                                osc-port)))))))

(scene-clear)
(scene-load :ch2 :sc1)

(defn scene-load [])

(speaker+ "leigh" 0 [0 0 1] 8051)
(speaker+ "roger" 1 [1 0 0] 8052)
(speaker+ "zeb" 2 [1 0 1] 8053)




#_(GameObject/Destroy (object-named "AmbisonicVisualizer"))

(def ambisonic-viz
  #_(GameObject. "AmbisonicVisualizer")
  (object-named "AmbisonicVisualizer"))

(def particles 
  #_(cmpt+ ambisonic-viz ParticleSystem)  ; TODO: set material
  (cmpt ambisonic-viz ParticleSystem))

#_(.. particles (Clear))





                                       
 ; pick first track of room to control room dimensions and listener position
      
