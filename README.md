# defpage

A simple library for defining ring handlers, a more flexible & powerful version of 'defpage' from Noir. It is an alternative to compojure's

## Usage

```clojure
(:require [defpage.core :as defpage])

(defpage foo "/foo/bar" [request]
  "hello world")

```

the defpage macro takes an optional route name, the route path, then the standard arguments and function body. defpage functions always take a single argument, the ring request

The route path can also be a vector, to specify which HTTP methods it responds to:

```clojure
(defpage [:post "/bar"] [request]
  "successful post")
```

`defpage` is mostly stateless, it's only sugar on top of defn. To hook up your new routes to ring, use `collect-routes:`

```clojure
(collect-routes my-ns.foo)
```

`collect-routes` returns a fn that behaves like a compojure handler, a fn that takes a request, and returns a ring response, or nil if no route matched. defpages are ordered according to line number (from the :line metadata) in the namespace.

Route ordering between namespaces can be handled using standard ring/compojure techniques:

```clojure
(defn handler []
  (compojure.core/routes
    (collect-routes 'foo)
    (collect-routes 'bar)))
```

## map-wrap-routes
If you'd like to apply a middleware to routes, but you don't want to interfere with route handling, use map-wrap-routes. For a motivating example, let's say you want to require login for a set of routes, but you want non-matching routes to 404:

```clojure
(defn catch-all [request]
  {:status 404
   :body nil})

(defpage "/foo" [req]
  "foo")
(defpage "/bar" [req]
  "bar")

(defn wrap-require-login [handler]
  (fn [request]
    (if (logged-in? request)
      (handler request)
      {:status 403
       :body "login required"})))

(def logged-in-app
  (->> (defpage/find-defpages (find-ns 'authenticated.ns))
       (defpage/map-wrap-matching-routes wrap-require-login)
       (defpage/combine-routes)))

(defn whole-app []
 (routes #'logged-in-app
         #'catch-all))
```

This will properly check for logged in on "/foo" and "/bar", and return 404 on "/bogus"

## Data model
routes defined by defpage are just standard defns with extra metadata:

```clojure
{:defpage.core/defpage true
 :defpage.core/method :get
 :defpage.core/route "/foo/bar"}
```

## params, url-for
defpage uses clout, so you can use keywords in routes:

(defpage "/user/:id" ...)

Regexes can also be specified

(defpage ping-user [:post "/user/:id/ping" {:id #"\d+"}] ...)

You can use url-for to return a complete path, given a route

(url-for ping-user {:id 10})
=> "/user/10/ping"


## License

Copyright © 2014 Allen Rohner.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
