(ns mal.step3_env
  (:require [mal.env :as env]
            [mal.reader :as reader]
            [mal.printer :as printer]))

(def repl_env (env/make-env nil))

(doall ;; Load the aritmethic ops into the root env
 (map (fn [[k v]] (env/set repl_env k v)) {'+ +, '- -, '* *, '/ /}))

(def READ reader/read_str)

(declare eval_ast)

(defn EVAL [ast envi]
  (cond
    (not (seq? ast)) (eval_ast ast envi)
    (and (seq? ast) (empty? ast)) ast
    :else (let [[f & xs] ast]
            (cond
              (= 'def! f) (let [val (EVAL (second xs) envi)]
                            (env/set envi (first xs) val)
                            val)
              (= 'let* f) (let [newenv (env/make-env envi)
                                ps     (partition 2 (first xs))
                                body   (second xs)]
                            (dorun
                             (map (fn [[k v]] (env/set newenv k (EVAL v newenv))) ps))
                            (EVAL body newenv))
              :else         (let [[f & xs] (eval_ast ast envi)]
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

