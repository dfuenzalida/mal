(ns mal.step7_quote
  (:require [mal.core :as core]
            [mal.env :as env]
            [mal.reader :as reader]
            [mal.printer :as printer]))

(def repl_env
  (env/make-env nil (keys core/ns) (vals core/ns)))

(def READ reader/read_str)

(declare eval_ast)

(defn rebind [binds args] ;; update args if bindings include `&`
  (if (some #{'&} binds)
    (let [binds'  (remove #{'&} binds)
          [as bs] (split-at (dec (count binds')) args)
          args'   (concat as (list bs))]
      [binds' args'])
    [binds args]))

(defn is_pair [x] ;; returns true if the parameter is a non-empty (seq or vector)
  (and (or (vector? x) (seq? x))
       (seq x)))

(defn quasiquote [ast]
  (if-not (is_pair ast)
    (list 'quote ast)
    (let [[f & xs] ast]
      (cond
        (= 'unquote f) (first xs)
        (and (is_pair f) (= 'splice-unquote (first f)))
        (list 'concat (second f) (quasiquote xs))
        :else (list 'cons (quasiquote f) (quasiquote xs))))))

(defn EVAL [ast envi]
  (loop [ast ast, envi envi]
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
                        (recur body newenv))
                'do   (do (dorun (map #(EVAL % envi) (butlast xs))) ;; was eval_ast instead of EVAL
                          (recur (last xs) envi))
                'if   (let [[test then else] xs]
                        (if (EVAL test envi) (recur then envi) (recur else envi)))
                'fn*  (let [[binds body] xs
                            func (fn [& args] ;; TODO validate number of args over binds
                                   (let [[bs' as'] (rebind binds args)
                                         newenv    (env/make-env envi bs' as')]
                                     (EVAL body newenv)))]
                        {:ast body :params binds :env envi :fn func :type :tco})
                'quote (first xs)
                'quasiquote (recur (quasiquote (first xs)) envi)
                ;; otherwise, attempt regular function application
                (let [[f-or-tco & xs] (eval_ast ast envi)]
                  (if f-or-tco
                    (cond
                      (fn? f-or-tco) (apply f-or-tco xs)
                      :else (let [{:keys [ast params env fn]} f-or-tco
                                  [ps' xs'] (rebind params xs)
                                  newenv    (mal.env/make-env env ps' xs')]
                              (recur ast newenv)))
                    (throw (ex-info (str "Symbol not found. ast:" ast) {})))))))))

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
  ;; Expose `eval`
  (env/set repl_env 'eval #(EVAL % repl_env))

  ;; Defined in mal itself
  (rep "(def! not (fn* (a) (if a false true)))")
  (rep "(def! load-file (fn* (f) (eval (read-string (str \"(do \" (slurp f) \"\\nnil)\")))))")
  (rep (str "(def! *ARGV* (list " (->> (map #(format "\"%s\"" %) (rest args)) (interpose " ") (apply str)) "))"))

  (if-let [filename (first args)]
    (rep (format "(load-file \"%s\")" filename))
    ;; otherwise run a REPL
    (loop [input (prompt)]
      (when input
        (try
          (println (rep input))
          (catch Exception ex
            (when-let [message (ex-message ex)] ;; print message only when there's one
              (do
                (println "ERROR:" message)
                #_(.printStackTrace ex)))))
        (recur (prompt))))))

