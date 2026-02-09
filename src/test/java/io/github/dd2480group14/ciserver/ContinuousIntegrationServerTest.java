package io.github.dd2480group14.ciserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.*;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit test for simple App.
 */
public class ContinuousIntegrationServerTest {

    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
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
        assertEquals(message, ciServer.getBuildLog(1));
    }


    /**
     * Creates a new server with empty log folder.
     * Trying to retreive a log should give the 
     * message: "Log not found."
     */
    @Test
    public void getBuildLogNegative(@TempDir Path path) {
        File dir = path.toFile();

        ContinuousIntegrationServer ciServer = new ContinuousIntegrationServer(dir);
        assertEquals("Log not found.", ciServer.getBuildLog(1));
    }
}

