(defproject relayer "0.1.0-SNAPSHOT"
  :min-lein-version "2.0.0"
  :main ^:skip-aot relayer.handler
  :uberjar-name "standalone.jar"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [prismatic/schema "1.1.6"]
                 [metosin/compojure-api "1.1.10"]
                 [ring/ring-jetty-adapter "1.6.1"]
                 [jumblerg/ring.middleware.cors "1.0.1"]
                 [com.taoensso/carmine "2.16.0"]]
                 
  :profiles {
    :uberjar {:aot :all}
    :dev {
      :repl-options {:init-ns relayer.load}
      :plugins [[lein-ring "0.9.7"]]
      :ring {:handler relayer.handler/app}
    }
  })
