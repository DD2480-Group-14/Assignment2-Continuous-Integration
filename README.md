# Assignment2-Continuous-Integration

## Setting up the CI server

### Starting and setting up the server
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

## Functionality
When the server is running it has the following functionality.

### Retreiving build logs
- If you are on the machine that runs the server, you can go to http://localhost:8080/logs.

- If you are using forwarding with ngrok, you can visit your forwarding URL and append /logs (eg http://someurl.ngork.io/logs).