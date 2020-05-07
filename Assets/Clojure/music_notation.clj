







(ns music-notation
  (use arcadia.core
       arcadia.linear
       clojure.set)
  (:use [music-glyphs :only [glyphs]])
  (:import (UnityEngine Color GameObject Mesh MeshFilter MeshRenderer Mathf TextMesh Vector2 Vector3))
  )

(def lexicon "examples and definitions of terms used"
  {
   :note-value "1/2 - the rhythmic length of a note event"
   :note "60 - the MIDI number corresponding to pitch"
   :pitch ":c4 - a keyword corresponding to pitch"
   :pitch-class ":c# - a keyword corresponding to pitch class with no octave"
   :natural-pitch-class ":c - a keyword corresponding to a pitch-class with no accidental"
   :accidental "one of :# :b :n"
   :octave "4 and integer indicating the octave of a pitch"
   :glyph ":note-2-up - a keyword specifying the font character to render"
   :staff ""
   ;:staff-grid "?"
   :x "position on x-dimension of staff-grid"
   :y "position on y-dimension of staff-grid"
   :t "position on y-dimension of staff-grid, offset from t0"

   })


; Note data generation
; Note data generation
; Note data generation
; Note data generation

(defn- note->pitch-class-b-bias
  "returns pitch class given note, with flats as accidentals"
  [note]
  (nth (cycle [:c :db :d :eb :e :f :gb :g :ab :a :bb :b]) note))

