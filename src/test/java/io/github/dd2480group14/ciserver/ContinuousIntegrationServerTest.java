package io.github.dd2480group14.ciserver;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.List;


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

