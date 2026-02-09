package io.github.dd2480group14.ciserver;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

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

		ContinuousIntegrationServer continuousIntegrationServer = new ContinuousIntegrationServer();
		PushEventInfo info = continuousIntegrationServer.extractPushInfo(json);

		assertEquals("https://github.com/test/example.git", info.getRepoURL());
		assertEquals("123123", info.getSHA());
		assertEquals("example", info.getBranch());
		assertEquals("test-user", info.getAuthor());
		assertEquals("Initial commit", info.getCommitMessage());
	}

	/**
	 *Should throw an exception when payload is not empty or not a push event.
	 */
	@Test
	public void testNonPushWebhookEvent() {
		String payload = "{}";

		JSONObject json = new JSONObject(payload);

		ContinuousIntegrationServer continuousIntegrationServer = new ContinuousIntegrationServer();

		assertThrows(org.json.JSONException.class, () -> continuousIntegrationServer.extractPushInfo(json));
	}
}
