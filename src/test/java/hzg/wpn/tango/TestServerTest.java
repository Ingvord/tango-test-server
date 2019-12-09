package hzg.wpn.tango;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.CommunicationTimeout;
import fr.esrf.TangoApi.DeviceProxy;
import fr.esrf.TangoApi.DeviceProxyFactory;
import fr.esrf.TangoApi.events.ITangoChangeListener;
import fr.esrf.TangoApi.events.TangoChange;
import fr.esrf.TangoApi.events.TangoChangeEvent;
import fr.soleil.tango.clientapi.TangoAttribute;
import fr.soleil.tango.clientapi.TangoCommand;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.tango.client.ez.proxy.ReadAttributeException;
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
    public void testTimeoutCommand() throws Exception{
        TangoCommand cmd = new TangoCommand("development/test_server/0/getTestTimeoutEcho");

        for(long i = 0; i<1_000_000; ++i) {
            String result = String.valueOf(cmd.executeExtract(String.format("A-%d@%d",i,System.currentTimeMillis())));
            System.out.println(String.format("%s@%d",result, System.currentTimeMillis()));
        }
    }

    @Test
    @Ignore
    public void testTimeoutCommand_withEz() throws Exception{
        TangoProxy proxy = TangoProxies.newDeviceProxyWrapper("tango://hzgxenvtest:10000/development/test_server/0");

        for(long i = 0; i<1_000_000; ++i) {
            String result = proxy.executeCommand("getTestTimeoutEcho",String.format("B-%d@%d",i,System.currentTimeMillis()));
            System.out.println(String.format("%s@%d",result, System.currentTimeMillis()));
        }
    }

    @Before
    public void before() throws Exception{
        TangoCommand cmd = new TangoCommand("development/test_server/0/resetReqId");
        cmd.execute();
    }

    @Test
    @Ignore
    public void testTimeoutAttribute() throws Exception{
        TangoAttribute attr = new TangoAttribute("development/test_server/0/testTimeoutAttribute");

        for(long i = 0; i<1_000_000; ++i) {
            long start = System.currentTimeMillis();

            String result = null;
            try {
                result = attr.read(String.class);
                System.out.println(String.format("%d\t%d\t%s\t%d",i,start,result, System.currentTimeMillis()));
            } catch (CommunicationTimeout timeout) {
                System.out.println(String.format("%d\t%d\ttimeout!!!\t%d",i,start,System.currentTimeMillis()));
                throw timeout;
            }

        }
    }

    @Test
    @Ignore
    public void testTimeoutAttribute_withEz() throws Exception{
        TangoProxy proxy = TangoProxies.newDeviceProxyWrapper("tango://hzgxenvtest:10000/development/test_server/0");

        for(long i = 0; i<1_000_000; ++i) {
            long start = System.currentTimeMillis();
            try {
                String result = proxy.readAttribute("testTimeoutAttribute");
                System.out.println(String.format("%d\t%d\t%s\t%d",i,start,result, System.currentTimeMillis()));
            } catch (ReadAttributeException e) {
                System.out.println(String.format("%d\t%d\ttimeout!!!\t%d",i,start,System.currentTimeMillis()));
                throw e;
            }
        }
    }

}