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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.07.2015
 */
@Device
public class TestServer {
    private static final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
    private double aDouble = 100.0D;
    private float aFloat = 50.0F;
    private long aLong = 1000;
    private int anInt = 10;
    //Simulate camera API
    private Path imageDirectory;

    {
        try {
            if (System.getProperty("os.name").equals("linux"))
                imageDirectory = Files.createTempDirectory("tmp_",
                        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
            else {
                imageDirectory = Files.createTempDirectory("tmp_");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @DeviceManagement
    private DeviceManager deviceManager;
    @State
    private DeviceState state;
    private Runnable register13 = new Runnable() {
        @Override
        public void run() {
            try {
                long value = System.currentTimeMillis();
//                    System.out.println("Sending new value = " + value);
                deviceManager.pushEvent("register13", new AttributeValue(value), EventType.CHANGE_EVENT);
            } catch (DevFailed devFailed) {
                DevFailedUtils.printDevFailed(devFailed);
            }
        }
    };
    private FutureTask<Void> register13Task;
    private SensorSizePx sensorSizePx = SensorSizePx._16P;
    private long delay = 50L;

    public static void main(String[] args) {
        ServerManager.getInstance().start(args, TestServer.class);
    }

    public void setDeviceManager(DeviceManager manager) {
        deviceManager = manager;
    }

    @Attribute(pushChangeEvent = true, checkChangeEvent = false)
    @AttributeProperties(description = "System.currentTimeMillis")
    public long getRegister13() {
        return System.currentTimeMillis();
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

    @Command
    @StateMachine(endState = DeviceState.ON)
    public void start() {
        setState(DeviceState.RUNNING);
        ByteBuffer buffer = getByteBuffer();

        for (int i = 0; i < getNumberOfImages(); ++i) {
            try {
                Files.write(imageDirectory.resolve(getFilePrefix() + "00000" + i + ".tiff"), buffer.array());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ByteBuffer getByteBuffer() {
        Random rnd = new Random();

        int size = sensorSizePx.value * 2;// each pixel = 2 Bytes

        ByteBuffer buffer = ByteBuffer.allocate(size);

        for (int i = 0; i < size; ++i) {
            buffer.asShortBuffer().put((short) rnd.nextInt());
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
    }

    @Attribute
    public void setTikTakDelay(long delay) {
        this.delay = delay;
    }

    @Command
    public void simulate_tik_tak() {
        register13Task = (FutureTask<Void>) exec.scheduleWithFixedDelay(register13, 0L, delay, TimeUnit.MILLISECONDS);
    }

    @Command
    public void stop_tik_tak() {
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
