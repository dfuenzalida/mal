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
   ;; Step 8 - macros
   'nth nth
   'first first
   'rest rest
   ;; Step 9 - try*/catch*
   'throw (fn [val] (throw (ex-info nil {:cause val})))
   'nil? nil?
   'true? true?
   'false? false?
   'symbol? symbol?
   'symbol symbol
   'keyword keyword
   'keyword? keyword?
   'vector vector
   'vector? vector?
   'sequential? (fn [x] (or (seq? x) (vector? x)))
   'hash-map hash-map
   'map? map?
   'assoc assoc
   'dissoc dissoc
   'get get
   'contains? contains?
   'keys (fn [m] (concat () (keys m)))
   'vals (fn [m] (concat () (vals m)))

   'apply (fn [f & xs]
            (let [f'   (get f :fn f) ;; support both fn and tco
                  args (butlast xs)
                  lst  (let [lst (last xs)]
                         (if (or (seq? lst) (vector? lst)) lst (list lst)))]
              (apply f' (concat args lst))))

   ;; will use seq . mapv otherwise an exception could be thrown too late
   'map (fn [f xs & _]
          (let [f' (get f :fn f)] ;; support both fn and tco
            (seq (mapv f' xs))))

   ;; Step A - mal
   'readline (fn [s] (print s) (flush) (read-line))
   'time-ms (fn [] (.getTime (java.util.Date.)))
   'meta (comp :metaval meta)
   'with-meta (fn [val metaval] (with-meta val {:metaval metaval}))

   'fn? (fn [f]
          (and (fn? (get f :fn f)) ;; function or tco
               (not (get f :is_macro false)))) ;; not a macro

   'string? string?
   'number? number?
   'seq (fn [x] (if (string? x) (seq (map str x)) (seq x)))
   'conj conj
   })
