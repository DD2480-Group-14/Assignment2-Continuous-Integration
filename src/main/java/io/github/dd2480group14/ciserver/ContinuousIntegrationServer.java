package io.github.dd2480group14.ciserver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
 
import java.util.Arrays;

import java.io.*;

import java.util.Scanner;
import java.util.stream.Stream;
import java.util.Comparator;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/** 
 *A ContinuousIntegrationServer which acts as webhook.
 */
public class ContinuousIntegrationServer extends AbstractHandler {
    private final File logsFolder;

    /**
     * Constructs a new ContinuousIntegrationServer instance with the default logs folder path.
     */
    public ContinuousIntegrationServer() {
        logsFolder = new File("logs");

        if (!logsFolder.exists()) {
            logsFolder.mkdir();
        }

        if (logsFolder.isFile()) {
            throw new IllegalArgumentException("logsFolder can not be an already existing file.");
        }
    }
    
    /**
     * Constructs a new ContinuousIntegrationServer instance with a specified logs folder path.
     * 
     * @param logsFolder The specified logs folder.
     */
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

    private void handlePost(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException, ServletException
    {
        // here you do all the continuous integration tasks
        // for example
        // 1st clone your repository
        // 2nd compile the code

        response.getWriter().println("CI job done");
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
     * Returns a string containing information of all logs in the log directory.
     * @return A string containing information of all logs in the log directory.
     */
    public String getBuilds() {
        return "TODO";
    }

    public void storeBuildLog(String log) {
        return;
    }
 
    /**
     * Starts a new server with port 8080 and the default log directory.
     * @param args Not used
     * @throws Exception
     */
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8080);
        server.setHandler(new ContinuousIntegrationServer()); 
        server.start();
        server.join();
    }
}
