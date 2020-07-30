(ns mal.printer)

(def escape-map
  {\\ "\\\\", \newline "\\n", \return "\\r", \tab "\\t", \" "\\\""})

(defn escape-string [s]
  (->> (map #(get escape-map % %) s) (reduce str)))

(defn pr_str
  ([x] (pr_str x true))
  ([x print_readably]
   (cond
     (nil? x) "nil"
     (map? x) (let [ks (map #(pr_str % print_readably) (keys x))
                    vs (map #(pr_str % print_readably) (vals x))]
                (->> (map str ks (repeat " ") vs)
                     (interpose " ")
                     (reduce str)
                     (format "{%s}")))
     (symbol? x) (name x)
     (string? x) (if print_readably (->> x escape-string (format "\"%s\"")) x)
     (seq? x) (->> (map #(pr_str % print_readably) x)
                   (interpose " ")
                   (reduce str)
                   (format "(%s)"))
     (vector? x) (->> (map #(pr_str % print_readably) x)
                      (interpose " ")
                      (reduce str)
                      (format "[%s]"))
     (number? x) (str x)
     (boolean? x) (if x "true" "false")
     (keyword? x) (str x)
     (fn? x) "#<function>")))
