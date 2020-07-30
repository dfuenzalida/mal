(ns mal.core
  (:require [mal.printer :refer [pr_str]])
  (:refer-clojure :exclude [ns]))

(defn print-helper [print_readably separator f]
  (fn [& xs] (->> (map #(pr_str % print_readably) xs)
                  (interpose separator)
                  (reduce str)
                  f)))

(def ns
  {'+ +
   '- -
   '* *
   '/ /
   'list list
   'list? seq?
   'empty? empty?
   'count count
   '> >
   '>= >=
   '= =
   '<= <=
   '< <
   'pr-str  (print-helper true " " identity)
   'str     (print-helper false "" identity)
   'prn     (print-helper true " " println)
   'println (print-helper false " " println)
   })
