package io.github.dd2480group14.ciserver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
 
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.nio.file.Files;
import java.util.List;
import java.io.BufferedReader;
import java.io.File;
 
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/** 
 Skeleton of a ContinuousIntegrationServer which acts as webhook
 See the Jetty documentation for API documentation of those classes.
*/
public class ContinuousIntegrationServer extends AbstractHandler
{
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

    void removeGitDir() {
        return;
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

    String getBuildLog(String identifier) {
        return "Text";
    }

    String getBuilds() {
        return "Builds";
    }

    void storeBuildLog(String identifier, String log) {
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
