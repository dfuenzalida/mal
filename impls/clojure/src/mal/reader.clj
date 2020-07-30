(ns mal.reader
  (:refer-clojure :exclude [peek next])
  (:require [clojure.string :refer [ends-with? starts-with?]]))

(def quote-tokens
  {"'" 'quote, "~" 'unquote, "`" 'quasiquote, "~@" 'splice-unquote, "@" 'deref})

(defn make-reader [ts]
  (atom ts))

(defn peek [rdr]
  (first @rdr))

(defn next [rdr]
  (let [val (peek rdr)]
    (swap! rdr clojure.core/next)
    val))

(defn empty-reader? [rdr] ;; did we consume all tokens?
  (empty? @rdr))

(def tokenize-re
  #"[\s,]*(~@|[\[\]{}()'`~^@]|\"(?:\\.|[^\\\"])*\"?|;.*|[^\s\[\]{}('\"`,;)]*)")

(defn tokenize [s]
  (->> (re-seq tokenize-re s)
       (map second)
       (take-while (complement empty?))))

(declare read_form read_atom read_vector)

(defn read_list
  ([rdr] (read_list rdr "(" ")"))
  ([rdr opening closing]
   (when-not (= opening (next rdr)) (throw (ex-info "EOF" {})))
   (loop [items ()]
     (let [item (read_form rdr)]
       (cond
         (and (nil? item) (empty-reader? rdr)) (throw (ex-info "EOF" {}))
         (= (str item) closing) items
         :else (recur (concat items (list item))))))))

(defn read_vector [rdr]
  (into [] (read_list rdr "[" "]")))

(defn read_hashmap [rdr]
  (->> (read_list rdr "{" "}") (partition 2) (map vec) (into {})))

(def unescape-map
  {\\ \\, \n "\n", \b "\b", \f "\f", \r "\r", \t, "\t", \" "\""})

(defn unescape [s]
  (loop [xs (seq s) result "" escaping? false]
    (if-let [x (first xs)]
      (cond
        (= [escaping? x] [false \\]) (recur (rest xs) result true)
        (false? escaping?) (recur (rest xs) (str result x) false)
        :else (recur (rest xs) (str result (get unescape-map x "")) false))
      (if escaping?
        (throw (ex-info "EOF" {}))
        result))))

(defn read_string [s]
  (if (and (> (count s) 1) (starts-with? s "\"") (ends-with? s "\""))
    (->> (subs s 1 (dec (count s))) unescape)
    (throw (ex-info "EOF" {}))))

(def number-re #"[+-]?\d+")

(defn read_atom [rdr]
  (when-let [item (next rdr)]
    (cond
      (re-matches number-re item) (Long/parseLong item)
      (= "nil" item) nil
      (= "true" item) true
      (= "false" item) false
      (= "^" item) (let [mtdt (read_form rdr), obj (read_form rdr)]
                     (list 'with-meta obj mtdt))
      (quote-tokens item) (list (quote-tokens item) (read_form rdr))
      (first (filter #{\"} item)) (read_string item)
      (starts-with? item ":") (keyword (subs item 1))
      :else (symbol item))))

(defn read_form [rdr]
  (case (peek rdr)
    "(" (read_list rdr)
    "[" (read_vector rdr)
    "{" (read_hashmap rdr)
    (read_atom rdr)))

(defn read_str [s]
  (let [tokens (tokenize s)
        reader (make-reader tokens)]
    (read_form reader)))

