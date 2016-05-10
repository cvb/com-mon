(ns com-mon.main
  (:gen-class)
  (:require [clojure.tools.cli :as c]
            [com-mon.core      :as core :refer [get-vk-l-s3]]))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
    (clojure.string/join \newline errors)))

(defn usage [options-summary]
  (->> ["This is my program. There are many like it, but this one is mine."
        ""
        "Usage: program-name action [options]"
        ""
        "Options:"
        options-summary
        ""
        ""
        "Please refer to the manual page for more information."]
    (clojure.string/join \newline)))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]}
        (c/parse-opts args [["-h" "--help"]])

        ns (find-ns 'com-mon.main)
        action (nth args 0)]
    (cond
      (:help options) (usage summary)
      (not= (count args) 1) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))
    (if-let [f (->> action clojure.string/trim symbol (ns-resolve ns))]
      (f)
      (prn "Unknown action" action ns))))
