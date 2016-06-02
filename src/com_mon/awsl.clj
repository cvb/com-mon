(ns com-mon.awsl
  (:require [com-mon.core :as c]
            [cheshire.core :as json]
            [uswitch.lambada.core :refer [deflambdafn]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [amazonica.aws.s3 :as s3]
            [taoensso.timbre :as log]))

(deflambdafn awsl.StoreVkL
  [in out ctx]
  (let [cfg (json/parse-string (slurp in) true)
        log-level (or (some->> cfg :log :level keyword) :info)]
    (log/with-level log-level
      (log/info "Starting, level:" log-level)
      (c/store-vk-l-s3))))
