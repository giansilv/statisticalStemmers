echo Compilazione
javac -classpath "lib/*" *.java

echo Esecuzione
java -cp ".:lib/*" -Xmx10144m -Xms256m SnsStemmer

