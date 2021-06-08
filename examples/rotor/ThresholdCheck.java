import java.util.*;
import java.util.logging.*;
import java.text.*;
import java.net.Socket;
import pilots.runtime.*;
import pilots.runtime.errsig.*;

public class ThresholdCheck extends PilotsRuntime {
    private static Logger LOGGER = Logger.getLogger(ThresholdCheck.class.getName());
    private int currentMode;
    private int currentModeCount;
    private Timer timer;
    private long[] nextSendTimes;

    private static final double A_LOW = -0.0884;
    private static final double A_HIGH = 0.0884;

    public ThresholdCheck(String args[]) {
        super(args);

        timer = new Timer();
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
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                // Inputs
                data.put("a", getData("a", new Method(Method.CLOSEST, "t")));
                data.put("b", getData("b", new Method(Method.CLOSEST, "t")));
                LOGGER.fine("Inputs: " + "a=" + data.get("a") + ", " + "b=" + data.get("b"));

                // Errors computation
                data.put("e1", data.get("b")-data.get("a"));
                LOGGER.fine("Errors: " + "e1=" + data.get("e1"));

                // Error detection
                int mode = -1;
                if ((data.get("e1")>A_LOW) && data.get("e1")<A_HIGH) {
                    mode = 0;	// "Normal"
                } else if (data.get("e1")<=A_LOW) {
                    mode = 1;	// "Failure"
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

            }
        }, 0, interval);
    }

    public static void main(String[] args) {
        ThresholdCheck app = new ThresholdCheck(args);
        app.startServer();
        app.produceOutputs();
    }
}
