package io.github.dd2480group14.ciserver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
 
import java.io.*;

import java.util.Scanner;
 
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

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
     * Opens a new terminal in the directory dir
     * and runs the given command.
     * @param command The command to run.
     * @param dir The directory to run it in.
     * @return Returns the terminal output after the command.
     */
    String runCommand(String command, File dir) {
        return "Output";
    }

    void gitClone(String url) {
        return;
    }

    void removeGitDir() {
        return;
    }

    String runTests(File dir) {
        return "log";
    }

    String getBuildLog(int buildId) {
        File file = new File(logsFolder.getPath() + "/" + buildId + ".log");
        StringBuilder stringBuilder = new StringBuilder();

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
            stringBuilder.append(scanner.nextLine()).append("\n");
            }
        } catch (FileNotFoundException e) {
            return "Log not found.";
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
