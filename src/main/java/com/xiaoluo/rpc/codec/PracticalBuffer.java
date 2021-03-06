package com.xiaoluo.rpc.codec;

import com.google.protobuf.Message;
import io.netty.buffer.ByteBuf;

/**
 * @author Chenlong
 * 缓冲区包装装饰器类，提供在Bytebuf之上更常用的接口
 * */
public interface PracticalBuffer {
	byte readByte();
	boolean readBoolean();
	short readShort();
	int readInt();
	float readFloat();
	long readLong();
	double readDouble();
	String readString();
	void readBytes(byte[] dst);
	Message.Builder readProtoBuf(Message.Builder builder);
	<T> T readJSON(Class<T> clazz);
	PracticalBuffer readBinary(int length);
	void writeByte(byte b);
	void writeBoolean(boolean b);
	void writeShort(short s);
	void writeInt(int i);
	void writeFloat(float f);
	void writeLong(long l);
	void writeDouble(double d);
	void writeString(String s);
	void writeProtoBuf(Message.Builder builder);
	void writeJSON(Object bean);
	void writeBytes(PracticalBuffer buffer);
	ByteBuf getByteBuf();
	void writeBytes(byte[] bytes);

}
