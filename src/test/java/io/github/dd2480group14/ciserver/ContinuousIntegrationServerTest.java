package io.github.dd2480group14.ciserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Unit test for Conitinuous Integration Server.
 */
public class ContinuousIntegrationServerTest {
	/**
	 * The command "echo Testing" should
	 * give the output "Testing"
	 * runCommand should therefor return
	 * "Testing"
	 */
	@Test
	public void runCommandOutput() throws IOException, InterruptedException {
		ContinuousIntegrationServer continuousIntegrationServer = new ContinuousIntegrationServer();
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

        ContinuousIntegrationServer ciServer = new ContinuousIntegrationServer(dir);
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

        ContinuousIntegrationServer ciServer = new ContinuousIntegrationServer(dir);
        assertEquals(message, ciServer.getBuildLog("1"));
        assertEquals(summary, ciServer.getBuildLogHTMLTableRow("1"));
    }

    /**
     * Creates a new server with empty log folder.
     * Trying to retreive a log (or its summary) should throw
     * NoSuchFileException.
     */
    @Test
    public void getBuildLogNegative(@TempDir Path path) throws IOException {
        File dir = path.toFile();
        ContinuousIntegrationServer ciServer = new ContinuousIntegrationServer(dir);
		assertThrows(FileNotFoundException.class, () -> ciServer.getBuildLog("1"));
		assertThrows(FileNotFoundException.class, () -> ciServer.getBuildLogHTMLTableRow("1"));
    }


