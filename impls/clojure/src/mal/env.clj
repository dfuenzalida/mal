(ns mal.env
  (:refer-clojure :exclude [find get set]))

(defrecord Env [outer data])

(defn make-env [outer]
  (Env. outer (atom {})))

(defn set [this key val]
  (when-let [data (:data this)]
    (swap! data assoc key val)))

(defn find [this key]
  (if (clojure.core/get @(:data this) key)
    this
    (if-let [outer (:outer this)]
      (find outer key)
      (throw (ex-info (str "Env containing " key " not found") {})))))

(defn get [this key]
  (if-let [val (clojure.core/get @(:data this) key)]
    val
    (if-let [outer (:outer this)]
      (get outer key)
      (throw (ex-info (str key " not found") {})))))

