# Step 0

(def READ identity)
(def EVAL identity)
(def PRINT identity)

(defn rep [s]
  (PRINT (EVAL (READ s))))

(defn prompt-line []
  (prin "user> ")
  (flush)
  (getline))

# Main loop

(loop [input :iterate (prompt-line) :while (not (empty? input))]
  (prin (rep input)))

