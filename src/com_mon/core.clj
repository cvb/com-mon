(ns com-mon.core
  (:gen-class)
  (:require [environ.core :refer [env]]
            [clojure.core.match :as m]
            [clojure.zip :as z]
            [clojurewerkz.neocons.rest :as nr]
            [clj-time.core :as tt]
            [clj-time.local :as tl]
            [clj-time.format :as tf]

            [com-mon.vk :as vk]
            [com-mon.fetcher :as f]
            [com-mon.db.neo4j :as neo]
            [amazonica.aws.s3 :as s3]))


(def vk-lentach-last-comments
  [{:group (fn [_] (vk/groups-getbyid "oldlentach"))
    :children
    [{:post (fn [loc] (vk/wall-get (vk/group-id loc) {:count 100}))
      :children
      [{:comment (fn [loc] (vk/wall-getcomments
                             (vk/group-id loc)
                             (-> (vk/parent loc :post) :id)
                             {:count 100}))
        :children
        [{:like (fn [loc] (vk/likes-getlist
                            (vk/group-id loc)
                            (-> (vk/parent loc :comment) :id)
                            {:count 100}))}]}]}]}])

(defn process-vk-fetched
  [data &{:keys [save-group save-post save-comment save-like]}]
  (loop [loc (f/zipper data)]
    (if (z/end? loc)
      (z/root loc)
      (if (sequential? (z/node loc))
        (recur (z/next loc))
        (do
          (m/match (vk/node-type (z/node loc))
            :group   (save-group   (z/node loc))
            :post    (save-post    (z/node loc) (-> loc z/up z/node))
            :comment (save-comment (z/node loc) (-> loc z/up z/node))
            :like    (save-like    (z/node loc) (-> loc z/up z/node)))
          (recur (z/next loc)))))))

(defn update-neo4j-vk [data]
  (let [conn (neo/conn)]
    (process-vk-fetched data
      :save-group   (partial neo/save-group conn)
      :save-post    (partial neo/save-post conn)
      :save-comment (partial neo/save-comment conn)
      :save-like    (partial neo/save-like conn))))

(defn fetch-last-vk []
  (let [[v fails] (f/run-sync vk-lentach-last-comments)]
    (if-not (empty? fails)
      (throw (ex-info "Some requests are failed" {:causes fails}))
      v)))

(defn get-vk-l-s3 []
  (let [lastf (neo/get-vk-l-last (neo/conn))
        all (->> (s3/list-objects :bucket-name :s-stuff :prefix "vk-l/updates")
              :object-summaries
              (map :key)
              sort
              reverse
              (take-while #(not= lastf %))
              reverse)]
    (doseq [[n e] (map vector (range) all)]
      (println (+ 1 n) "of" (count all) e)
      (-> (s3/get-object :bucket-name :s-stuff :key e)
        :input-stream
        slurp
        read-string
        update-neo4j-vk)
      (neo/set-vk-l-last (neo/conn) e))))
