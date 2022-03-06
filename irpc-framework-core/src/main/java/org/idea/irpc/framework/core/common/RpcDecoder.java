package org.idea.irpc.framework.core.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import static org.idea.irpc.framework.core.common.constants.RpcConstants.MAGIC_NUMBER;

/**
 * RPC解码器
 *
 * @Author linhao
 * @Date created in 9:52 上午 2021/12/4
 */
public class RpcDecoder extends ByteToMessageDecoder {

    /**
     * 协议的开头部分的标准长度
     */
    public final int BASE_LENGTH = 2 + 4;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out)  {
        if (byteBuf.readableBytes() >= BASE_LENGTH) {
            byteBuf.readShort();
            int len = byteBuf.readInt();
            byte[] body = new byte[len];
            byteBuf.readBytes(body);
            RpcProtocol rpcProtocol = new RpcProtocol(body);
            out.add(rpcProtocol);
//            if (byteBuf.readableBytes() > 2046) {
//                byteBuf.skipBytes(byteBuf.readableBytes());
//            }
//            int beginReader;
//            while (true) {
//                beginReader = byteBuf.readerIndex();
//                byteBuf.markReaderIndex();
//                if (byteBuf.readShort() == MAGIC_NUMBER) {
//                    break;
//                }
//                byteBuf.resetReaderIndex();
//                byteBuf.readByte();
//
//                if (byteBuf.readInt() < BASE_LENGTH) {
//                    return;
//                }
//            }

//            int length = byteBuf.readInt();
//            if (byteBuf.readableBytes() < length) {
//                byteBuf.readerIndex(beginReader);
//                return;
//            }
//            byte[] data = new byte[length];
//            byteBuf.readBytes(data);

        }
    }
}
