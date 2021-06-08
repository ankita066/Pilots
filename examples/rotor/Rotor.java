import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.text.*;
import java.net.Socket;
import pilots.runtime.*;
import pilots.runtime.errsig.*;

public class Rotor extends PilotsRuntime {
    private static Logger LOGGER = Logger.getLogger(Rotor.class.getName());
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

    public Rotor(String args[]) {
        super(args);

        time = 0;
        nextSendTimes = new long[1];
        Arrays.fill(nextSendTimes, 0L);
    }

    public void produceOutputs() {
        try {
            openOutput(0, "roll", "pitch", "yaw");
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

            // Data transfer
            Date now = getTime();
            try {
                sendData(0, data.get("roll"), data.get("pitch"), data.get("yaw"));
                LOGGER.info("Outputs: " + now + " " + "roll=" + data.get("roll") + " " + "pitch=" + data.get("pitch") + " " + "yaw=" + data.get("yaw") + " ");
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            time += interval;
            progressTime(interval);
        }

        LOGGER.info("Finished at " + getTime());
    }

    public static void main(String[] args) {
        Rotor app = new Rotor(args);
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
