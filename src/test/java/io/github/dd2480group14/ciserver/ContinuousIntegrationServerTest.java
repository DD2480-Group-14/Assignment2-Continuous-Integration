package io.github.dd2480group14.ciserver;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}

