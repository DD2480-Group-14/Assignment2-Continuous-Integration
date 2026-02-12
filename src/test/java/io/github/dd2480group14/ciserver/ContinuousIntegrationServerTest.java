package io.github.dd2480group14.ciserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.HmacUtils;
import org.eclipse.jetty.server.Request;
import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Unit test for Conitinuous Integration Server.
 */
public class ContinuousIntegrationServerTest {


	String testSignature = "test";
	String testToken = "test";

	/**
	 * The command "echo Testing" should
	 * give the output "Testing"
	 * runCommand should therefor return
	 * "Testing"
	 */
	@Test
	public void runCommandOutput() throws IOException, InterruptedException {
		ContinuousIntegrationServer continuousIntegrationServer = new ContinuousIntegrationServer(testSignature, testToken);
		File directory = new File("./");
		List<String> command = List.of("echo", "Testing");
		String output = continuousIntegrationServer.runCommand(command, directory);
		assertEquals("Testing", output);
	}


    /**
     * Creates a new server with a log folder with a 
     * log with the given message. 
	 * Retrieveing the log should return the same message.
	 * Also, retrieving the summary should return the appropriate summary.
     * @param path
     */
    @Test
    public void getBuildLogPositive(@TempDir Path path) throws IOException {
		String commitId = "123";
		String buildDate = "2022-01-01";
        String message = "Commit ID: " + commitId
					   + "\nBuild date: " + buildDate
					   + "\nAdditional log content\n";
        String summary = "<tr><td><a href=\"/logs/1\"</a>1</td><td>" + buildDate + "</td><td>" + commitId + "</td></tr>";

        File dir = path.toFile();
        File log = new File(dir.getPath() + "/1.log");
        try {
            log.createNewFile();
        } catch (IOException e) {
            assertTrue(false);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(log, true))) {
            
            writer.write(message);
            
        } catch (IOException e) {
            assertTrue(false);
        }

        ContinuousIntegrationServer ciServer = new ContinuousIntegrationServer(testSignature, testToken, dir);
        assertEquals(message, ciServer.getBuildLog("1"));
        assertEquals(summary, ciServer.getBuildLogHTMLTableRow("1"));
    }

	/**
     * Creates a new server with a log folder with a 
     * log with the given message. 
	 * Retrieveing the log should return the same message.
	 * Also, retrieving the summary should return the appropriate summary.
	 * Note that build date field in the summary should be null
	 * since it does not exist.
     * @param path
     */
    @Test
    public void getBuildLogPositiveNoBuildDate(@TempDir Path path) throws IOException {
		String commitId = "123";
        String message = "Commit ID: " + commitId
					   + "\nAdditional log content\n";
        String summary = "<tr><td><a href=\"/logs/1\"</a>1</td><td>" + null + "</td><td>" + commitId + "</td></tr>";

        File dir = path.toFile();
        File log = new File(dir.getPath() + "/1.log");
        try {
            log.createNewFile();
        } catch (IOException e) {
            assertTrue(false);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(log, true))) {
            
            writer.write(message);
            
        } catch (IOException e) {
            assertTrue(false);
        }

        ContinuousIntegrationServer ciServer = new ContinuousIntegrationServer(testSignature, testToken, dir);
        assertEquals(message, ciServer.getBuildLog("1"));
        assertEquals(summary, ciServer.getBuildLogHTMLTableRow("1"));
    }

    /**
     * Creates a new server with empty log folder.
     * Trying to retreive a log (or its summary) should throw
     * NoSuchFileException.
     * @param path
     */
    @Test
    public void getBuildLogNegative(@TempDir Path path) throws IOException {
        File dir = path.toFile();
        ContinuousIntegrationServer ciServer = new ContinuousIntegrationServer(testSignature, testToken, dir);
		assertThrows(FileNotFoundException.class, () -> ciServer.getBuildLog("1"));
		assertThrows(FileNotFoundException.class, () -> ciServer.getBuildLogHTMLTableRow("1"));
    }


	/**
	 * Creates a new server with empty log folder, and log file
	 * outside of this folder. Trying to retrieve 
	 * the log should throw IllegalArgumentException
     * @param path
	 */
	@Test
	public void getBuildLogOutsideOfLogsFolder(@TempDir Path path) throws IOException {
		File directory = path.toFile();
		File testFile = new File(directory + "/../42304892.log");
		testFile.createNewFile();
		testFile.deleteOnExit();
		ContinuousIntegrationServer ciServer = new ContinuousIntegrationServer(testSignature, testToken, directory);
		assertThrows(IllegalArgumentException.class, () -> ciServer.getBuildLog("../42304892"));
	}

	/**
	 * The command "Fakecommand" does usually
	 * not exist in most Unix OS
	 * runCommand should therefor throw
	 * an IOException
	 */
	@Test
	public void runCommandFail() {
		ContinuousIntegrationServer continuousIntegrationServer = new ContinuousIntegrationServer(testSignature, testToken);
		File directory = new File("./");
		List<String> command = List.of("Fakecommand");
		assertThrows(IOException.class, () -> continuousIntegrationServer.runCommand(command, directory));
	}

	/**
	 * Verifies that a push payload is correctly parsed.
	 * The PushEventInfo should have the correct 
	 * repository URL, commit SHA, branch name, author and commit message.
	 */
	@Test
	public void fromJSONCorrectParsing() {
		String payload = """
			{
				"ref": "refs/heads/example",
				"after": "123123",
				"repository": {
					"clone_url": "https://github.com/test/example.git"
				},
				"pusher": {
					"name": "test-user"
				},
				"commits": [
					{
						"message": "Initial commit"
					}
				]
			}
			""";

		JSONObject json = new JSONObject(payload);

		PushEventInfo info = PushEventInfo.fromJSON(json);

		assertEquals("https://github.com/test/example.git", info.repoURL());
		assertEquals("123123", info.SHA());
		assertEquals("example", info.branch());
		assertEquals("test-user", info.author());
		assertEquals("Initial commit", info.commitMessage());
	}

    /**
     * Verifies that owner defaults to "Unknown" 
     * when owner field is missing from the payload.
     */
    @Test
    public void fromJSONMissingOwnerField() {
		String payload = """
			{
				"ref": "refs/heads/main",
				"after": "123123",
				"repository": {
					"clone_url": "https://github.com/test/example.git"
				},
				"pusher": {
					"name": "test-user"
				}
			}
			""";

        JSONObject json = new JSONObject(payload);

        PushEventInfo info = PushEventInfo.fromJSON(json);
        assertEquals("Unknown", info.owner());
    }



}
