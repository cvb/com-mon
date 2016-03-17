(ns com-mon.vk
  (:refer-clojure :exclude [get])
  (:require [clojure.string           :as s]
            [org.httpkit.client       :as http]
            [cheshire.core            :as json]
            [cats.core                :refer [>>= extract lift-m return]]
            [cats.context             :as ctx]
            [cats.monad.exception     :as exc]))

(def base-url "https://api.vk.com/method")

(defn get-raw [method params opts]
  (let [url (str base-url "/" (name method))
        resp (http/get url (merge opts {:query-params params}))]
    (let [{:keys [status body error] :as resp} @resp]
      (if error
        (exc/failure {:method method :response resp}
                     (str "Failed to get" method "with" error))
        (exc/success body)))))

(defn parse-response
  [s]
  (exc/try-on (json/parse-string s true)))

(defn process-vk-resp
  [r]
  (if (:error r)
    (exc/failure (:error r) (-> r :error :error_msg))
    (exc/success r)))

(defn get
  ([method params opts]
   (ctx/with-monad exc/context
     (>>= (get-raw method (merge params {:v "5.50"}) opts)
          parse-response
          process-vk-resp
          (comp return :response))))

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

(defget :wall.get {:owner_id owner_id})

(defget :likes.getList {:owner_id owner-id
                        :item_id  item-id
                        :type     "comment"
                        :filter   "likes"})
