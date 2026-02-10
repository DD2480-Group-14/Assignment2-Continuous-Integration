package io.github.dd2480group14.ciserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONException;
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
     * Handles incoming webhook notifications from Github 
     * by parsing the JSON payload and trigger the build process.
     * 
     * @param target                target of the request.
     * @param baseRequest           
     * @param request               HttpServletRequest request containing headers and payload.
     * @param response              HttpServletResponse reponse acknowledge webhook. 
     */
    private void handlePost(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException, ServletException
    {
        String githubEvent = request.getHeader("X-GitHub-Event");

        try {
            String body = IOUtils.toString(request.getReader());
            JSONObject jsonObject = new JSONObject(body);

            if ("push".equals(githubEvent)) {
                PushEventInfo info = extractPushInfo(jsonObject);
                
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("Push event recieved.");

                // TO DO: Run CI Pipeline

            } else {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("No push event recieved. Event ignored.");
            }

        } catch (IllegalArgumentException | JSONException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
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
     * @return a PushEventInfo record containing extracted data.
     * @throws IllegalArgumentException if payload is not valid.
     */
    public PushEventInfo extractPushInfo(JSONObject jsonObject){
        try {
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
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid Github push payload", e);
        }
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

    /**
     * Recursively removes specified directory, 
     * subfiles and subdirectories if located in
     * the system's tmp directory
     *
     * @param directory The directory to remove 
     */
    void removeDirectoryInTmp(File directory) throws IOException {
		Path verifiedDirectoryPath = getVerifiedPath(directory);
		try ( Stream<Path> paths = Files.walk(verifiedDirectoryPath)) {
				for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
					Files.delete(path);
				}
		};
    }

    private Path getVerifiedPath(File directory) throws IllegalArgumentException, IOException {
		if (directory == null || !directory.exists()) {
				throw new IllegalArgumentException("Directory does not exists");
		}
		Path directoryPath = directory.toPath().toRealPath();
		Path systemTmpPath = Paths.get(System.getProperty("java.io.tmpdir")).toRealPath();
		if (!directoryPath.startsWith(systemTmpPath)) {
				throw new IllegalArgumentException(
					String.format("Only allowed to remove directories in %s", systemTmpPath)
				);
		}
		return directoryPath;
    }

    /**
     * Runs mvn test to test the cloned repo
     * @param directory The path to the cloned directory
     * @return The terminal output after trying to build and test
     */
    public String runTests(File directory) throws IOException, InterruptedException {
        List<String> testCommand = Arrays.asList("mvn", "clean", "test");
        return runCommand(testCommand, directory);
    }

    String getBuildLog(String buildId) throws IOException, IllegalArgumentException {
        File file = new File(logsFolder.getPath() + "/" + buildId + ".log");
		if (!isInLogDirectory(file)) {
			throw new IllegalArgumentException("Build log must be in logs directory");
		};
        StringBuilder stringBuilder = new StringBuilder();

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
            stringBuilder.append(scanner.nextLine()).append("\n");
            }
        } 
        return stringBuilder.toString();
    }

    private boolean isInLogDirectory(File file) {
		Path realFilePath = file.toPath().toAbsolutePath().normalize();
		Path realLogPath = logsFolder.toPath().toAbsolutePath().normalize();
		boolean result = realFilePath.startsWith(realLogPath);
		return result;
    };

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
