import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.text.*;
import java.net.Socket;
import pilots.runtime.*;
import pilots.runtime.errsig.*;

public class ThresholdCheck_new extends PilotsRuntime {
    private static Logger LOGGER = Logger.getLogger(ThresholdCheck_new.class.getName());
    private int currentMode;
    private int currentModeCount;
    private int time; // msec
    private long[] nextSendTimes;

    private static final double A_LOW = -0.0884;
    private static final double A_HIGH = 0.0884;

    public ThresholdCheck_new(String args[]) {
        super(args);

        time = 0;
        nextSendTimes = new long[1];
        Arrays.fill(nextSendTimes, 0L);
    }

    public void produceOutputs() {
        try {
            openOutput(0, "a", "b", "mode");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        final int interval = 1000;
        Map<String, Double> data = new HashMap<>();
        while (!isEndTime()) {
            // Inputs
            data.put("a", getData("a", new Method(Method.CLOSEST, "t")));
            data.put("b", getData("b", new Method(Method.CLOSEST, "t")));
            LOGGER.fine("Inputs: " + "a=" + data.get("a") + ", " + "b=" + data.get("b"));

            // Errors computation
            data.put("e1", data.get("b")-data.get("a"));
            LOGGER.fine("Errors: " + "e1=" + data.get("e1"));

            // Error detection
            int mode = -1;
            if ( ( data.get("e1") >= A_LOW && data.get("e1") <= A_HIGH ) || data.get("e1")>A_HIGH) {
                mode = 0;	// "Normal"
            } else if ( data.get("e1")<=A_LOW) {
                mode = 1;	// "Failure"
            } else if ( ( data.get("e1") >= A_LOW && data.get("e1") <= A_HIGH ) || ( data.get("e1") >= 3 && data.get("e1") <= 5 )) {
                mode = 2;	// "mode 2"
            }
            LOGGER.fine("Detected: mode=" + mode);

            // Data transfer
            Date now = getTime();
            try {
                sendData(0, data.get("a"), data.get("b"), mode);
                LOGGER.info("Outputs: " + now + " " + "a=" + data.get("a") + " " + "b=" + data.get("b") + " " + "mode=" + mode + " ");
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            time += interval;
            progressTime(interval);
        }

        LOGGER.info("Finished at " + getTime());
    }

    public static void main(String[] args) {
        ThresholdCheck_new app = new ThresholdCheck_new(args);
        app.startServer();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Hit ENTER key after running input producer(s).");
        try {
            reader.readLine();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        app.produceOutputs();
    }
}
