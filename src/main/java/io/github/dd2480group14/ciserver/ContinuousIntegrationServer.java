package io.github.dd2480group14.ciserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.stream.Stream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONException;
import org.json.JSONObject;

import io.github.cdimascio.dotenv.Dotenv;

/** 
 *A ContinuousIntegrationServer which acts as webhook.
 */
public class ContinuousIntegrationServer extends AbstractHandler {
    private final File logsFolder;
    private final GitHubApiClient githubClient;
    private final String signature;
    
    /**
     * Constructs a new ContinuousIntegrationServer instance with the default logs folder path.
     */
    public ContinuousIntegrationServer(String signature, String githubToken, File logsFolder) {
        this.logsFolder = logsFolder;


        if (!logsFolder.exists()) {
            logsFolder.mkdir();
        }

        if (logsFolder.isFile()) {
            throw new IllegalArgumentException("logsFolder can not be an already existing file.");
        }
		this.signature = signature;
        githubClient = new GitHubApiClient(githubToken);
    }
    

    /**
     * Constructs a new ContinuousIntegrationServer instance with the default logs folder path.
     */
    public ContinuousIntegrationServer(String signature, String githubToken) {
        this(signature, githubToken, new File("logs"));
    }
    

    /**
     * Handles incoming HTTP requests 
     * @param target                target of the request.
     * @param baseRequest           Request to signal to Jetty that the request has been processed
     * @param request               HttpServletRequest request containing headers and payload.
     * @param response              HttpServletResponse reponse acknowledge webhook. 
     */
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException, ServletException
    {
        response.setContentType("text/html;charset=utf-8");
        baseRequest.setHandled(true);

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
        	String githubSignature = request.getHeader("X-Hub-Signature-256");
        try {
            String body = IOUtils.toString(request.getReader());
			validateGithubSignature(githubSignature, body);
            String urlDecoded = URLDecoder.decode(body, StandardCharsets.UTF_8);
            String jsonStr = urlDecoded.replace("payload=", "");
            JSONObject jsonObject = new JSONObject(jsonStr);

            if ("push".equals(githubEvent)) {
                PushEventInfo info = PushEventInfo.fromJSON(jsonObject);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("Push event recieved.");

                File gitDirectory = gitClone(info.repoURL(), info.SHA());
                String testLog = runTests(gitDirectory);
                storeBuildLog(testLog, info.SHA());
                
                // TO DO: Run CI Pipeline

            } else {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("No push event recieved. Event ignored.");
            }
        } catch (SecurityException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (IllegalArgumentException | JSONException | InterruptedException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Validates the incoming github webhook signature of the
     * payload and throws SecurityException if invalid
     */
    private void validateGithubSignature(String githubSignature, String body) throws SecurityException, IllegalArgumentException {
		if (githubSignature == null || githubSignature.isEmpty()) {
			throw new IllegalArgumentException("Github Signature cant be null");
		}
		String calculatedHmac = new HmacUtils("HmacSHA256", signature).hmacHex(body);
		boolean signaturesAreEqual = githubSignature.equals("sha256=" + calculatedHmac);
		if (!signaturesAreEqual) {
			throw new SecurityException("Signature does not match webhook");
		}
    }

    private void handleGet(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException, ServletException
    {

        if (target.equals("/logs")) {
            response.getWriter().println(getBuilds());
            return;
        }

        if (target.startsWith("/logs/")) {
            String subString = target.substring(6);
            try {
                String logText = getBuildLog(subString);
                response.getWriter().write(logText);
                return;
            } catch (FileNotFoundException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            } catch (IllegalArgumentException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            } catch (IOException e) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;   
            }
        }

        response.sendError(404);
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
     * @param branch The branch that we want
     * @param commitId The specific commit ID. If null, the latest commit is used.
     * @return directory The temporary directory containing the repo
     */
    File gitClone(String url, String commitId) throws IOException, InterruptedException {
		File directory = Files.createTempDirectory("repository").toFile();
		List<String> command = List.of("git", "clone", url, ".");
		runCommand(command, directory);
        if (commitId != null) {
            command = List.of("git", "checkout", commitId);
		    runCommand(command, directory);
        }
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

    /**
     * Returns the log with the specified build ID.
     * @param buildId The build ID of the log
     * @return The log as a String
     * @throws IOException If file does not exist.
     * @throws IllegalArgumentException If argument leads to a path outside of the logs folder.
     */
    public String getBuildLog(String buildId) throws IOException, IllegalArgumentException {
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

    /**
     * Checks wheter the given file is inside the log directory.
     * @param file The specified file.
     * @return True if the file is in the log directory.
     */
    private boolean isInLogDirectory(File file) {
		Path realFilePath = file.toPath().toAbsolutePath().normalize();
		Path realLogPath = logsFolder.toPath().toAbsolutePath().normalize();
		boolean result = realFilePath.startsWith(realLogPath);
		return result;
    };

    /**
     * Returns an HTML table row containing a summary of the
     * log with the given build ID.
     * @param buildId Build ID of the log
     * @return The summary of the log with the following format:
     * <tr>
     *  <td> [Build ID (as a href)] </td>
     *  <td> [Date] </td>
     *  <td> [Commit ID] </td>
     * </tr>
     * Metadata is replaced by "null" if it does not exist.
     * @throws IOException If the file does not exist
     * @throws IllegalArgumentException If the argument leads to a path outisde of the logs folder
     */
    String getBuildLogHTMLTableRow(String buildId) throws IOException, IllegalArgumentException {
        String fullText = getBuildLog(buildId);
        String commitId = StringUtils.substringBetween(fullText, "Commit ID: ", "\n");
        String date = StringUtils.substringBetween(fullText, "Build date: ", "\n");
        String output = "<tr><td><a href=\"/logs/" + buildId + "\"</a>" + buildId + "</td>" 
        + "<td>" + date + "</td>" +  "<td>" + commitId + "</td></tr>";
        return output;
    }

    /**
     * Creates HTML output based on Build IDs. The HTML output also
     * contains a style tag used to put borders and centralize text
     * in cells.
     *
     * @param buildIds The list of Build IDs currently in the log folder
     * @return An HTML table containing Build ID, date and Commit ID for all builds
     */ 
    private String createHTMLTableWithLogSummaries(List<String> buildIds) {
        StringBuilder logTable = new StringBuilder();

        logTable.append("<table><tr><td> Build ID </td><td> Date </td><td> Commit ID </td></tr>");

        for (String buildId : buildIds) {
            try {
                logTable.append(getBuildLogHTMLTableRow(buildId));
            } catch (IOException e) {
                continue;
            }
        }
        logTable.append("</table>");
        logTable.append("<style>table, th, td {border: 1px solid black;border-collapse: collapse;text-align: center;}</style>");
        return logTable.toString();
    }

    /**
     * Searches the log directory for log files, and returns a string.
     * The string contains a HTML table, with rows for each log entry
     * and cells with Build ID, date and Commit ID.
     * @return A string containing information of all logs in the log directory.
     */
    public String getBuilds() {
        Path logsFolderPath = logsFolder.toPath();

        // List to store all build IDs found in the log folder
        List<String> buildIds = new ArrayList<>();

        // Iterate the log directory and save file names that have
        // the ".log" extension
		try ( Stream<Path> paths = Files.walk(logsFolderPath)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                String buildId = path.getFileName().toString();
                
                if(!buildId.endsWith(".log")) {
                    continue;
                }

                // Extract the file name without extension and
                // append to buildIds
                buildId = buildId.substring(0, buildId.length() - 4);
                buildIds.add(buildId);
            }
	    } catch (Exception e) {
            return null;
        }

        // Sort the buildId list to display the 
        // builds in ascending order
        buildIds.sort(Comparator.comparingInt(Integer::parseInt));
        String logTable;
        // Get an HTML table containing summaries for 
        // the found buildIds
        logTable = createHTMLTableWithLogSummaries(buildIds);

        return logTable;
    }

    /**
     * Get the next number to be
     * used for the name of a new
     * log file
     *
     * @return The next number
     */ 
    private int getLogCount() {
        Path logsFolderPath = logsFolder.toPath();

        List<Path> fileList;
        try (Stream<Path> logFiles = Files.walk(logsFolderPath)){
            fileList = logFiles.filter(fileName -> fileName.getFileName().toString().endsWith(".log")).toList();
        } catch (IOException e) {
            return 0;
        }

        return fileList.size();
    }

    /**
     * Stores a build log in a log file
     * The log file is named in ascending
     * order from the previously created
     * log file.
     *
     * @param log The output from building the project
     * @param commitId The commit id used to identify a specific log
     */ 
    public void storeBuildLog(String log, String commitId) {
        StringBuilder fileName = new StringBuilder();
        int nextNumber = getLogCount() + 1;

        fileName.append(logsFolder.getPath()).append("/").append(nextNumber).append(".log");
        File logFile = new File(fileName.toString());

        if(logFile.exists()) {
            return;
        }

        StringBuilder fullLog = new StringBuilder();
        fullLog.append("Commit ID: ").append(commitId).append("\n");
        fullLog.append("Build date: ").append(LocalDate.now().toString()).append("\n");
        fullLog.append(log);
        


        try {
            logFile.createNewFile();
            Files.writeString(Path.of(fileName.toString()), fullLog.toString());
        } catch (Exception e) {
            return;
        }
    }
 
    /**
     * Starts a new server with port 8080 and the default log directory.
     * @param args Not used
     * @throws Exception
     */
    public static void main(String[] args) throws Exception
    {
		Dotenv dotenv = Dotenv.load();
		String webhookSignature = dotenv.get("WEBHOOK_SIGNATURE");
		if (webhookSignature == null || webhookSignature.isEmpty()) {
			throw new IllegalStateException("env variable WEBHOOK_SIGNATURE must be set in .env file");
		}
        String githubToken = dotenv.get("GITHUB_TOKEN");
        if (githubToken == null || githubToken.isEmpty()) {
			throw new IllegalStateException("env variable GITHUB_TOKEN must be set in .env file");
		}
        Server server = new Server(8080);
        server.setHandler(new ContinuousIntegrationServer(webhookSignature, githubToken)); 
        server.start();
        server.join();
    }
}
