package io.github.dd2480group14.ciserver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

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

import java.time.LocalDate;

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

    /**
     * Get the next number to be
     * used for the name of a new
     * log file
     *
     * @return The next number
     */ 
    private int getLogFilesNextNumber() {
        Path logsFolderPath = logsFolder.toPath();

        List<Path> fileList;
        try (Stream<Path> logFiles = Files.walk(logsFolderPath)){
            fileList = logFiles.filter(fileName -> fileName.getFileName().toString().endsWith(".log")).toList();
        } catch (IOException e) {
            return -1;
        }

        return fileList.size() + 1;
    }

    /**
     * Stores a build log in a log file
     * The log file is named buildId.log,
     * where the buildId could be a commit
     * id
     *
     * @param log The output from building the project
     * @param buildId The commit id used to identify a specific log
     */ 
    void storeBuildLog(String log, String buildId) {
        StringBuilder fileName = new StringBuilder();
        int nextNumber = getLogFilesNextNumber();
        nextNumber = (nextNumber == -1) ? 1 : nextNumber;

        fileName.append(logsFolder.getPath()).append("/").append(nextNumber).append(".log");
        File logFile = new File(fileName.toString());

        if(logFile.exists()) {
            return;
        }

        StringBuilder fullLog = new StringBuilder();
        fullLog.append("Build id (commit): ").append(buildId).append("\n");
        fullLog.append("Build date: ").append(LocalDate.now().toString()).append("\n");
        fullLog.append(log);
        


        try {
            logFile.createNewFile();
            Files.writeString(Path.of(fileName.toString()), fullLog.toString());
        } catch (Exception e) {
            return;
        }
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
