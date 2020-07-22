(ns mal.printer)

(def escape-map
  {\\ "\\\\", \newline "\\n", \return "\\r", \tab "\\t", \" "\\\""})

(defn escape-string [s]
  (->> (map #(get escape-map % %) s) (reduce str)))

(defn pr_str [x]
  (cond
    (nil? x) "nil"
    (map? x) (let [ks (map pr_str (keys x))
                   vs (map pr_str (vals x))]
               (->> (map str ks (repeat " ") vs)
                    (interpose " ")
                    (reduce str)
                    (format "{%s}")))
    (symbol? x) (name x)
    (string? x) (->> x escape-string (format "\"%s\""))
    (seq? x) (->> (map pr_str x)
                  (interpose " ")
                  (reduce str)
                  (format "(%s)"))
    (vector? x) (->> (map pr_str x)
                     (interpose " ")
                     (reduce str)
                     (format "[%s]"))
    (number? x) (str x)
    (boolean? x) (if x "true" "false")))
