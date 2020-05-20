(ns lifted.core-test
  (:require [clojure.test :refer :all]
            [lifted.core :refer :all]))

;;; Setup

(defn -no-args [])

(defn -foo "My docs"
  [x y]
  (+ x y))

(defn -var-args [x & xs]
  (apply + x xs))

(defn- -private [x] x)

(lift-as Foo {:expand-vararg-for #{-var-args}})


;;; Tests

(deftest basic-test

  (let [impl (lift-on Foo 10)]

    (is (nil? (resolve 'no-args)))

    (is (= 30 (foo impl 20)))

    (is (= 100 (var-args impl 20 30 40)))

    (is (= "My docs" (-> #'foo meta :doc)))

    (is (= 10 (private impl)))))
