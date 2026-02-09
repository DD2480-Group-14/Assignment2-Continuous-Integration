package io.github.dd2480group14.ciserver;

<<<<<<< 6-handle-push-notification
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
=======
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.*;

import java.util.Scanner;
import java.util.List;

import java.nio.file.Files;
 
import org.eclipse.jetty.server.Server;
>>>>>>> main
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONObject;

/** 
 Skeleton of a ContinuousIntegrationServer which acts as webhook
 See the Jetty documentation for API documentation of those classes.
*/
public class ContinuousIntegrationServer extends AbstractHandler {
    private final File logsFolder;

    public ContinuousIntegrationServer() {
        logsFolder = new File("logs");

        if (!logsFolder.exists()) {
            logsFolder.mkdir();
        }

        if (logsFolder.isFile()) {
            throw new IllegalArgumentException("logsFolder can not be an already existing file.");
        }
    }
    
    public ContinuousIntegrationServer(File logsFolder) {
        this.logsFolder = logsFolder;

        if (!logsFolder.exists()) {
            logsFolder.mkdir();
        }

        if (logsFolder.isFile()) {
            throw new IllegalArgumentException("logsFolder can not be an already existing file.");
        }
    }

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException, ServletException
    {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        System.out.println(target);

        switch (request.getMethod().toUpperCase()) {
            case "POST":
                handlePost(target, baseRequest, request, response);
                break;

            case "GET":
                handleGet(target, baseRequest, request, response);
                break;

            default:
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                break;

        }
    }


    /**
     * TO DO
     * 
     * @param target
     * @param baseRequest
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    private void handlePost(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException, ServletException
    {
        String githubEvent = request.getHeader("X-GitHub-Event");
        String body = IOUtils.toString(request.getReader());
        JSONObject jsonObject = new JSONObject(body);

        if ("push".equals(githubEvent)) {
            PushEventInfo info = extractPushInfo(jsonObject);
            response.getWriter().println("Push event recieved.");
        } else {
            response.getWriter().println("No push event recieved.");
        }
    }
    
    private void handleGet(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException, ServletException
    {
        response.getWriter().println("GET request");
    }

    /**
     * Extracts information from Github push webhook payload.
     * 
     * @param jsonObject the JSON payload recieved from Github push event.
     * @return a PushEventInfo object containing extracted data.
     */
    public PushEventInfo extractPushInfo(JSONObject jsonObject){
       
        JSONObject repo = jsonObject.getJSONObject("repository");
        String repoURL = repo.getString("clone_url");

        String SHA = jsonObject.getString("after");

        String ref = jsonObject.getString("ref");
        String branch = ref.replace("refs/heads/", "");

        JSONObject pusher = jsonObject.getJSONObject("pusher");
        String author = pusher.getString("name");

        JSONArray commits= jsonObject.getJSONArray("commits");
        JSONObject latestCommit = commits.getJSONObject(0);
        String commitMessage = latestCommit.getString("message");

        return new PushEventInfo(author, repoURL, SHA, branch, commitMessage);
    }


    /**
     * Executes command in specificed directory 
     * @param command The command to run.
     * @param directory The directory to run it in.
     * @return Returns the terminal output after the command.
     */
    String runCommand(List<String> command, File directory) throws IOException, InterruptedException {
	ProcessBuilder processBuilder = new ProcessBuilder(command);
	processBuilder.directory(directory);
	processBuilder.redirectErrorStream(true);
	Process process = processBuilder.start();

	try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
		StringBuilder stringBuilder = new StringBuilder();
		String line;
		boolean firstLine = true;
		while ((line = bufferedReader.readLine()) != null) {
			if (!firstLine) {
					stringBuilder.append("\n");
			}
			stringBuilder.append(line);
			firstLine = false;
		}
		process.waitFor();
		String output = stringBuilder.toString();
		return output;
	} finally {
		process.destroy();
	}
    }


    /**
     * Clones git repository into a temporary directory
     *
     * @param url The url of the repository
     * @return directory The temporary directory containing the repo
     */
    File gitClone(String url) throws IOException, InterruptedException {
		File directory = Files.createTempDirectory("repository").toFile();
		List<String> command = List.of("git", "clone", url);
		runCommand(command, directory);
		return directory;
    }

    void removeGitDir() {
        return;
    }

    String runTests(File dir) {
        return "log";
    }

    String getBuildLog(String buildId) {
        File file = new File(logsFolder.getPath() + "/" + buildId + ".log");
        StringBuilder stringBuilder = new StringBuilder();

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
            stringBuilder.append(scanner.nextLine()).append("\n");
            }
        } catch (FileNotFoundException e) {
            return null;
        }

        return stringBuilder.toString();
    }

    String getBuilds() {
        return "Builds";
    }

    void storeBuildLog(String log) {
        return;
    }
 
    // used to start the CI server in command line
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8080);
        server.setHandler(new ContinuousIntegrationServer()); 
        server.start();
        server.join();
    }
}
