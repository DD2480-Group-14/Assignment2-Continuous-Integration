# Assignment2-Continuous-Integration

## Setting up the CI server

### Starting the server
Open a terminal and go to this root folder of this reposittory

1. Create the jar file
```
mvn clean package
```

2. Run the jar file
```
java -jar target/ci-server-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Making the server accessible from the internet
Make sure you have installed and configured ngrok. Open a new terminal window and run the following command
```
ngrok http 8080
```

### Add the server to your github repository
In your Github repository:

* go to `Settings >> Webhooks`, click on `Add webhook`.
* paste the forwarding URL (from the ngrok terminal) in the field `Payload URL` and then click on `Add webhook`. In the simplest setting, nothing more is required.