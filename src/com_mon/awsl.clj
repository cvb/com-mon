(ns com-mon.awsl
  (:require [soc-fetch.core :as c]
            [cheshire.core :as json]
            [uswitch.lambada.core :refer [deflambdafn]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [amazonica.aws.s3 :as s3]))

(deflambdafn awsl.StoreVkL
  [in out ctx]
  (let [path "vk-l/updates"
        bucket "s-stuff"
        last (c/fetch-last-vk)
        fname (c/store-vk-l "/tmp" last)]
    (s3/put-object
      :bucket-name bucket
      :key         (str path "/" fname)
      :file        (str "/tmp/" fname))))
