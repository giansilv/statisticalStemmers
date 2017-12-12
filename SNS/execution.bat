@echo off

echo Compilazione
javac -classpath ".;lib\*;" *.java

echo Esecuzione
java -classpath ".;lib\*;" -Xmx6144m -Xms256m  SnsStemmer