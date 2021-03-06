package log;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.net.SimpleSocketServer;
import com.xiaoluo.rpc.log.RpcLogConverter;

import java.net.URL;

/**
 * Created by Administrator on 2015/8/26.
 */
public class SocketLogServer {
    public static void main(String[] args) throws Exception{
        PatternLayout.defaultConverterMap.put("serviceName",RpcLogConverter.ServiceNameConvert.class.getName());
        PatternLayout.defaultConverterMap.put("address", RpcLogConverter.AddressConvert.class.getName());
        URL url=Thread.currentThread().getContextClassLoader().getResource("logback-server.xml");
        SimpleSocketServer.main(new String[]{"8082", url.getFile()});
    }
}
