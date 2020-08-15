(ns mal.core
  (:require [mal.reader :refer [read_str]]
            [mal.printer :refer [pr_str]])
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
   'read-string read_str
   'slurp slurp
   ;; Atom functions
   'atom atom
   'atom? (partial instance? clojure.lang.Atom)
   'deref deref
   'reset! reset!
   ;; swap! supports functions and function-TCOs
   ;; so you can do `(swap! (atom 1) (fn* (x) (+ 1 x)))`
   'swap! (fn [atm fn-or-tco & args]
            (let [args (-> (or args ()) (conj @atm)) ;; (@atm arg1 arg2 arg3 ...)
                  f    (get fn-or-tco :fn fn-or-tco)
                  val  (apply f args)]
              (reset! atm val)))

   ;; Step 7 - quote
   'cons cons
   'concat concat
   })
