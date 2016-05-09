(ns com-mon.fetcher
  (:require [clojure.zip :as z]
            [clojure.core.match :as m]))

(defmacro prep-fn
  [f let]
  (let* [a (gensym)
         letform (->> (partition 2 let)
                   (map (fn [[n f]] [n `(~f ~a)]))
                   (apply concat)
                   vec)]
    `(fn [~a]
       (let ~letform
         (~f ~a)))))

(defmacro nodes
  ([name fn form] `(nodes ~name ~fn {} ~form))

  ([name fn {let' :let :or {let' []}} form]
     `{:name ~name :fn (prep-fn ~fn ~let') :children ~form})

  ([name fn] `{~name ~fn}))

(defn branch? [node]
  (or (sequential? node) (-> node :children nil? not)))

(defn get-children
  [node]
  (if (sequential? node)
    node
    (:children node)))

(defn make-node [node children]
  (if (sequential? node)
    children
    (update node :children (fn [_] children))))

(defn get-fetch-fn
  [node]
  (->> node (filter (fn [[k v]] (fn? v))) seq first))


(defn update-node
  [loc name new]
  (let [cur (z/node loc)]
    (z/remove
      (reduce (fn [loc n]
                (z/insert-right loc
                  (z/make-node loc
                    (update (z/node loc) name (fn [_] n))
                    (-> (z/node loc) :children))))
        loc
        new))))

(defn fetch
  [zp]
  (if-let [[name f] (get-fetch-fn (z/node zp))]
    (m/match (f zp)
      [:success r] [:success (update-node zp name r)]
      [:fail    r] [:fail {:error r}])
    [:success zp]))

(defn zipper [desc]
  (z/zipper branch? get-children make-node desc))

(defn run-sync
  [desc]
  (loop [zp (zipper desc)
         failed []]
    (if (z/end? zp)
      [(z/root zp) failed]
      (if (sequential? (z/node zp))
        (recur (z/next zp) failed)
        (m/match (fetch zp)
          [:success zp'] (recur (z/next zp') failed)
          [:fail   fail] (if-let [r (z/right zp)]
                           (recur r (conj failed fail))
                           [(z/root zp) (conj failed fail)]))))))
