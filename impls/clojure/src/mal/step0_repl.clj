(ns mal.step0_repl)

(def READ identity)
(def EVAL identity)
(def PRINT identity)

(def rep (comp PRINT EVAL READ))

(defn prompt []
  (print "user> ")
  (flush)
  (read-line))

;; MAIN LOOP

(defn -main [& args]
  (loop [input (prompt)]
    (when input
      (println (rep input))
      (recur (prompt)))))

