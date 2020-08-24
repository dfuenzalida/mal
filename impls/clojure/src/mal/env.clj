(ns mal.env
  (:refer-clojure :exclude [find get set]))

(declare set)

(defn make-env
  ([outer] (make-env outer [] []))
  ([outer binds exprs]
   (let [newenv {:outer outer :data (atom {})}]
     (doall
      (map (partial set newenv) binds exprs))
     newenv)))

(defn set [this key val]
  (when-let [data (:data this)]
    (swap! data assoc key val)))

(defn find [this key]
  (if (clojure.core/get @(:data this) key)
    this
    (if-let [outer (:outer this)]
      (find outer key)
      (throw (ex-info "" {:cause (str "Env containing " key " not found")})))))

(defn get [this key]
  (if (some #{key} (keys @(:data this)))
    (clojure.core/get @(:data this) key)
    (if-let [outer (:outer this)]
      (get outer key)
      (throw (ex-info "" {:cause (format "'%s' not found" key)})))))

