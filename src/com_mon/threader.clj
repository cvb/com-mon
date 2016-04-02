(ns com-mon.threader
  (:require [clojure.core.match :as m]))

(def fail-fn (fn [f v]
               (m/match v
                 [:success v'] (f v')
                 [:fail _] v)))

(defmacro threader-builder
  [bind-fn apply expr & forms]
  (loop [x expr, fs forms]
    (if fs
      (let [v (gensym)
            f (first fs)
            threaded (list bind-fn `(fn [~v] (~apply ~v ~f)) x)]
        (recur threaded (next fs)))
      x)))

(defmacro fail->
  [expr & forms]
  `(threader-builder fail-fn -> ~expr ~@forms ))

(defmacro fail->>
  [expr & forms]
  `(threader-builder fail-fn ->> ~expr ~@forms ))

