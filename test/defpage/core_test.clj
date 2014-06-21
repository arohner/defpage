(ns defpage.core-test
  (:require [clojure.test :refer :all]
            [clout.core :as clout]
            [defpage.core :as d]))

(deftest parse-args-works
  (let [result (d/parse-args '(foo "/foo" [req] {:status 200}))]
    (is (= "/foo" (:path result)))
    (is (= :get (:method result)))
    (is (= 'foo (:name result)))
    (is (= '[req] (:destruct result)))
    (is (= '({:status 200}) (:body result))))

  (let [result (parse-args '(foo [:post "/foo"] [req] {:body "hello"}))]
    (is (= :post (:method result))))

  (let [result (parse-args '("/foo" [req] {:body "hello"}))]
    (is (:name result)))

  (is (parse-args '([:post "/foo"] [req] {:body "hello"})))

  (let [result (is (parse-args '([:get "/users/:id" {:id #"\d+"}] [req] {:body "hello"})))]
    (is (map? (:regexes result)))))

(defpage foo "/foo" [req]
  {:status 200})

(deftest make-route-works
  (let [r (d/make-route :get "/foo" (fn [req] {:status 200}))]
    (is (r {:request-method :get
            :uri "/foo"}))
    (is (not (r {:request-method :get
                 :uri "/bogus"})))
    (is (not (r {:request-method :post
                 :uri "/foo"})))))

(defpage foo "/bar" [req])
(deftest route-names-are-preserved
  (is #'foo)
  (is (:defpage.core/defpage (meta #'foo)))
  (is (:defpage.core/method (meta #'foo))))

(deftest route-matches-properly
  (is (d/route-matches? #'foo {:request-method :get
                               :uri "/bar"}))
  (is (not (d/route-matches? #'foo {:request-method :get
                                    :uri "/bogus"})))
  (is (not (d/route-matches? #'foo {:request-method :post
                                    :uri "/bar"}))))

(deftest routes-can-be-compiled
  (let [args (parse-args '([:get "/users/:id" {:id #"\d+"}] [req] {:body "hello"}))]
    (clout/route-compile (:path args) (:regexes args))))

(defpage regex-route [:post "/user/:id" {:id #"\d+"}] [req]
  {:body (str "yo, " (-> req :params :id))})

(deftest routes-can-specify-regex
  (is (d/route-matches? #'regex-route {:request-method :post
                                       :uri "/user/10"}))
  (is (not (d/route-matches? #'regex-route {:request-method :post
                                            :uri "/user/bogus"}))))

(deftest url-for-works
  (is (= "/user/5" (d/url-for regex-route {:id 5}))))
