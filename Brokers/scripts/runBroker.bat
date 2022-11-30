cd ..
javac -d out/ -cp ".;lib/json-simple-1.1.1.jar" com\example\myapp\Broker.java
cd ./out
start /b java -cp ".;../lib/json-simple-1.1.1.jar" com.example.myapp.Broker 0 3
start /b java -cp ".;../lib/json-simple-1.1.1.jar" com.example.myapp.Broker 1 3
start /b java -cp ".;../lib/json-simple-1.1.1.jar" com.example.myapp.Broker 2 3