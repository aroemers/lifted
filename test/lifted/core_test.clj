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

(lift-as Foo {:expand-varargs-for #{-var-args}})


;;; Tests

(deftest lift-as*-test
  (is (= (lift-as* 'lifted.core-test nil)
         '((foo [x y] "My docs")
           (private [x] nil)
           (var-args [x] nil))))

  (is (= (lift-as* 'lifted.core-test {:expand-varargs-for #{'-var-args}})
         '((foo [x y] "My docs")
           (private [x] nil)
           (var-args [x]
                     [x xs_0]
                     [x xs_0 xs_1]
                     [x xs_0 xs_1 xs_2]
                     [x xs_0 xs_1 xs_2 xs_3]
                     [x xs_0 xs_1 xs_2 xs_3 xs_4]
                     [x xs_0 xs_1 xs_2 xs_3 xs_4 xs_5]
                     [x xs_0 xs_1 xs_2 xs_3 xs_4 xs_5 xs_6]
                     [x xs_0 xs_1 xs_2 xs_3 xs_4 xs_5 xs_6 xs_7]
                     [x xs_0 xs_1 xs_2 xs_3 xs_4 xs_5 xs_6 xs_7 xs_8]
                     [x xs_0 xs_1 xs_2 xs_3 xs_4 xs_5 xs_6 xs_7 xs_8 xs_9]
                     [x xs_0 xs_1 xs_2 xs_3 xs_4 xs_5 xs_6 xs_7 xs_8 xs_9 xs_10]
                     [x xs_0 xs_1 xs_2 xs_3 xs_4 xs_5 xs_6 xs_7 xs_8 xs_9 xs_10 xs_11]
                     [x xs_0 xs_1 xs_2 xs_3 xs_4 xs_5 xs_6 xs_7 xs_8 xs_9 xs_10 xs_11 xs_12]
                     [x xs_0 xs_1 xs_2 xs_3 xs_4 xs_5 xs_6 xs_7 xs_8 xs_9 xs_10 xs_11 xs_12 xs_13]
                     [x xs_0 xs_1 xs_2 xs_3 xs_4 xs_5 xs_6 xs_7 xs_8 xs_9 xs_10 xs_11 xs_12 xs_13 xs_14]
                     [x xs_0 xs_1 xs_2 xs_3 xs_4 xs_5 xs_6 xs_7 xs_8 xs_9 xs_10 xs_11 xs_12 xs_13 xs_14 xs_15]
                     [x xs_0 xs_1 xs_2 xs_3 xs_4 xs_5 xs_6 xs_7 xs_8 xs_9 xs_10 xs_11 xs_12 xs_13 xs_14 xs_15 xs_16]
                     [x xs_0 xs_1 xs_2 xs_3 xs_4 xs_5 xs_6 xs_7 xs_8 xs_9 xs_10 xs_11 xs_12 xs_13 xs_14 xs_15 xs_16 xs_17]
                     [x xs_0 xs_1 xs_2 xs_3 xs_4 xs_5 xs_6 xs_7 xs_8 xs_9 xs_10 xs_11 xs_12 xs_13 xs_14 xs_15 xs_16 xs_17 xs_18]
                     nil)))))

(deftest lift-on*-test
  (is (= (lift-on* 'lifted.core-test 'lifted.core/Lifted 'G_123 nil)
         '((lifted [this] (lifted.core/-lifted G_123)))))

  (is (= (lift-on* 'lifted.core-test 'lifted.core/Lifted 'G_123 {:impl-ns 'lifted.other})
         '((lifted [this] (lifted.other/-lifted G_123))))))

(deftest api-test
  (let [impl (lift-on Foo 10)]

    (is (nil? (resolve 'no-args)))

    (is (= 30 (foo impl 20)))

    (is (= 100 (var-args impl 20 30 40)))

    (is (= "My docs" (-> #'foo meta :doc)))

    (is (= 10 (private impl)))

    (is (= 10 (lifted impl)))))
