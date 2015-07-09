package hzg.wpn.tango;

import org.tango.DeviceState;
import org.tango.server.ServerManager;
import org.tango.server.annotation.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 09.07.2015
 */
@Device
public class TestServer {
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

    @State
    private DeviceState state;

    public static void main(String[] args) {
        ServerManager.getInstance().start(args, TestServer.class);
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
                Files.write(imageDirectory.resolve(getFilePrefix() + "000" + i + ".tiff"), buffer.array());
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
    public void init() {
        aDouble = 100.0D;
        aFloat = 50.0F;
        aLong = 1000;
        anInt = 10;
    }
}
