package com.xl.boot;

import com.xl.dispatch.handler.ValidateOKHandler;
import com.xl.dispatch.method.RpcMethodDispatcher;
import com.xl.dispatch.method.JavassitRpcMethodDispatcher;
import com.xl.dispatch.method.PrototypeBeanAccess;
import com.xl.dispatch.tcp.TCPServerInitializer;
import com.xl.session.SessionFire;
import com.xl.utils.NGSocketParams;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2015/4/25.
 */
public class ServerSocketEngine extends SocketEngine{
    private static final Logger log= LoggerFactory.getLogger(ServerSocketEngine.class);
    private ServerSettings settings;
    public ServerSocketEngine(ServerSettings settings,RpcMethodDispatcher rpcMethodDispatcher) {
        super(settings, rpcMethodDispatcher);
        this.settings=settings;
    }
    public ServerSocketEngine(ServerSettings settings){
        this(settings,new JavassitRpcMethodDispatcher(new PrototypeBeanAccess(),Runtime.getRuntime().availableProcessors()));
    }
    /**
     * 启动网络服务
     * */
    public void startSocket(){
        log.info("ServerSocketEngine Init!");
        final EventLoopGroup bossGroup = new NioEventLoopGroup(settings.bossThreadSize);
        final EventLoopGroup workerGroup = new NioEventLoopGroup(settings.workerThreadSize);
        try {
            ChannelInitializer<SocketChannel> initializer=null;
            switch (settings.protocol.toLowerCase()){
                case TCP_PROTOCOL:
                    initializer=new TCPServerInitializer(rpcMethodDispatcher);
                    break;
//                case WEBSOCKET_PROTOCOL:
//                    initializer=new WsServerInitalizer(rpcMethodDispatcher);
//                    break;
                default:
                    throw new IllegalArgumentException("Unsupport protocol:protocol = "+settings.protocol);
            }

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(initializer);
            ChannelFuture f =  b.bind(settings.port).sync();
            log.info("Protocol type: {}",settings.protocol);
            log.info("Boss thread : {}",settings.bossThreadSize);
            log.info("Worker thread : {}",settings.workerThreadSize);
            log.info("Logic thread:{}",settings.cmdThreadSize);
            log.info("Socket package encrypt : {}", NGSocketParams.isSocketPacketEncrypt());
            log.info("Cmd Dispatcher : {}", rpcMethodDispatcher.getClass().getCanonicalName());
            log.info("Socket port :{}",settings.port);

            //f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            if(log.isErrorEnabled()){
                log.error("<<<<<<<ServerSocketEngine Start Error!>>>>>>", e);
            }
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            return;
        }
        //如果系统配置不加密则不发送密码表
        if(NGSocketParams.isSocketPacketEncrypt()){
            //用来给客户端发送密码表
            SessionFire.getInstance().registerEvent(SessionFire.SessionEvent.SESSION_LOGIN, new ValidateOKHandler());
        }
        log.info("ServerSocketEngine Start OK!");
    }
}
