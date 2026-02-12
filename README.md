# Assignment2-Continuous-Integration

## Setting up the CI server

### Dependencies
* Apache Maven: 3.9.11
* Java 25.0.2
* Unix-like OS
* Ngrok 3.36.0

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

## Testing
When the server receives a push event from Github, it builds and tests the project automatically. This is done through extracting e.g. repository URL, which commit and which branch to test from the payload of the HTTP request.

### Test Execution
The test execution is carried out by creating a new process and running the command ```mvn clean test```, which builds and runs all tests implemented under ```src/test/java```. Information produced by the build and test is collected from the terminal output. To test that this works properly, we use two minimal maven projects that are compiled and unit tested by the server's methods.

### Unit testing
Unit testing is implemented with the ```JUnit``` library. Each public method have at least one corresponing unit test to test its functionality. Several methods require writing and/or reading files, which is done by creating files and directories within a temporary directory. The temporary directory, ```@TempDir``` in JUnit, helps managing temporary files used during testing.
