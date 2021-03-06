package com.xiaoluo.rpc.dispatch.tcp;

import com.xiaoluo.rpc.codec.BinaryPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * Created by Caedmon on 2015/4/14.
 * 将ByteBuf封装为BinaryPacket
 */
public class TCPHeadDecoder extends ReplayingDecoder<Void> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int length=in.readInt();
        BinaryPacket packet=new BinaryPacket(in.readBytes(length));
        out.add(packet);
    }
}
