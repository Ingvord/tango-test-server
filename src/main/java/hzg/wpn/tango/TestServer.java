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
import java.util.concurrent.Executors;
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
            imageDirectory = Files.createTempDirectory("tmp_");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @DeviceManagement
    private DeviceManager deviceManager;
    @State
    private DeviceState state;

    public static void main(String[] args) {
        ServerManager.getInstance().start(args, TestServer.class);
    }

    public void setDeviceManager(DeviceManager manager) {
        deviceManager = manager;
    }

    @Attribute
    @AttributeProperties(description = "System.currentTimeMillis")
    public long getRegister15() {
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

    @Command
    @StateMachine(endState = DeviceState.ON)
    public void start() {
        setState(DeviceState.RUNNING);
        ByteBuffer buffer = ByteBuffer.allocate(4 * 8 * 2);
        buffer.asShortBuffer().put(new short[]{
                255, 0, 0, 0, 0, 0, 0, 255,
                0, 0, 255, 255, 255, 255, 0, 0,
                0, 0, 255, 255, 255, 255, 0, 0,
                255, 0, 0, 0, 0, 0, 0, 255
        });

        for (int i = 0; i < getNumberOfImages(); ++i) {
            try {
                Files.write(imageDirectory.resolve(getFilePrefix() + "00000" + i + ".tiff"), buffer.array());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

        exec.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    deviceManager.pushEvent("register15", new AttributeValue(System.currentTimeMillis()), EventType.USER_EVENT);
                } catch (DevFailed devFailed) {
                    DevFailedUtils.printDevFailed(devFailed);
                }
            }
        }, (long) Math.random() * 100L, 300L, TimeUnit.NANOSECONDS);
        deviceManager.startPolling("register15", 1);
    }

    @Delete
    public void delete() throws Exception {
        exec.shutdownNow();
        deviceManager.stopPolling("register15");
    }
}
