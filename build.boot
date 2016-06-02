(set-env!
  :source-paths #{"src"}
  :dependencies '[[org.clojure/clojure "1.8.0"]
                  [http-kit "2.1.18"]
                  [cheshire "5.5.0"]
                  [org.clojure/core.match "0.3.0-alpha4"]
                  [funcool/cats "1.2.1"]
                  [clojurewerkz/neocons "3.1.0"]
                  [environ "1.0.2"]
                  [adzerk/boot-jar2bin "1.1.0" :scope "test"]
                  [org.clojure/tools.cli "0.3.3"]
                  [clj-time "0.11.0"]
                  [uswitch/lambada "0.1.0"]
                  [amazonica "0.3.57" :exclusions [com.amazonaws/aws-java-sdk]]
                  [com.amazonaws/aws-java-sdk-core "1.10.75"]
                  [com.amazonaws/aws-java-sdk-s3 "1.10.75"]
                  [com.taoensso/timbre "4.3.1"]])

(require '[adzerk.boot-jar2bin :refer :all])

(task-options!
  pom {:project 'com-mon
       :version "0.1.0"}
  aot {:namespace '#{com-mon.main}}
  jar {:main 'com-mon.main
       :file "com-mon.jar"}
  bin {:output-dir ".bin"})

(deftask build []
  (comp (aot) (pom) (uber) (jar) (bin)))

(require 'boot.repl)

(swap! boot.repl/*default-dependencies*
  concat '[[cider/cider-nrepl "0.13.0-SNAPSHOT"]
           [refactor-nrepl "2.2.0-SNAPSHOT"]
           [org.clojure/tools.nrepl "0.2.12"]])

(swap! boot.repl/*default-middleware*
  conj 'cider.nrepl/cider-middleware
  'refactor-nrepl.middleware/wrap-refactor)

(deftask dev-server
  [b bind ADDR str "The address server listens on."
   p port PORT int "The port to listen on and/or connect to."]
  (System/setProperty "BOOT_EMIT_TARGET" "no")
  (repl
    :port port
    :bind bind
    :handler 'cider.nrepl/cider-nrepl-handler
    :server true))

(deftask awsl []
  (comp (aot) (pom) (uber) (jar)))
