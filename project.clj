(defproject defpage "0.1.5-SNAPSHOT"
  :description "A simple way to generate ring handlers"
  :url "https://github.com/arohner/defpage"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring "1.3.0"]
                 [clout "1.2.0"]
                 [compojure "1.1.8"]]
  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :creds :gpg}]])
