import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.text.*;
import java.net.Socket;
import pilots.runtime.*;
import pilots.runtime.errsig.*;

public class Rotor_check_mode extends PilotsRuntime {
    private static Logger LOGGER = Logger.getLogger(Rotor_check_mode.class.getName());
    private int currentMode;
    private int currentModeCount;
    private int time; // msec
    private long[] nextSendTimes;

    private static final double R_LOW = -0.0153;
    private static final double R_HIGH = 0.006;
    private static final double P_LOW = -0.0884;
    private static final double P_HIGH = -0.0703;
    private static final double Y_LOW = -0.0040;
    private static final double Y_HIGH = 0.0022;

    public Rotor_check_mode(String args[]) {
        super(args);

        time = 0;
        nextSendTimes = new long[1];
        Arrays.fill(nextSendTimes, 0L);
    }

    public void produceOutputs() {
        try {
            openOutput(0, "roll", "pitch", "yaw", "mode");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        final int interval = 100;
        Map<String, Double> data = new HashMap<>();
        while (!isEndTime()) {
            // Inputs
            data.put("roll", getData("roll", new Method(Method.CLOSEST, "t")));
            data.put("pitch", getData("pitch", new Method(Method.CLOSEST, "t")));
            data.put("yaw", getData("yaw", new Method(Method.CLOSEST, "t")));
            LOGGER.fine("Inputs: " + "roll=" + data.get("roll") + ", " + "pitch=" + data.get("pitch") + ", " + "yaw=" + data.get("yaw"));

            // Errors computation
            data.put("e1", data.get("roll"));
            data.put("e2", data.get("pitch"));
            data.put("e3", data.get("yaw"));
            LOGGER.fine("Errors: " + "e1=" + data.get("e1") + ", " + "e2=" + data.get("e2") + ", " + "e3=" + data.get("e3"));

            // Error detection
            int mode = -1;
            if (data.get("e2")>=P_LOW && data.get("e2")<=P_HIGH || data.get("e2")<P_LOW && data.get("e3")>=Y_LOW && data.get("e3")<=Y_HIGH || data.get("e2")<P_LOW && data.get("e3")>Y_HIGH && data.get("e1")>=R_LOW && data.get("e1")<=R_HIGH) {
                mode = 0;	// "Normal"
            } else if (data.get("e2")<P_LOW && data.get("e3")<Y_LOW) {
                mode = 1;	// "Rotor 1 failure"
            } else if (data.get("e2")<P_LOW && data.get("e3")>Y_HIGH && data.get("e1")<R_LOW) {
                mode = 2;	// "Rotor 2 failure"
            } else if (data.get("e2")<P_LOW && data.get("e3")>Y_HIGH && data.get("e1")>R_HIGH) {
                mode = 3;	// "Rotor 6 failure"
            }
            LOGGER.fine("Detected: mode=" + mode);

            // Data transfer
            Date now = getTime();
            try {
                sendData(0, data.get("roll"), data.get("pitch"), data.get("yaw"), mode);
                LOGGER.info("Outputs: " + now + " " + "roll=" + data.get("roll") + " " + "pitch=" + data.get("pitch") + " " + "yaw=" + data.get("yaw") + " " + "mode=" + mode + " ");
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            time += interval;
            progressTime(interval);
        }

        LOGGER.info("Finished at " + getTime());
    }

    public static void main(String[] args) {
        Rotor_check_mode app = new Rotor_check_mode(args);
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
