(ns com-mon.vk
  (:refer-clojure :exclude [get])
  (:require [clojure.string           :as s]
            [org.httpkit.client       :as http]
            [cheshire.core            :as json]
            [clojure.core.match       :as m]
            [com-mon.threader         :as t]
            [clojure.zip              :as z]))

(def base-url "https://api.vk.com/method")

(defn get-raw [method params opts]
  (let [url (str base-url "/" (name method))
        resp (http/get url (merge opts {:query-params params}))]
    (let [{:keys [status body error] :as resp} @resp]
      (if error
        [:fail {:message (str "Failed to get" method "with" error)
                :type :get-failed
                :error {:method method
                        :params params
                        :opts opts
                        :response resp}}]
        [:success body]))))

(defn parse-response
  [s]
  (try
    [:success (json/parse-string s true)]
    (catch Exception e
      [:fail {:message (.getMessage e)
              :type :parse-failed
              :error e}])))

(defn process-vk-resp
  [r]
  (if (:error r)
    [:fail {:message (-> r :error :error_msg)
            :error (:error r)
            :type :processing-failed}]
    [:success r]))

(defn get
  ([method params opts]
   (t/fail-> (get-raw method (merge params {:v "5.50"}) opts)
     parse-response
     process-vk-resp
     ((fn [v] [:success (:response v)]))))

  ([method params] (get method params {})))

(defmacro defget
  ([name params] `(defget ~name ~params ~(fn [v#] [:success v#])))
  ([name params post-proc]
   (let [fname (-> name
                 s/lower-case
                 (s/replace "." "-")
                 (s/replace ":" "")
                 symbol)
         args (->> (vals params) (filter symbol?) vec)]
     `(defn ~fname
        (~args
         (~fname ~@args {}))
        (~(conj args 'prms)
         (t/fail-> (get ~name (merge ~'prms ~params))
           ~post-proc))))))

(defget :groups.getById {:group_id name})

(defn items [resp] [:success (:items resp)])

(defget :wall.get {:owner_id owner-id} items)

(defget :wall.getComments {:owner_id owner-id :post_id  post-id} items)

(defget :likes.getList {:owner_id owner-id
                        :item_id  item-id
                        :type     "comment"
                        :filter   "likes"}
  items)

(defn id-from-group [g] (-> g :id -))

(defn node-type
  [node]
  (->> node keys (filter #(not= :children %)) first))

(defn parent
  [loc type]
  (if-let [up (z/up loc)]
    (let [n (z/node up)]
      (if (and n (not (sequential? n)) (= type (node-type (z/node up))))
        (type n)
        (recur up type)))))

(defn group-id
  [loc]
  (-> (parent loc :group) id-from-group))
