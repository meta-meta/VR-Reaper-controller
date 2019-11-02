;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

; Author: Stephen C. Gilardi

;;  clojure.test-clojure.printer
;;
;;  scgilardi (gmail)
;;  Created 29 October 2008

(ns clojure.test-clojure.printer
  (:use clojure.test))

(deftest print-length-empty-seq
  (let [coll () val "()"]
    (is (= val (binding [*print-length* 0] (print-str coll))))
    (is (= val (binding [*print-length* 1] (print-str coll))))))

(deftest print-length-seq
  (let [coll (range 5)
        length-val '((0 "(...)")
                     (1 "(0 ...)")
                     (2 "(0 1 ...)")
                     (3 "(0 1 2 ...)")
                     (4 "(0 1 2 3 ...)")
                     (5 "(0 1 2 3 4)"))]
    (doseq [[length val] length-val]
      (binding [*print-length* length]
        (is (= val (print-str coll)))))))

(deftest print-length-empty-vec
  (let [coll [] val "[]"]
    (is (= val (binding [*print-length* 0] (print-str coll))))
    (is (= val (binding [*print-length* 1] (print-str coll))))))

(deftest print-length-vec
  (let [coll [0 1 2 3 4]
        length-val '((0 "[...]")
                     (1 "[0 ...]")
                     (2 "[0 1 ...]")
                     (3 "[0 1 2 ...]")
                     (4 "[0 1 2 3 ...]")
                     (5 "[0 1 2 3 4]"))]
    (doseq [[length val] length-val]
      (binding [*print-length* length]
        (is (= val (print-str coll)))))))

(deftest print-level-seq
  (let [coll '(0 (1 (2 (3 (4)))))
        level-val '((0 "#")
                    (1 "(0 #)")
                    (2 "(0 (1 #))")
                    (3 "(0 (1 (2 #)))")
                    (4 "(0 (1 (2 (3 #))))")
                    (5 "(0 (1 (2 (3 (4)))))"))]
    (doseq [[level val] level-val]
      (binding [*print-level* level]
        (is (= val (print-str coll)))))))

(deftest print-level-length-coll
  (let [coll '(if (member x y) (+ (first x) 3) (foo (a b c d "Baz")))
        level-length-val
        '((0 1 "#")
          (1 1 "(if ...)")
          (1 2 "(if # ...)")
          (1 3 "(if # # ...)")
          (1 4 "(if # # #)")
          (2 1 "(if ...)")
          (2 2 "(if (member x ...) ...)")
          (2 3 "(if (member x y) (+ # 3) ...)")
          (3 2 "(if (member x ...) ...)")
          (3 3 "(if (member x y) (+ (first x) 3) ...)")
          (3 4 "(if (member x y) (+ (first x) 3) (foo (a b c d ...)))")
          (3 5 "(if (member x y) (+ (first x) 3) (foo (a b c d Baz)))"))]
    (doseq [[level length val] level-length-val]
      (binding [*print-level* level
                *print-length* length]
        (is (= val (print-str coll)))))))

(deftest print-dup-expected
  (are [x s] (= s (binding [*print-dup* true] (print-str x)))
       1 "1"
       1.0 "1.0"
       1N "1N"
	   (clojure.lang.BigInteger/Parse "1") "#=(clojure.lang.BigInteger/Parse \"1\")"             ;;; (java.math.BigInteger. "1") "#=(java.math.BigInteger. \"1\")"
       1M "1M"
       "hi" "\"hi\""))

(deftest print-dup-readable
  (are [form] (let [x form]
                (= x (read-string (binding [*print-dup* true] (print-str x)))))
       1
       1.0
       1N
       1M
       "hi"))
	   
(def ^{:foo :anything} var-with-meta 42)
(def ^{:type :anything} var-with-type 666)

(deftest print-var
  (are [x s] (= s (pr-str x))
       #'pr-str  "#'clojure.core/pr-str"
       #'var-with-meta "#'clojure.test-clojure.printer/var-with-meta"
       #'var-with-type "#'clojure.test-clojure.printer/var-with-type"))

(deftest print-meta
  (are [x s] (binding [*print-meta* true] 
               (let [pstr (pr-str x)]
                 (and (.EndsWith pstr s)                                    ;;; .endsWith
                      (.StartsWith pstr "^")                                ;;; .startsWith
                      (.Contains pstr (pr-str (meta x))))))                 ;;; .contains
       #'pr-str  "#'clojure.core/pr-str"
       #'var-with-meta "#'clojure.test-clojure.printer/var-with-meta"
       #'var-with-type "#'clojure.test-clojure.printer/var-with-type"))

#_(deftest print-throwable                                                  ;;; we don't get stack traces unelss an exception is thrown.
  (binding [*data-readers* {'error identity}]
    (are [e] (= (-> e Throwable->map)
                (-> e pr-str read-string))
         (Exception. "heyo")
         (Exception. "I can a throwable"                                  ;;; Throwable
                     (Exception. "chain 1"
                                 (Exception. "chan 2")))
         (ex-info "an ex-info" {:with "its" :data 29})
         (Exception. "outer"
                     (ex-info "an ex-info" {:with "data"}
                              (System.InvalidProgramException. "less outer"                       ;;; Error.
                                      (ex-info "the root"
                                               {:with "even" :more 'data})))))))

(deftest print-ns-maps
  (is (= "#:user{:a 1}" (binding [*print-namespace-maps* true] (pr-str {:user/a 1}))))
  (is (= "{:user/a 1}" (binding [*print-namespace-maps* false] (pr-str {:user/a 1}))))
  (let [date-map {:day 3, :date 31, :time 0, :month 11, :seconds 0, :year 69,  :timezoneOffset 360, :hours 18, :minutes 0}]  ;;; (bean (java.util.Date. 0))  -- don't have bean
    (is (= (binding [*print-namespace-maps* true] (pr-str date-map))
           (binding [*print-namespace-maps* false] (pr-str date-map))))))

(deftest print-symbol-values
  (are [s v] (= s (pr-str v))
             "##Inf" Double/PositiveInfinity        ;;; Double/POSITIVE_INFINITY
             "##-Inf" Double/NegativeInfinity       ;;; Double/NEGATIVE_INFINITY
             "##NaN" Double/NaN
             "##Inf" Single/PositiveInfinity        ;;; Float/POSITIVE_INFINITY
             "##-Inf" Single/NegativeInfinity       ;;; Float/NEGATIVE_INFINITY
             "##NaN" Single/NaN))                   ;;; Float/NaN