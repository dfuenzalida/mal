all:
	javac src/mal/*.java -d bin

bin/mal/step%.class: src/mal/step%.java ${SOURCES}
	make all

clean:
	rm -rf bin
