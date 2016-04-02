(ns com-mon.vk
  (:refer-clojure :exclude [get])
  (:require [clojure.string           :as s]
            [org.httpkit.client       :as http]
            [cheshire.core            :as json]
            [clojure.core.match       :as m]
            [com-mon.threader         :as t]))

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
   (println "getting" method params opts)
   (t/fail-> (get-raw method (merge params {:v "5.50"}) opts)
     parse-response
     process-vk-resp
     ((fn [v] [:success (:response v)]))))

  ([method params] (get method params {})))

(defmacro defget
  [name params]
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
        (get ~name (merge ~'prms ~params))))))

(defget :groups.getById {:group_id name})

(defget :wall.get {:owner_id owner-id})

(defget :wall.getComments {:owner_id owner-id :post_id  post-id})

(defget :likes.getList {:owner_id owner-id
                        :item_id  item-id
                        :type     "comment"
                        :filter   "likes"})

(defn l-likes [gid comment]
  (t/fail-> (likes-getlist gid (:id comment))
    ((fn [v] [:success {:comment comment :likes (-> v :items)}]))))

(defn l-comments [post]
  (let [gid (:owner_id post)]
    (if (> 0 (-> post :comments :count))
      []
      (t/fail-> (wall-getcomments gid (:id post))
        ((fn [v] [:success
                  {:post post
                   :comments (map #(l-likes gid %) (:items v))}]))))))

(defn l-posts []
  (t/fail-> (groups-getbyid "oldlentach")
    ((fn [v] [:success (-> v first :id -)]))
    wall-get
    ((fn [v] [:success (map l-comments (:items v))]))))
