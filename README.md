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
* Enter a "secure" password in `Secret`
* paste the forwarding URL (from the ngrok terminal) in the field `Payload URL` and then click on `Add webhook`. In the simplest setting, nothing more is required.

### Add secret to project
Create `.env` file in project root and set the variable `WEBHOOK_SIGNATURE` to the password you used in previous step

Alternatively, one could copy paste and replace "password123" with their "secure" password
```bash
echo WEBHOOK_SIGNATURE=password123 > .env
```

### Generate Github API Token

* Go to https://github.com/settings/tokens 
* Select resource owner and then this repository for "repository access" and set the following permission: "Commit statuses" repository permissions (Read and Write)
* Generate token
* Set the variable `GITHUB_TOKEN` to the generated token in `.env` file in a similiar manner as previous step

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

## Documentation
A browsable documenation using Javadoc can be generated with the following command:

```mvn javadoc:javadoc```

The documenation becomes available under ```target/reports/apidocs```. 

## Essence

Looking at the checklist we have completed the **seeded** phase and **formed** stage after the first assignment. We would argue that we are currently in the **collaborating** phase. This is primarly because of the "The team members know and trust each other" check, we are still getting to know each other. Overtime, by continuing to collaborate and communicating, we will get to know each other better and trust each other more and eventually be able to move on to the **performing** stage.

## Contributions

### Melker Trané
- Implemeted http GET functionality for a specific log
- Documented how to set up the server, how to make it visible from the interner, and how to add the webhook.
- Reviewed around 1/2 of other team members PRs.

### Edwin Nordås Jogensjö
- Implemented functionality that stores build logs
- Implemented the retreiving of all build logs as HTML
- Created a test-running method and two small maven projects that are compiled and run as tests.