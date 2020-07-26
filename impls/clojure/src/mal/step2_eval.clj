(ns mal.step2_eval
  (:require [mal.reader :as reader]
            [mal.printer :as printer]))

(def repl_env {'+ +, '- -, '* *, '/ /})

(def READ reader/read_str)

(declare eval_ast)

(defn EVAL [ast env]
  (cond
    (not (seq? ast)) (eval_ast ast env)
    (and (seq? ast) (empty? ast)) ast
    :else (let [[f & xs] (eval_ast ast env)]
            (if f
              (apply f xs)
              (throw (ex-info "Symbol not found" {}))))))

(defn eval_ast [ast env]
  (cond
    (symbol? ast) (env ast)
    (seq? ast)    (map #(EVAL % env) ast)
    (vector? ast) (mapv #(EVAL % env) ast)
    (map? ast)    (zipmap (keys ast) (map #(EVAL % env) (vals ast)))
    :else ast))

(def PRINT printer/pr_str)

(defn rep [s]
  (-> (READ s)
      (EVAL repl_env)
      PRINT))

(defn prompt []
  (print "user> ")
  (flush)
  (read-line))

;; MAIN LOOP

(defn -main [& args]
  (loop [input (prompt)]
    (when input
      (try
        (println (rep input))
        (catch Exception ex
          (when-let [message (ex-message ex)] ;; print message only when there's one
            (println message))))
      (recur (prompt)))))

