(ns defpage.core-test
  (:require [clojure.test :refer :all]
            [clout.core :as clout]
            [defpage.core :as d :refer (defpage)]))

(deftest parse-args-works
  (let [result (d/parse-args '(foo "/foo" [req] {:status 200}))]
    (is (= "/foo" (:path result)))
    (is (= :get (:method result)))
    (is (= 'foo (:name result)))
    (is (= '[req] (:destruct result)))
    (is (= '({:status 200}) (:body result))))

  (let [result (d/parse-args '(foo [:post "/foo"] [req] {:body "hello"}))]
    (is (= :post (:method result))))

  (let [result (d/parse-args '("/foo" [req] {:body "hello"}))]
    (is (:name result)))

  (is (d/parse-args '([:post "/foo"] [req] {:body "hello"})))

  (let [result (d/parse-args '([:get "/users/:id" {:id #"\d+"}] [req] {:body "hello"}))]
    (is (map? (:regexes result)))))

(defpage foo "/foo" [req]
  {:status 200})

(deftest simple-calling-works
  (is (foo {:uri "/foo"
            :request-method :get})))

(deftest make-route-works
  (let [r (d/make-route :get "/foo" (fn [req] {:status 200}))]
    (is (r {:request-method :get
            :uri "/foo"}))
    (is (not (r {:request-method :get
                 :uri "/bogus"})))
    (is (not (r {:request-method :post
                 :uri "/foo"})))))

(defpage bar "/bar" [req])
(deftest route-names-are-preserved
  (is #'bar)
  (is (:defpage.core/defpage (meta #'bar)))
  (is (:defpage.core/method (meta #'bar))))

(deftest route-matches-properly
  (is (d/route-matches? #'bar {:request-method :get
                               :uri "/bar"}))
  (is (not (d/route-matches? #'bar {:request-method :get
                                    :uri "/bogus"})))
  (is (not (d/route-matches? #'bar {:request-method :post
                                    :uri "/bar"}))))

(deftest routes-can-be-compiled
  (let [args (d/parse-args '([:get "/users/:id" {:id #"\d+"}] [req] {:body "hello"}))]
    (clout/route-compile (:path args) (:regexes args))))

(defpage regex-route [:post "/user/:id" {:id #"\d+"}] [req]
  {:body (str "yo, " (-> req :params :id))})

(deftest routes-can-specify-regex
  (is (d/route-matches? #'regex-route {:request-method :post
                                       :uri "/user/10"}))
  (is (not (d/route-matches? #'regex-route {:request-method :post
                                            :uri "/user/bogus"}))))

(deftest url-for-works
  (is (= "/user/5" (d/url-for regex-route {:id 5})))
  (testing "url-for throws on invalid args"
    (is (thrown? AssertionError (d/url-for regex-route {:bogus "foo"})))))

(deftest can-use-symbols-for-method-and-route
  (let [meth :get
        route "/route"]
    (is (defpage the-name [meth route] [request] {:status 200 :body "hello"}))))
