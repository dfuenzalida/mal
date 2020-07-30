(ns mal.step4_if_fn_do
  (:require [mal.core :as core]
            [mal.env :as env]
            [mal.reader :as reader]
            [mal.printer :as printer]))

(def repl_env
  (env/make-env nil (keys core/ns) (vals core/ns)))

(def READ reader/read_str)

(declare eval_ast)

(defn EVAL [ast envi]
  (cond
    (not (seq? ast)) (eval_ast ast envi)
    (and (seq? ast) (empty? ast)) ast
    :else (let [[f & xs] ast]
            (condp = f
              'def! (let [val (EVAL (second xs) envi)]
                      (env/set envi (first xs) val)
                      val)
              'let* (let [newenv (env/make-env envi)
                          ps     (partition 2 (first xs))
                          body   (second xs)]
                      (dorun
                       (map (fn [[k v]] (env/set newenv k (EVAL v newenv))) ps))
                      (EVAL body newenv))
              'do   (last (eval_ast xs envi))
              'if   (let [[test then else] xs]
                      (if (EVAL test envi) (EVAL then envi) (EVAL else envi)))
              'fn*  (let [[binds body] xs]
                      (fn [& args]
                        (if (some #{'&} binds)
                          (let [binds'  (remove #{'&} binds)
                                [as bs] (split-at (dec (count binds')) args)
                                ;; TODO fix: args' is a Seq, not list, we use `seq?`
                                args'   (concat as (list bs))
                                newenv  (env/make-env envi binds' args')]
                            (EVAL body newenv))
                          (let [newenv (env/make-env envi binds args)]
                            (EVAL body newenv)))))
              ;; otherwise, attempt regular function application
              (let [[f & xs] (eval_ast ast envi)]
                (if f
                  (apply f xs)
                  (throw (ex-info "Symbol not found" {}))))))))

(defn eval_ast [ast envi]
  (cond
    (symbol? ast) (env/get envi ast)
    (seq? ast)    (map #(EVAL % envi) ast)
    (vector? ast) (mapv #(EVAL % envi) ast)
    (map? ast)    (zipmap (keys ast) (map #(EVAL % envi) (vals ast)))
    :else ast))

(def PRINT printer/pr_str)

(defn rep [s]
  (-> (READ s)
      (EVAL repl_env)
      (PRINT true)))

(defn prompt []
  (print "user> ")
  (flush)
  (read-line))

;; MAIN LOOP

(defn -main [& args]
  ;; Define `not` in mal itself
  (rep "(def! not (fn* (a) (if a false true)))")

  (loop [input (prompt)]
    (when input
      (try
        (println (rep input))
        (catch Exception ex
          (when-let [message (ex-message ex)] ;; print message only when there's one
            (println "ERROR:" message))))
      (recur (prompt)))))

