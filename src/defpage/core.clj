(ns defpage.core
  (:require [clout.core :as clout]
            [clojure.string :as str]
            [compojure.core :as compojure]))

(defn sym-with-meta
  "Given a symbol, return the symbol w/ metadata"
  [sym metadata]
  (vary-meta sym merge metadata))

(defn parse-name [[result args]]
  (let [[name remaining] (if (symbol? (first args))
                           [(first args) (rest args)]
                           [nil args])]
    [(assoc result :name name) remaining]))

(defn- route->key
  "Given a keyword method (i.e. :get) and a URL path, return a string that is a valid clojure var name"
  [method rte]
  (let [method (str/replace (str method) #".*/" "")]
    (str method (-> rte
                    (str/replace #"\." "!dot!")
                    (str/replace #"/" "--")
                    (str/replace #":" ">")
                    (str/replace #"\*" "<")))))

(defn parse-route [[{:keys [name] :as result} [route :as args]]]
  (assert (or (and (vector? route)
                   (keyword? (first route))
                   (string? (second route)))
              (string? route))
          (format "Routes must either be a string or vector, got %s" route))
  (let [{:keys [method path] :as route} (if (vector? route)
                                          {:method (first route)
                                           :path (second route)
                                           :regexes (nth route 2 nil)}
                                          {:method :get
                                           :path route
                                           :regexes nil})
        name (or name (symbol (route->key method path)))]
    [(merge result route {:name name}) (rest args)]))

(defn parse-destruct [[result [destruct & rest]]]
  (assert (or (vector? destruct)
              (map? destruct)))
  [(assoc result :destruct destruct) rest])

(defn parse-body [[result args]]
  [(assoc result :body args) []])

(defn parse-args
  "parses the arguments to defpage. Returns a map containing the
  keys :name :method :url :destruct :body"
  [args]
  (-> (parse-name [{} args])
      (parse-route)
      (parse-destruct)
      (parse-body)
      first))

(defn make-route
  "Returns a function that will only call the handler if the method
  and Clout route match the request."
  [method route handler]
  (assert (or (keyword? method) (nil? method)))
  (let [f (#'compojure/if-method method
            (#'compojure/if-route route
                                  (fn [request]
                                    (compojure.response/render (handler request) request))))]
    (with-meta f (merge (meta f) {::defpage true
                                  ::method method
                                  ::route route}))))

(defmacro defpage [& args]
  (let [{:keys [name method path regexes destruct body]
         :as args} (parse-args args)]
    `(let [body-fn# (fn [request#]
                      (let [~@destruct request#]
                        ~@body))
           path# ~path
           regexes# ~regexes
           method# ~method
           compiled-route# (if regexes#
                             (clout/route-compile path# regexes#)
                             (clout/route-compile path#))
           v# (def ~name (make-route method# compiled-route# body-fn#))]
       (alter-meta! (var ~name) merge {::defpage true
                                       ::method method#
                                       ::route compiled-route#})
       v#)))

(defn find-defpages
  ([]
     (find-defpages *ns*))
  ([ns]
     (->> ns
          ns-publics
          vals
          (filter (fn [v]
                    (-> v meta ::defpage)))
          (sort-by :line))))

(defn combine-routes
  "Takes a seq of defpage fns, returns a single ring handler that checks each route in turn"
  [handlers]
  (fn [request]
    (apply compojure/routing request handlers)))

(defn collect-routes
  "find all defpages defined in ns. Returns a ring handler. With no args, collects routes in the current ns"
  ([]
     (collect-routes *ns*))
  ([& nses]
     (let [handlers (mapcat find-defpages nses)]
       (combine-routes handlers))))

(defn route-matches?
  "Takes a defpage fn, returns truthy if the request matches"
  [route-fn request]
  (let [method (-> route-fn meta ::method)
        route (-> route-fn meta ::route)]
    (assert method)
    (assert route)
    (and (#'compojure/method-matches? method request)
         (clout.core/route-matches route request))))

(defn map-wrap-matching-routes
  "Takes a series of routes, from find-defpages. individually wraps
  middleware around each route, only calling middleware if the route
  matches first"
  [middleware routes]
  (map (fn [handler]
         (let [new-handler (middleware handler)]
           (fn [request]
             (when (route-matches? handler request)
               (new-handler request))))) routes))

(defn url-for
  "Given a defpage"
  [route args]
  (let [compiled-route (-> route meta ::route)
        route-args (-> compiled-route :keys)]
    (assert (= (set (keys args)) (set route-args)) (format "url-for arg names must match route params: %s vs. %s" (seq route-args) (keys args)))
    (reduce (fn [path k]
              (str/replace-first path #"\([^\)]+\)" (str (get args k)))) (-> compiled-route :re str) route-args)))
