package hzg.wpn.tango;

import fr.esrf.Tango.DevFailed;
import org.tango.DeviceState;
import org.tango.server.ServerManager;
import org.tango.server.annotation.*;
import org.tango.server.attribute.AttributeValue;
import org.tango.server.device.DeviceManager;
import org.tango.server.events.EventType;
import org.tango.utils.DevFailedUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.07.2015
 */
@Device
public class TestServer {
    private ScheduledExecutorService exec;
    private double aDouble = 100.0D;
    private float aFloat = 50.0F;
    private long aLong = 1000;
    private int anInt = 10;
    //Simulate camera API
    private Path imageDirectory = Paths.get("/tmp");

    private volatile int value = 0;

    @DeviceManagement
    private DeviceManager deviceManager;
    @State
    private DeviceState state;
    private FutureTask<Void> register13Task;
    private SensorSizePx sensorSizePx = SensorSizePx._16P;
    private long delay = 50L;
    private Runnable register13 = new Runnable() {
        @Override
        public void run() {
            long iter = 0;
            while (true)
            try {
                if (TestServer.this.state == DeviceState.FAULT)
                    throw new RuntimeException("TestServer is in FAULT state!");

                System.err.println("Iteration #" + iter++);
                System.err.println("System timestamp = " + System.currentTimeMillis());
                Thread.sleep(delay);
                //taking image
                System.err.println("Sending event: start image");
                deviceManager.pushEvent("register13", new AttributeValue(1), EventType.CHANGE_EVENT);
                value = 1;

                long startWrite = System.currentTimeMillis();
                TestServer.this.write_image();//create image
                long stopWrite = System.currentTimeMillis();

                Thread.sleep(Math.max(delay, delay - (stopWrite - startWrite)));

                //done taking image
                System.err.println("Sending event: stop image");
                deviceManager.pushEvent("register13", new AttributeValue(0), EventType.CHANGE_EVENT);
                value = 0;
            } catch (DevFailed devFailed) {
                DevFailedUtils.printDevFailed(devFailed);
                TestServer.this.state = DeviceState.FAULT;
            } catch (IOException ioe) {
                ioe.printStackTrace();
                TestServer.this.state = DeviceState.FAULT;
            } catch (InterruptedException e) {
                System.err.println("Interrupting...");
                Thread.currentThread().interrupt();
                break;
            }
        }
    };

    public static void main(String[] args) {
        ServerManager.getInstance().start(args, TestServer.class);
    }

    public void setDeviceManager(DeviceManager manager) {
        deviceManager = manager;
    }

    @Attribute(pushChangeEvent = true, checkChangeEvent = false)
    public long getRegister13() {
        return value;
    }

    @Attribute
    public double getDouble() {
        return aDouble++;
    }

    @Attribute
    public float getFloat() {
        return aFloat++;
    }

    @Attribute
    public long getLong() {
        return aLong++;
    }

    @Attribute
    public int getInt() {
        return anInt++;
    }

    @Attribute
    public String getString() {
        return "A String";
    }

    @Attribute
    public String getDirectory() {
        return imageDirectory.toAbsolutePath().toString();
    }

    @Attribute
    public String getFilePrefix() {
        return "img_";
    }

    @Attribute
    public int getNumberOfImages() {
        return 10;
    }

    @Attribute(maxDimX = 8, maxDimY = 4)
    public short[][] getLiveImage() {
        return new short[][]{
                new short[]{255, 0, 0, 0, 0, 0, 0, 255},
                new short[]{0, 0, 255, 255, 255, 255, 0, 0},
                new short[]{0, 0, 255, 255, 255, 255, 0, 0},
                new short[]{255, 0, 0, 0, 0, 0, 0, 255}
        };
    }

    @Attribute
    public void setSensorSizePx(String size) {
        this.sensorSizePx = SensorSizePx.valueOf(size.toUpperCase());
    }

    @Attribute
    public String getSensorSizePx(String size) {
        return this.sensorSizePx.name();
    }

    @Command
    @StateMachine(endState = DeviceState.ON)
    public void write_image() throws IOException {
        setState(DeviceState.RUNNING);
        ByteBuffer buffer = getByteBuffer();

        Files.write(Paths.get("/home/p07user/test00000.tif"), buffer.array(), StandardOpenOption.CREATE);
    }

    private ByteBuffer getByteBuffer() {
        Random rnd = new Random();

        int size = sensorSizePx.value;// each pixel = 2 Bytes

        ByteBuffer buffer = ByteBuffer.allocate(size * 2);

        ShortBuffer shortBuffer = buffer.asShortBuffer();
        for (int i = 0; i < size; ++i) {
            shortBuffer.put((short) rnd.nextInt());
        }

        return buffer;
    }

    public DeviceState getState() {
        return state;
    }

    public void setState(DeviceState newState) {
        state = newState;
    }

    @Init
    @StateMachine(endState = DeviceState.ON)
    public void init() throws Exception {
        aDouble = 100.0D;
        aFloat = 50.0F;
        aLong = 1000;
        anInt = 10;

        exec = Executors.newScheduledThreadPool(1);
    }

    @Attribute
    public void setTikTakDelay(long delay) {
        this.delay = delay;
    }

    @Command
    @StateMachine(endState = DeviceState.RUNNING)
    public void start() {
        System.err.println("Starting...");
        register13Task = (FutureTask<Void>) exec.submit(register13);
    }

    @Command
    @StateMachine(endState = DeviceState.ON)
    public void stop() {
        System.err.println("Stopping...");
        register13Task.cancel(true);
    }

    @Delete
    public void delete() throws Exception {
        exec.shutdownNow();
    }

    public static enum SensorSizePx {
        _20MP(20000000),
        _4MP(4000000),
        _16P(16);

        public final int value;

        SensorSizePx(int value) {
            this.value = value;
        }
    }
}