(defn- note->pitch-class-#-bias
  "returns pitch class given note, with sharps as accidentals"
  [note]
  (nth (cycle [:c :c# :d :d# :e :f :f# :g :g# :a :a# :b]) note))

(defn- octave-of-note
  "returns the octave # of a given note see: https://en.wikipedia.org/wiki/Scientific_pitch_notation#Table_of_note_frequencies"
  [note]
  (- (int (/ note 12)) 1))

(defn- notes-in-key
  "returns all notes in the major key of the given note"
  [note]
  (->> (cycle [2 2 1 2 2 2 1])
       (reductions + (- (mod note 12) 12))
       (drop-while #(< % 0))
       (take-while #(< % 128))))

(def notes-in-c
  "a set of all notes in key of c"
  (set (notes-in-key 0)))


; Keyword manipulation
; Keyword manipulation
; Keyword manipulation
; Keyword manipulation

(defn- pitch->data
  ":c#4 {:pc-natural :c, :acc #, :oct 4}"
  [pitch]
  (let [s (name pitch)
        pc-natural (subs s 0 1)
        acc-or-oct (subs s 1 2)
        acc (if (= 2 (count s))
              nil
              (keyword acc-or-oct))
        oct (int (str (last s)))]
    {:pc-natural pc-natural
     :acc        acc
     :oct        oct
     }))

(defn- strip-octave
  "returns the pitch class of provided pitch"
  [pitch]
  (keyword (clojure.string/reverse
             (subs
               (clojure.string/reverse (name pitch))
               1))))

(defn- strip-accidental
  "returns the natural pitch class of provided pitch-class"
  [pitch-class]
  (keyword (subs (name pitch-class) 0 1)))

(defn- extract-accidental
  "returns the accidental or nil"
  [pitch-class]
  (if (< (count (name pitch-class)) 2)
    nil
    (keyword (subs (name pitch-class) 1 2))))





(def keys-data ; TODO cb and c# ??
  ; to get the accidentals in each key, we walk in either direction on the circle of 5ths
  (->> [:sharp :flat]
       (map
         (fn [bias]
           (let [
                 [note->pitch-class interval]
                 (case bias ;either prefer sharp or flat when spelling notes
                   :flat [note->pitch-class-b-bias 5] ; prefer flats, walk by 4ths
                   :sharp [note->pitch-class-#-bias 7]) ; prefer sharps, walk by 5ths

                 circle-order ; order of notes around the circle of 5ths
                 (->> (range 12)
                      (map #(mod (* interval %) 12))
                      (map note->pitch-class))
                 ]
             (->> (range 7) ; 7 keys in each direction (c is duplicated then deduped later)
                  (map #(mod (* interval %) 12))
                  (map (fn [note]
                         (let [key (note->pitch-class note)

                               accidentals
                               (->> (notes-in-key note) ;all notes in this note's key
                                    (take 7) ; there are only 7 pitch classes in a key
                                    (map #(when-not ; a note not in the key of c is an accidental
                                            (contains? notes-in-c %)
                                            (note->pitch-class %))) ; get its pitch-class
                                    (filter identity) ; filter nil (non-accidentals)
                                    (set) ; it's not in the order we want. more useful as a set
                                    )

                               accidentals-renamed
                               (case key ; when the key...
                                 :f# (union #{:e#} accidentals) ; add :e#
                                 :gb (union #{:cb} accidentals) ; add :cb
                                 accidentals ; otherwise leave it alone
                                 )

                               circle-order-renamed
                               (->> circle-order
                                    (map #(cond
                                            (and (= key :f#) (= % :f)) :e#
                                            (and (= key :gb) (= % :b)) :cb
                                            true %)))

                               accidentals-ordered
                               (->> circle-order-renamed
                                    (filter #(contains? accidentals-renamed %)))

                               note-spellings
                               (->> (range 128)
                                    (map
                                      #(let [
                                             pc
                                             (note->pitch-class %)

                                             spelling
                                             (cond
                                               ; corrections for :f# and :gb
                                               (and (= key :f#) (= pc :e))
                                               :en

                                               (and (= key :f#) (= pc :f))
                                               :e

                                               (and (= key :gb) (= pc :c))
                                               :cn

                                               (and (= key :gb) (= pc :b))
                                               :c

                                               ; accidentals are spelled without accidental
                                               (contains? accidentals pc)
                                               (strip-accidental pc)

                                               ; base pc of accidentals are spelled "natural"
                                               (contains?
                                                 (set (map strip-accidental accidentals))
                                                 pc)
                                               (keyword (str (name (strip-accidental pc))
                                                             "n"))

                                               ;otherwise leave it alone
                                               true pc)

                                             octave
                                             (octave-of-note %)
                                             ]
                                         (keyword (str (name spelling) octave))))
                                    (apply vector) ; faster lookup as vector
                                    )
                               ]
                           [key
                            {
                             :accidentals accidentals-ordered
                             :spellings   note-spellings
                             }])))))))
       (flatten) ; combine both sequences. the key of c will be present in both.
       (apply hash-map) ; turn it into a map so we can dedupe and look up data by key
       )
  )


(defn- spell-note
  "returns the spelling of the note in given key :c#4"
  [key note]
  (nth (:spellings (key keys-data)) note))


(def natural-pitch->staff-y
  "a map of pitches in the key of c to vertical positions on staff :c4 -> 0"
  (->> (notes-in-key 0)
       (map-indexed
         #(apply vector
                 [(spell-note :c %2)
                  (- %1 35) ; :c4 = 60 = index 35 of notes in key of c
                  ]))
       (flatten)
       (apply hash-map)
       ))


; Create Game Objects
; Create Game Objects
; Create Game Objects
; Create Game Objects

(def game-objects
  "an atom containing a map of GameObjects we control"
  (atom {
         :staff          #{}
         :key-signature  #{}
         :time-signature #{}
         :notes          #{}
         :played-notes   #{}
         }))

(defn- game-objects+
  "adds game-objects to the game-object atom"
  [category go-or-gos]
  (swap! game-objects
         update-in
         [category]
         #(union % (set (flatten [go-or-gos])))))

(defn- game-objects-
  "removes game-objects from the game-object atom and destroys them"
  [category go-or-gos]
  (let [gos (set (flatten [go-or-gos]))]
    (swap! game-objects
           update-in
           [category]
           #(difference % gos))
    (doseq [go gos]
      (destroy go))))

(defn- game-objects-clear
  "clears game-objects from the scene and from the game-object atom"
  [category]
  (doseq [go (category @game-objects)]
    (destroy go))
  (swap! game-objects
         assoc-in
         [category]
         #{}))

(defn- +glyph
  "creates a glyph at (x,y,0). returns the GameObject"
  [glyph x y]
  (let [go (instantiate (object-named "GlyphTemplate"))
        tm (cmpt go TextMesh)
        dx 1 ;smallest unit in x-dimension for rendering glyphs
        dy 0.25 ;smallest unit in y-dimension for rendering glyphs
        ]
    (set! (.. go transform position) (v3 (* x dx)
                                         (* y dy) ;center on middle c = 0
                                         0))
    (set! (. go name) (str glyph))
    (set! (. tm color) (Color. 0. 0. 0. 0.6))
    (set! (. tm text) (str (glyph glyphs)))
    go
    ))

; Trying to make the glyph from scratch but seems font won't load dynamically
;(let [obj (GameObject. "abc")
;      tm (cmpt+ obj UnityEngine.TextMesh)]
;  (set! (.. obj transform localScale) (v3 0.2))
;  (set! (. tm fontSize) (int 100))
;  (set! (. tm font) (UnityEngine.Resources/Load "Bravura"))
;  (set! (. tm text) (str \uE050))
;  )

(defn- +staff [n y]
  (doall (->> (range (* 3 n))
              (map #(+glyph :staff-5 % y)))))

(defn- +grand-staff
  "adds grand-staff to the scene and to the :staff category of the game-objects atom"
  []
  (let [game-objects [
                      (+glyph :staff-5 -2 2)
                      (+glyph :staff-5 -1 2)
                      (+glyph :clef-g -2 4)

                      (+glyph :staff-5 -2 -10)
                      (+glyph :staff-5 -1 -10)
                      (+glyph :clef-f -2 -4)

                      (+staff 16 2)
                      (+staff 16 -10)]]
    (game-objects+ :staff game-objects)))

(defn- +keysig
  "creates the glyphs of the given key signature within a vertical range on the staff"
  [key min-staff-y max-staff-y]
  (doall
    (->> (:accidentals (key keys-data))
         (map-indexed
           #(let [pitch-class (nth (str %2) 1)
                  y (->> (range 6)
                         (map (fn [o] (natural-pitch->staff-y (keyword (str pitch-class o)))))
                         (filter (fn [y] (and (>= y min-staff-y)
                                              (<= y max-staff-y))))
                         (first))
                  x %1
                  accidental (case (last (str %2))
                               \# :sharp
                               \b :flat)]
              (+glyph accidental x y))))))

(defn- +keysig-treble [key]
  (+keysig key 4 12))

(defn- +keysig-bass [key]
  (+keysig key -10 -3))

(defn- +keysig-grand [key]
  (let [game-objects [
                      (+keysig-treble key)
                      (+keysig-bass key)]]
    (game-objects+ :key-signature game-objects)))

(defn- +time-signature
  "adds time-signature to the scene and to game-objects. timesig is a vector e.g. [3 4]"
  [[beats-per-measure note-value-of-beat]]
  (let [x (+ 1 (count (:accidentals (:f# keys-data)))) ; TODO: double digits
        glyph-top (keyword (str "timesig-" beats-per-measure))
        glyph-bot (keyword (str "timesig-" note-value-of-beat))
        game-objects [
                      (+glyph glyph-top x 8)
                      (+glyph glyph-bot x 4)
                      (+glyph glyph-top x -4)
                      (+glyph glyph-bot x -8)]]
    (game-objects+ :time-signature game-objects)))

(defn +bar [x]
  "creates a bar at x"
  [(+glyph :bar-single x 2)
   (+glyph :bar-single x -10)
   (+glyph :bar-short x -6)])

(def x0 ;TODO: make this actually 0 and put key/time-sig in negatives
  "the zero-line"
  (+ 3 (count (:accidentals (:f# keys-data)))))

(defn- +zero-line []
  "creates a bar at the zero line"
  (game-objects+ :staff (+bar (- x0 1)))
  )

(defn- +note-raw ; TODO: offset when minor second notes would be in same position
  "creates a note on the staff with optionally accidental and dot"
  [t y note-value accidental dotted category]
  (let [x (+ x0 (* 3 t))
        glyph-gos [
                   (when accidental (+glyph (case accidental
                                              :b :flat
                                              :n :natural
                                              :# :sharp)
                                            x
                                            y))
                   (+glyph (case note-value
                             1 :note-1
                             1/2 :note-2-up
                             1/4 :note-4-up
                             1/8 :note-8-up
                             1/16 :note-16-up
                             1/32 :note-32-up
                             )
                           (+ 1 x) ; make room for accidental
                           y)
                   (when dotted (+glyph :note-dot (+ 2 x) y))
                   (when (or (= 0 y) (> y 10) (< y -10))
                     (doall (->> (cond
                                   (> y 10) (range 12 (if (odd? y) y (+ 1 y)) 2)
                                   (< y -10) (range (if (even? y) y (+ 1 y)) -10 2)
                                   (= y 0) [0])
                                 (map #(+glyph :ledger (+ 1 x) %)))))
                   ]
        go (GameObject. (str "note-"
                             note-value "-"
                             y "-"
                             accidental "-"
                             (when dotted "o")))
        ]
    (set! (.. go transform position) (v3 x 0 1)) ; not setting y here because this is grid-y. actual translation y is calculated in +glyph based on dY. TODO: formalize distinction between grid x,y and translation x,y,z
    (doseq [glyph-go (filter identity (flatten glyph-gos))]
      (child+ go glyph-go true))
    (game-objects+ category go)
    go
    ))

(def s (atom {
              :key-signature :f#
              :time-signature [3 4]
              }))

; TODO: note 0 produces "no matching clause" error
(defn +note
  "creates a note spelled and positioned on the staff. returns the GameObject containing the glyphs for the note"
  ([note t category]
   (let [pitch (spell-note (:key-signature @s) note)
         {pc-natural :pc-natural
          acc        :acc
          oct        :oct
          } (pitch->data pitch)
         natural-pitch (keyword (str (name pc-natural) oct))
         ]
     (+note-raw t
                (natural-pitch natural-pitch->staff-y)
                1
                acc
                false
                category
                )))
  ([note t] (+note note t :notes)))

(defn -note
  "removes note from registry and scene"
  [go]
  (game-objects- :notes go))

(defn clear-notes "clears all notes from staff" []
  (game-objects-clear :notes))


(defn set-played-notes! [notes->vels]
  (game-objects-clear :played-notes)
  (doall (->> notes->vels
              (map #(+note (first %) 0 :played-notes)))))

(defn set-keysig! [key]
  (game-objects-clear :key-signature)
  (swap! s assoc :key-signature key)
  (+keysig-grand key))

; TODO: schedule a set-timesig! upcoming timesig is rendered in time in meter and swaps timesig when crossing the zero line
(defn set-timesig! [timesig]
  (game-objects-clear :time-signature)
  (swap! s assoc :time-signature timesig)
  (+time-signature timesig))


(do (+grand-staff)
    (+zero-line)
    (set-timesig! [4 4]))

(comment
  (do (+grand-staff)
      (+zero-line)
      (set-keysig! :f) ; when setting, must reset notes
      (set-timesig! [4 8])
      (doall (->> (range 12 40 12)
                  (map #(+bar %))))
      ))


(comment
  (do (game-objects-clear :staff)
      (game-objects-clear :key-signature)
      (game-objects-clear :time-signature)
      (game-objects-clear :notes)
      )
  )

(comment
  (defn- set-notes! [notes]
    (game-objects-clear :notes)
    (doall (->> notes
                (map-indexed #(+note %2 %1 :notes)))))

  (set-notes! (reductions + 60 [2 2 1 2 2 2 1 2 2 1 2 2 2 1]))

  (spell-note :f# 60)

  (:accidentals (:cb keys-data))

  (do
    (+glyph :ledger 2 0)
    (+glyph :ledger 2 12)

    (+glyph :flat 0 0)
    (+glyph :note-1 1 0)

    (+note-raw 3 2 1/2 :flat true)

    (->> (range 10)
         (map #(+note-raw % (- % 5) 1 :flat false)))

    (->> (range 10)
         (map #(+note-raw % (+ % 5) 1 :sharp false)))

    (+glyph :flat 2 1)
    (+glyph :note-1 3 1)
    ))

