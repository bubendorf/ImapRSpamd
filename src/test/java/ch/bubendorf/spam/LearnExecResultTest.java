package ch.bubendorf.spam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LearnExecResultTest {
    private ExecResult execResult;

    @BeforeEach
    public void setup() {
        execResult = new ExecResult(0, "Results for file: stdin (0.019 seconds)\n" +
                "success = true;\n" +
                "filename = \"stdin\";\n" +
                "scan_time = 0.020000;\n" +
                "error = \"<2129771540.22645.1643036940491@789ac7b1-778f-410d-7b83-3144> has been already learned as ham, ignore it\";\n" +
                "\n", "");
    }

    @Test
    public void getScanTime() {
        assertEquals(0.02, execResult.getScanTime());
    }

    @Test
    public void isSuccess() {
        assertTrue(execResult.isSuccess());
    }

    @Test
    public void getError() {
        assertEquals("<2129771540.22645.1643036940491@789ac7b1-778f-410d-7b83-3144> has been already learned as ham, ignore it", execResult.getError());
    }


}
