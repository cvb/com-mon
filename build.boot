(set-env!
  :source-paths #{"src"}
  :dependencies '[[org.clojure/clojure "1.8.0"]
                  [http-kit "2.1.18"]
                  [cheshire "5.5.0"]
                  [org.clojure/core.match "0.3.0-alpha4"]
                  [funcool/cats "1.2.1"]])

(task-options!
  pom {:project 'com-mon
       :version "0.1.0"}
  jar {:manifest { }})
