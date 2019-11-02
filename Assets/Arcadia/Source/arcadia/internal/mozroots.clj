(ns ^{:doc "Fix for obscure https bug (#175, #245) on Windows and Linux

           Root SSL certificates are not installed by default on Windows,
           and Linux, so making web requests to HTTPS URLs will fail. This
           breaks our package manager. The solution is a command line tool
           called mozroots that downloads the root SLL certificates that
           Mozilla uses in Firefox. We load the assembly and simulate a
           command line session passing --import --sync as arguments."}
  arcadia.internal.mozroots
  (:import [Arcadia BasicPaths Shell]
           [System.IO Path]))

;; TODO fix hardcoded path here
(defn import-sync-mozroots []
  (Shell/MonoRun Shell/MozrootsExePath
                 (str "--file " (Path/Combine (BasicPaths/ArcadiaFolder) "Infrastructure" "certdata.txt") " --import --sync")))
