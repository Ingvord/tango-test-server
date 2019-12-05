package hzg.wpn.tango;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.DeviceProxy;
import fr.esrf.TangoApi.DeviceProxyFactory;
import fr.esrf.TangoApi.events.ITangoChangeListener;
import fr.esrf.TangoApi.events.TangoChange;
import fr.esrf.TangoApi.events.TangoChangeEvent;
import fr.soleil.tango.clientapi.TangoCommand;
import org.junit.Ignore;
import org.junit.Test;
import org.tango.client.ez.proxy.TangoProxies;
import org.tango.client.ez.proxy.TangoProxy;
import org.tango.utils.DevFailedUtils;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 03.12.2019
 */
public class TestServerTest {

    @Test
    @Ignore
    public void testSubscription() throws Exception {
        DeviceProxy proxy = DeviceProxyFactory.get("development/test_server/0","hzgxenvtest:10000");

        new TangoChange(proxy, "Status",new String[0]).addTangoChangeListener(new ITangoChangeListener() {
            @Override
            public void change(TangoChangeEvent e) {
                try {
                    System.out.println(e.getValue().extractString());
                } catch (DevFailed devFailed) {
                    DevFailedUtils.printDevFailed(devFailed);
                }
            }
        }, true);


        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    @Ignore
    public void testTimeout() throws Exception{
        TangoCommand cmd = new TangoCommand("development/test_server/0/getTestTimeoutEcho");

        for(long i = 0; i<1_000_000; ++i) {
            String result = String.valueOf(cmd.executeExtract(String.format("A-%d@%d",i,System.currentTimeMillis())));
            System.out.println(String.format("%s@%d",result, System.currentTimeMillis()));
        }
    }

    @Test
    @Ignore
    public void testTimeout_withEz() throws Exception{
        TangoProxy proxy = TangoProxies.newDeviceProxyWrapper("tango://hzgxenvtest:10000/development/test_server/0");

        for(long i = 0; i<1_000_000; ++i) {
            String result = proxy.executeCommand("getTestTimeoutEcho",String.format("B-%d@%d",i,System.currentTimeMillis()));
            System.out.println(String.format("%s@%d",result, System.currentTimeMillis()));
        }
    }
}