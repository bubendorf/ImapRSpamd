package ch.bubendorf.spam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LearnExecResultErrorTest {
    private ExecResult execResult;

    @BeforeEach
    public void setup() {
        execResult = new ExecResult("", 0, """
                Results for file: stdin (0.007 seconds)
                error = "all learn conditions denied learning spam in default classifier";
                filename = "stdin";
                scan_time = 0.008000;

                """, "");
    }

    @Test
    public void getScanTime() {
        assertEquals(0.008, execResult.getScanTime());
    }

    @Test
    public void isSuccess() {
        assertFalse(execResult.isSuccess());
    }

    @Test
    public void isIgnore() {
        assertTrue(execResult.isIgnore());
    }

    @Test
    public void getError() {
        assertEquals("all learn conditions denied learning spam in default classifier", execResult.getError());
    }


}
