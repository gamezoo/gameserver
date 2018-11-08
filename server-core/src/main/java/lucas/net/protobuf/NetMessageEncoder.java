package lucas.net.protobuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lucas.net.message.NetMessageBody;
import lucas.net.message.NetMessageHead;

import java.util.List;

/**
 * @author lushengkao vip8
 * 2018/11/7 16:31
 */
public class NetMessageEncoder extends MessageToMessageEncoder<ProtoBufNetMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ProtoBufNetMessage msg, List<Object> out) {
        ByteBuf byteBuf = Unpooled.buffer(256);
        //编写head
        NetMessageHead netMessageHead = msg.getHead();
        byteBuf.writeShort(netMessageHead.getHead());
        //长度
        byteBuf.writeInt(0);
        byteBuf.writeByte(netMessageHead.getVersion());
        byteBuf.writeShort(netMessageHead.getCommand());
        byteBuf.writeInt(netMessageHead.getSerial());
        //编写body
        NetMessageBody netMessageBody = msg.getBody();
        byteBuf.writeBytes(netMessageBody.getBytes());
        //重新设置长度
        int skip = 6;
        int length = byteBuf.readableBytes() - skip;
        byteBuf.setInt(2, length);
        out.add(byteBuf);
    }
}
