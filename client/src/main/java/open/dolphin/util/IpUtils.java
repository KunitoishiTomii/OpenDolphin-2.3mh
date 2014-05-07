package open.dolphin.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * InetAddress.getLocalHostが動かない場合
 * 
 * @author masuda, Masuda Naika
 */
public class IpUtils {

    public static InetAddress getLocalhost() throws UnknownHostException {

        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
        }

        try {
            Enumeration<NetworkInterface> enumIfs = NetworkInterface.getNetworkInterfaces();
            while (enumIfs.hasMoreElements()) {
                NetworkInterface nif = enumIfs.nextElement();
                Enumeration<InetAddress> enumAddr = nif.getInetAddresses();
                while (enumAddr.hasMoreElements()) {
                    InetAddress addr = enumAddr.nextElement();
                    if (addr instanceof Inet4Address) {
                        if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                            return addr;
                        }
                    }
                }
            }

        } catch (SocketException ex) {
        }

        throw new UnknownHostException();
    }
}
