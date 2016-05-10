(ns com-mon.db.neo4j
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.cypher :as cy]
            [environ.core :refer [env]]))

(defn conn []
  (nr/connect (env :neo4j-url)))

(defn save-group
  [conn node]
  (cy/tquery conn
    "merge (a:group:user{id: {g}.id})
     on create set a={g}"
    {:g (update (select-keys (:group node) [:id :name :screen_name])
          :id -)}))

(defn save-post
  [conn post group]
  (cy/tquery conn
    "match (g:group{id: {gid}.id})
     merge (p:post{id: {post}.id})
     on create set p={post}
     merge (a:user{id: {uid}.id})
     on create set a={uid}
     merge (g)-[:have]->(p)
     merge (a)-[:make]->(p)"
    {:gid {:id (-> group :group :id -)}
     :uid {:id (:from_id (:post post))}
     :post (select-keys (:post post) [:owner_id :date :from_id :id :text])}))

(defn save-comment
  [conn comment post]
  (cy/tquery conn
    "match (p:post{id: {pid}.id})
     merge (c:comment{id: {cmt}.id})
     on create set c={cmt}
     merge (a:user{id: {usr}.id})
     on create set a={usr}
     merge (p)-[:have]->(c)
     merge (a)-[:make]->(c)"
    {:pid {:id (:id (:post post))}
     :usr {:id (:from_id (:comment comment))}
     :cmt (select-keys (:comment comment)
            [:owner_id :date :from_id :id :text])}))

(defn save-like
  [conn like comment]
  (cy/tquery conn
    "match (c:comment{id: {cid}})
     merge (u:user{id: {uid}.id})
     on create set u={uid}
     merge (u)-[:like]->(c)"
    {:cid (:id (:comment comment)) :uid {:id (:like like)}}))

(defn get-vk-l-last [conn]
  (some-> (cy/tquery conn "match (n:updates) return n.file as file")
    first
    (get "file")))

(defn set-vk-l-last [conn fname]
  (cy/tquery conn
    "merge (n:updates) on create set n = {f} on match set n = {f}"
    {:f {:file fname}}))

(defn make-idxs [conn]
  (cy/tquery conn "create index on :user(id)")
  (cy/tquery conn "create index on :comment(id)")
  (cy/tquery conn "create index on :post(id)")
  (cy/tquery conn "create index on :group(id)"))
