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
* Select this repository for "repository access" and give it "write" permission
* Generate token
* Set the variable `GITHUB_TOKEN` to the generated token in `.env` file in a similiar manner as previous step

## Functionality
When the server is running it has the following functionality.

### Retreiving build logs
- If you are on the machine that runs the server, you can go to http://localhost:8080/logs.

- If you are using forwarding with ngrok, you can visit your forwarding URL and append /logs (eg http://someurl.ngork.io/logs).
