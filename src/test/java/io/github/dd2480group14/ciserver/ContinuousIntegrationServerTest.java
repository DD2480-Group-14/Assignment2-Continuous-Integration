package io.github.dd2480group14.ciserver;

import java.io.*;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.List;
import java.util.Scanner;


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
     * log with the given message. Retrieveing the log
     * should return the same message.
     */
    @Test
    public void getBuildLogPositive(@TempDir Path path) {
        String message = "Text in file\n";
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
    }


    /**
     * Creates a new server with empty log folder.
     * Trying to retreive a log should return null.
     */
    @Test
    public void getBuildLogNegative(@TempDir Path path) {
        File dir = path.toFile();

        ContinuousIntegrationServer ciServer = new ContinuousIntegrationServer(dir);
        assertNull(ciServer.getBuildLog("1"));
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
	 * Clones a git repository and
	 * verifies that the .git directory
	 * exists
	 */
	@Test
	public void runGitClone() throws IOException, InterruptedException {
		ContinuousIntegrationServer continuousIntegrationServer = new ContinuousIntegrationServer();
		String url = "https://github.com/octocat/Hello-World.git";
		String repositoryName = "Hello-World";
		File tempDirectory = continuousIntegrationServer.gitClone(url);
		File repositoryFolder = new File(tempDirectory, repositoryName);
		boolean gitFolderExists = new File(repositoryFolder, ".git").exists();
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
        String buildId = "test";
        String log = "Text in file";

        continuousIntegrationServer.storeBuildLog(log, buildId);

        File expectedFile = new File(dir.getPath() + "/" + buildId + ".log");

        StringBuilder stringBuilder = new StringBuilder();
        try (Scanner scanner = new Scanner(expectedFile)) {
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine()).append("\n");
            }
        } catch (FileNotFoundException e) {
            assertTrue(false);
        }

        String message = stringBuilder.toString();
        StringBuilder actualMessage = new StringBuilder();
        actualMessage.append("Build id (commit): ").append(buildId).append("\n");
        actualMessage.append("Build date: ").append(LocalDate.now().toString()).append("\n");
        actualMessage.append(log).append("\n");
        assertEquals(message, actualMessage.toString());
    }

    /**
     * Creates a file beforehand, which should 
     * result in nothing being stored when trying
     * to store a build log
     */ 
    @Test
    public void writeLogNegative(@TempDir Path path) {
        File dir = path.toFile();
        ContinuousIntegrationServer continuousIntegrationServer = new ContinuousIntegrationServer(dir);
        String buildId = "test";
        String log = "Text in file";

        File testFile = new File(dir.getPath() + "/" + buildId + ".log");

        try {
        testFile.createNewFile();
        } catch (Exception e) {
            assertTrue(false);
        }

        continuousIntegrationServer.storeBuildLog(log, buildId);

        assertEquals(0, testFile.length());
    }
}

