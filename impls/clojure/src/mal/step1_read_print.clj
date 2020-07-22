(ns mal.step1_read_print
  (:require [mal.reader :as reader]
            [mal.printer :as printer]))

(def READ reader/read_str)
(def EVAL identity)
(def PRINT printer/pr_str)

(def rep (comp PRINT EVAL READ))

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

