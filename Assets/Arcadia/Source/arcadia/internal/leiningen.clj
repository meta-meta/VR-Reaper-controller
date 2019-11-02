(ns arcadia.internal.leiningen
  (:require [arcadia.internal.filewatcher :as fw]
            [arcadia.internal.asset-watcher :as aw]
            [arcadia.internal.state :as state]
            [arcadia.internal.file-system :as fs]
            [arcadia.internal.packages.data :as pd]
            [clojure.spec.alpha :as s]
            [arcadia.internal.spec :as as]
            [arcadia.internal.compiler :as compiler]
            [arcadia.internal.config :as config])
  (:import [System.Text.RegularExpressions Regex]
           [System.IO FileSystemInfo StringReader Directory DirectoryInfo Path]
           [UnityEditor AssetDatabase]
           [clojure.lang PushbackTextReader]))

;; ------------------------------------------------------------
;; grammar

(s/def ::dependencies (as/collude [] ::pd/dependency))

(s/def ::name string?)

(s/def ::version string?)

(s/def ::body map?)

(s/def ::source-paths
  (as/collude [] ::fs/path))

(s/def ::defproject
  (s/keys :req [::fs/path
                ::name
                ::pd/version
                ::dependencies
                ::body]))

(s/def ::project (s/keys :req [::fs/path ::defproject]))

(s/def ::projects (as/collude [] ::project))

(defn- read-all [s]
  (let [rdr (PushbackTextReader. (StringReader. s))
        opts {:read-cond :allow
              :eof ::eof}]
    (loop [exprs []]
      (let [next-expr (read opts rdr)]
        (if (= next-expr ::eof)
          exprs
          (recur (conj exprs next-expr)))))))

(defn- read-defproject [exprs]
  (->> exprs
       (filter seq?)
       (filter #(= 'defproject (first %)))
       first))

(defn- read-lein-project-file [file]
  (let [raw (slurp file)]
    (-> raw read-all read-defproject)))

(s/fdef project-file-data
  :ret ::defproject)

(defn project-file-data [pf]
  (let [[_ name version & body] (read-lein-project-file pf)
        body (apply hash-map body)]
    {::fs/path (fs/path pf)
     ::name name
     ::version version
     ::dependencies (vec (:dependencies body))
     ::body body}))

;; ============================================================
;; filesystem

;; if anyone can think of another way lemme know -tsg
(defn leiningen-project-file? [fi]
  (when-let [fi (fs/info fi)] ;; not sure this stupid function returns nil if input is already a filesysteminfo for a non existant filesystemthing
    (and (= "project.clj" (.Name fi))
         (boolean
           (re-find #"(?m)^\s*\(defproject(?:$|\s.*?$)" ;; shift to something less expensive
             (slurp fi))))))

(s/fdef project-data
  :ret ::project)

(defn project-data [dir]
  (let [dir (fs/directory-info dir)]
    (if (.Exists dir)
      (let [project-file (fs/file-info
                           (fs/path-combine dir "project.clj"))]
        (if (.Exists project-file)
          {::fs/path (fs/path dir)
           ::defproject (project-file-data project-file)}
          (throw
            (Exception.
              (str "No leiningen project file found at " (.FullName project-file))))))
      (throw
        (ArgumentException.
          (str "Directory " (.FullName dir) " does not exist"))))))

(defn- leiningen-structured-directory? [^DirectoryInfo di]
  (boolean
    (some leiningen-project-file?
      (.GetFiles di))))

(defn leiningen-project-directories []
  (->> (.EnumerateFiles
         (DirectoryInfo. (BasicPaths/BestGuessDataPath))
         "project.clj"
         System.IO.SearchOption/AllDirectories)
       (map #(.Directory %))
       (filter leiningen-structured-directory?)))

(s/fdef all-project-data
  :ret ::projects)

(defn all-project-data []
  (into []
    (map project-data)
    (leiningen-project-directories)))

;; ============================================================
;; loadpath

(s/fdef project-data-loadpath
  :args (s/cat :project ::project)
  :ret ::fs/path)

(defn project-data-loadpath [{{{arcadia-opts :arcadia, :as body} ::body} ::defproject,
                              p1 ::fs/path}]
  (let [{:keys [source-paths]} (merge
                                 (select-keys body [:source-paths])
                                 arcadia-opts)]
    (if source-paths
      (map (fn [p2]
             ; GetFullPath will convert directory separators; see issue #371
             (System.IO.Path/GetFullPath
               (Path/Combine p1 p2)))
        source-paths)
      [(Path/Combine p1 "src")])))

(defn leiningen-loadpaths []
  (->> (all-project-data)
       (mapcat project-data-loadpath)))

(defn leiningen-loadpaths-string []
  (clojure.string/join Path/PathSeparator
    (leiningen-loadpaths)))

(compiler/add-loadpath-extension-fn ::loadpath-fn #'leiningen-loadpaths-string)

;; ============================================================
;; hook up listener

(aw/add-listener ::fw/create-modify-delete-file ::config-reload
  (str Path/DirectorySeparatorChar "project.clj")
  (fn [{:keys [::fw/path]}]
    (when (leiningen-project-file? path)
      (config/update!))))
