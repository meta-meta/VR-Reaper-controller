(ns geometry
  (use arcadia.core
       arcadia.linear)
  (:import (UnityEngine GameObject Mesh MeshFilter MeshRenderer Mathf Vector2 Vector3)))


(defn new-go-mesh "returns a new GameObject with mesh"
  ([] (let [go (GameObject.)
            mf (cmpt+ go MeshFilter)
            mr (cmpt+ go MeshRenderer)]
        (set! (.. mf mesh) (Mesh.))
        go))
  ([go-name] (let [go (new-go-mesh)]
               (set! (.. go name) go-name)
               go)))

(defn mesh "returns mesh given GameObject or name of GameObject"
  [go-or-name]
  (let [go (if (string? go-or-name)
             (object-named go-or-name)
             go-or-name)]
    (.. (cmpt go MeshFilter) mesh)))

(defn update-mesh! "clears the mesh and sets verts, uvs and tris given a mesh or GameObject"
  [go-or-mesh verts uvs tris]
  (let [is-mesh (= Mesh (type go-or-mesh))
        mesh (if is-mesh go-or-mesh (mesh go-or-mesh))]
    (.. mesh (Clear))
    (set! (.. mesh vertices) (into-array Vector3 verts))
    (set! (.. mesh uv) (into-array Vector2 uvs))
    (set! (.. mesh triangles) (into-array Int32 tris))))


;Polygon
(def TWO_PI (* 2 (.. Mathf PI)))

(defn verts-2d->v3-xy
  "returns a list of Vector3 from verts-2d on x-y-plane"
  [verts-2d]
  (->> verts-2d
       (map (fn [[x y]] (v3 x 0 y)))))

(defn verts-2d->v2
  "returns a list of Vector2 from verts-2d"
  [verts-2d]
  (->> verts-2d
       (map #(apply v2 %))))

(defn polygon-verts-2d "returns a list of 2d vectors: vertices(including center as last vertex) of a unit polygon with n sides centered at [0,0]"
  [sides]
  (->> (range sides)
       (map
         (fn [n]
           [(Mathf/Cos (* (/ TWO_PI sides) n))
            (Mathf/Sin (* (/ TWO_PI sides) n))
            ]))
       (cons [0 0])
       (reverse)))

(defn polygon-uvs
  "returns a list of Vector2s representing uv coordinates for a polygon of n sides"
  [sides]
  (verts-2d->v2 (polygon-verts-2d sides)))

(defn polygon-tris "returns a list of ints representing triangles consisting of vertex indices, considering the center vertex"
  [sides]
  (->>
    (range sides)
    (map (fn [n]
           [n
            (mod (+ 1 n) sides)
            sides ;center vertex
            ]))
    flatten))

(defn polygon-tris-flipped "returns a list of ints representing triangles(normals flipped) consisting of vertex indices, considering the center vertex"
  [sides]
  (->>
    (polygon-tris sides)
    (partition 3)
    (map reverse)
    flatten))

(defn generate-polygon-mesh! "updates mesh with geometry for polygon of n sides"
  [mesh sides]
  (let [verts-2d (polygon-verts-2d sides)]
    (update-mesh! mesh
                  (verts-2d->v3-xy verts-2d)
                  (verts-2d->v2 verts-2d)
                  (polygon-tris sides))))

(defn polygon
  "returns a GameObject containing a n-sided polygon of radius 1 on the xy plane"
  ([sides]
   (let [go (new-go-mesh (str "poly-" sides))
         mesh (mesh go)]
     (generate-polygon-mesh! mesh sides)
     go))
  ([go sides]
   (generate-polygon-mesh! (mesh go) sides)
   go))

(comment ;usage
  (new-go-mesh "heyo") ;new GameObject named "heyo" which has been added to the scene
  (mesh (object-named "heyo")) ;the mesh attached to the GameObject named "heyo"
  (mesh "heyo") ;the mesh attached to the GameObject named "heyo"
  (polygon-verts-2d 3) ;([-0.4999999 -0.8660254] [-0.5000001 0.8660254] [1.0 0.0] [0 0])
  (verts-2d->v3-xy [[0 1][2 3]]) ;(#unity/Vector3 [0.0 0.0 1.0] #unity/Vector3 [2.0 0.0 3.0])
  (verts-2d->v2 [[0 1][2 3]]) ;(#unity/Vector2 [0.0 1.0] #unity/Vector2 [2.0 3.0])
  (polygon-tris 3) ;(0 1 3 1 2 3 2 0 3)
  (polygon-tris-flipped 3) ;(3 1 0 3 2 1 3 0 2)
  (polygon 5) ;new GameObject named "poly-5" with a pentagon or radius 1 on xy plane
  )


;; Dodecahedron TODO: move to its own namespace

(defn dod-verts
  "returns the 20 vertices of a dodecahedron with radius r"
  [r]
  (let [
        phi (/ (+ 1 (Mathf/Sqrt 5)) 2)
        sqrt3 (Mathf/Sqrt 3)
        a (/ r sqrt3)
        -a (* a -1)
        b (/ r (* sqrt3 phi))
        -b (* b -1)
        c (/ (* r phi) sqrt3)
        -c (* c -1)]
    [[ a  a  a] [-a  a  a] [ a -a  a] [ a  a -a]
     [-a -a  a] [ a -a -a] [-a  a -a] [-a -a -a]
     [ 0  b  c] [ 0 -b  c] [ 0  b -c] [ 0 -b -c]
     [ b  c  0] [-b  c  0] [ b -c  0] [-b -c  0]
     [ c  0  b] [ c  0 -b] [-c  0  b] [-c  0 -b]]))

(def pents
  "the indices of vertices making up each pentagon of a dodecahedron"
  [[0 8 1 13 12] [0 12 3 17 16] [0 16 2 9 8]
   [1 8 9 4 18] [1 18 19 6 13] [3 12 13 6 10]
   [2 14 15 4 9] [2 16 17 5 14] [3 10 11 5 17]
   [4 15 7 19 18] [5 11 7 15 14] [6 19 7 11 10]])

(defn pentagon
  "returns a GameObject"
  [i verts-v3 uvs tris]
  (let [go (new-go-mesh (str "pent-" i))]
    (update-mesh! go verts-v3 uvs tris)
    go))

(defn add-vecs [[a1 b1 c1] [a2 b2 c2]] [(+ a1 a2) (+ b1 b2) (+ c1 c2)])
(defn div-vec-scalar [v s] (map #(/ % s) v))
(defn centroid [vecs]
  (div-vec-scalar
    (reduce add-vecs vecs)
    (count vecs)))
(defn mult-vec-scalar [v s] (map #(* % s) v))
(defn sum-of-squares [v] (reduce #(+ %1 (* %2 %2)) 0 v))
(defn magnitude [v] (Mathf/Sqrt (sum-of-squares v)))
(defn normalize [v] (div-vec-scalar v (magnitude v)))

(defn dodecahedron
  ([r faces-in sphere]
   (map (fn [pent i]
          (let [
                all-verts (dod-verts r)
                verts (map #(nth all-verts %) pent)
                center (centroid verts)
                sphere-center (mult-vec-scalar
                                (normalize center)
                                r)
                verts-v3 (map #(apply v3 %)
                              (concat
                                verts
                                [(if sphere
                                   sphere-center
                                   center)]))
                uvs (polygon-uvs 5)
                tris ((if faces-in
                        polygon-tris
                        polygon-tris-flipped)
                       5)
                ]
            (pentagon i verts-v3 uvs tris)
            ))
        (take 12 pents) (range)))
  ([r] (dodecahedron r false false))
  ([] (dodecahedron 1))
  )