	/**
	 * Creates a new server with empty log folder, and log file
	 * outside of this folder. Trying to retrieve 
	 * the log should throw IllegalArgumentException
	 */
	@Test
	public void getBuildLogOutsideOfLogsFolder(@TempDir Path path) throws IOException {
		File directory = path.toFile();
		File testFile = new File(directory + "/../42304892.log");
		System.out.println(testFile.toString());
		testFile.createNewFile();
		testFile.deleteOnExit();
		ContinuousIntegrationServer ciServer = new ContinuousIntegrationServer(directory);
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
		ContinuousIntegrationServer continuousIntegrationServer = new ContinuousIntegrationServer();
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
	public void testExtractPushInfo() {
		String payload = """
			{
				"ref": "refs/heads/example",
				"after": "123123",
				"repository": {
					"clone_url": "https://github.com/test/example.git",
					"name": "test"
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

		assertEquals("https://github.com/test/example.git", info.repositoryURL());
		assertEquals("123123", info.SHA());
		assertEquals("example", info.branch());
		assertEquals("test-user", info.author());
		assertEquals("Initial commit", info.commitMessage());
	}

	/**
	 * Should throw an exception when payload lacks required fields.
	 */
	@Test
	public void testNonPushWebhookEvent() {
		String payload = "{}";

		JSONObject json = new JSONObject(payload);

		ContinuousIntegrationServer continuousIntegrationServer = new ContinuousIntegrationServer();

		assertThrows(IllegalArgumentException.class, () -> PushEventInfo.fromJSON(json));
	}
	/**
	 * Create a temporary git repository,
	 * clones it and verifies that the
	 * .git directory exists
	 */
	@Test
	public void runGitClone(@TempDir Path path) throws IOException, InterruptedException {
		ContinuousIntegrationServer continuousIntegrationServer = new ContinuousIntegrationServer();
		continuousIntegrationServer.runCommand(List.of("git", "init"), path.toFile());
		String url = path.toString();
		File tempDirectory = continuousIntegrationServer.gitClone(url, null);
		boolean gitFolderExists = new File(tempDirectory, ".git").exists();
		assertTrue(gitFolderExists);
	}

    /**
     * Creates a new server and writes a log 
     * The file contents should be the same as expected
     */ 
    @Test
    public void writeLogPositive(@TempDir Path path) {
        File dir = path.toFile();
        ContinuousIntegrationServer continuousIntegrationServer = new ContinuousIntegrationServer(dir);
        String commitId = "test";
        String log = "Text in file";

        continuousIntegrationServer.storeBuildLog(log, commitId);

        File expectedFile = new File(dir.getPath() + "/1.log");

        StringBuilder stringBuilder = new StringBuilder();
        try (Scanner scanner = new Scanner(expectedFile)) {
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine()).append("\n");
            }
        } catch (FileNotFoundException e) {
            assertTrue(false);
        }

        String message = stringBuilder.toString();
        StringBuilder expectedMessage = new StringBuilder();
        expectedMessage.append("Commit ID: ").append(commitId).append("\n");
        expectedMessage.append("Build date: ").append(LocalDate.now().toString()).append("\n");
        expectedMessage.append(log).append("\n");
        assertEquals(message, expectedMessage.toString());
    }

    /**
     * Create and store several log files
     * to test that they are named properly.
     * Should be named 1.log, 2.log, 3.log and 
     * 4.log
     */ 
    @Test
    public void writeLogSeveral(@TempDir Path path) {
        File dir = path.toFile();
        ContinuousIntegrationServer ciServer = new ContinuousIntegrationServer(dir);
        String commitId = "test";
        String log = "Text in file";
        
        ciServer.storeBuildLog(log, commitId + " 1");
        ciServer.storeBuildLog(log, commitId + " 2");
        ciServer.storeBuildLog(log, commitId + " 3");
        ciServer.storeBuildLog(log, commitId + " 4");

        File log1 = new File(dir.getPath() + "/1.log");
        File log2 = new File(dir.getPath() + "/2.log");
        File log3 = new File(dir.getPath() + "/3.log");
        File log4 = new File(dir.getPath() + "/4.log");

        assertTrue(log1.exists());
        assertTrue(log2.exists());
        assertTrue(log3.exists());
        assertTrue(log4.exists());
        
    }

    /** 
     * Runs "runTests" for a small maven project.
     * The build should be successfull
     */
    @Test
    public void runMavenTestSuccessfull() throws IOException, InterruptedException {
        ContinuousIntegrationServer continuousIntegrationServer = new ContinuousIntegrationServer();
        File directory = new File("src/test/resources/maven-projects/small-maven-success");
        String output = continuousIntegrationServer.runTests(directory);
        assertTrue(output.contains("BUILD SUCCESS"));
    }

    /**
     * Runs "runTests" for a small maven project.
     * The build should fail
     */
    @Test
    public void runMavenTestFail() throws IOException, InterruptedException {
        ContinuousIntegrationServer continuousIntegrationServer = new ContinuousIntegrationServer();
        File directory = new File("src/test/resources/maven-projects/small-maven-fail");
        String output = continuousIntegrationServer.runTests(directory);
        assertTrue(output.contains("BUILD FAIL"));
    }
  
	/**
	 * Creates a temporary directory with
	 * a file one level down. Verifies
	 * that both the directory and file
	 * is removed
	 */
	@Test
	public void removeDirectoryAndSubfileInTmp() throws IOException, InterruptedException {
		ContinuousIntegrationServer continuousIntegrationServer = new ContinuousIntegrationServer();
		File directory = Files.createTempDirectory("test").toFile();
		String testFileName = "test.py";
		File testFile = new File(directory, testFileName);
		List<String> createFileCommand = List.of("touch", testFileName);
		continuousIntegrationServer.runCommand(createFileCommand, directory);
		assertTrue(directory.exists());
		assertTrue(testFile.exists());
		continuousIntegrationServer.removeDirectoryInTmp(directory);
		assertFalse(directory.exists());
		assertFalse(testFile.exists());
	}

	/**
	 * Try to remove root of project
	 * should fail because its outside
	 * of the system's temporary folder
	 */
	@Test
	public void removeDirectoryOutsideOfTmp() throws IOException, InterruptedException {
		ContinuousIntegrationServer continuousIntegrationServer = new ContinuousIntegrationServer();
		File directory = new File("./");
		assertTrue(directory.exists());
		assertThrows(IllegalArgumentException.class, () -> continuousIntegrationServer.removeDirectoryInTmp(directory));
		assertTrue(directory.exists());
	}

    /**
     * Gets all build logs, which in this case is 
     * 2. The whole message should be equal to
     * the log header + the log summaries
     */ 
    @Test
    public void getAllBuildLogsPositive(@TempDir Path path) throws IOException {
        String message1 = "Commit ID: 1\nBuild date: " + LocalDate.now().toString();
        String message2 = "Commit ID: 2\nBuild date: " + LocalDate.now().toString();
        File dir = path.toFile();
        File log1 = new File(dir.getPath() + "/1.log");
        File log2 = new File(dir.getPath() + "/2.log");

        try {
            log1.createNewFile();
            log2.createNewFile();
        } catch (IOException e) {
            assertTrue(false);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(log1, true))) {
            
            writer.write(message1);
            
        } catch (IOException e) {
            assertTrue(false);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(log2, true))) {
            
            writer.write(message2);
            
        } catch (IOException e) {
            assertTrue(false);
        }

        String logListFirst = "<table><tr><td> Build ID </td><td> Date </td><td> Commit ID </td></tr>";
        String logListSecond = "<tr><td><a href=\"/logs/1\"</a>1</td><td>" + LocalDate.now().toString() + "</td><td>1</td></tr>";
        String logListThird = "<tr><td><a href=\"/logs/2\"</a>2</td><td>" + LocalDate.now().toString() + "</td><td>2</td></tr>";

        String logListFourth = "</table>";
        String logListStyle = "<style>table, th, td {border: 1px solid black;border-collapse: collapse;text-align: center;}</style>";

        String fullLogList = logListFirst + logListSecond + logListThird + logListFourth + logListStyle;

        ContinuousIntegrationServer ciServer = new ContinuousIntegrationServer(dir);
        assertEquals(fullLogList, ciServer.getBuilds());
    }

    /**
     * Tries to get build logs when there are no
     * .log files in the directory. Should return
     * an only a HTML string containing a p element
     */ 
    @Test
    public void getAllBuildLogsNegative(@TempDir Path path) throws IOException {
        File dir = path.toFile();
        File testFile = new File(dir.getPath() + "/log.txt");
        String message = "This should not be read";

        try {
        testFile.createNewFile();
        } catch (IOException e) {
            assertTrue(false);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(testFile, true))) {
            writer.write(message);
        } catch (IOException e) {
            assertTrue(false);
        }

        ContinuousIntegrationServer ciServer = new ContinuousIntegrationServer(dir);
        String logListEmpty = "<table><tr><td> Build ID </td><td> Date </td><td> Commit ID </td></tr></table><style>table, th, td {border: 1px solid black;border-collapse: collapse;text-align: center;}</style>";
        assertEquals(logListEmpty, ciServer.getBuilds());
    }
}

