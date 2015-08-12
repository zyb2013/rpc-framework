package com.xl.rpc.codec;

import com.xl.rpc.dispatch.SocketPacket;

import java.util.Arrays;

/**
 * @author Caedmon
 * 输出对象
 * */
public class RpcPacket extends SocketPacket {
    private boolean fromCall;
    private boolean sync;
    private String uuid;
    private boolean exception;
    private String[] classNameArray;
    public RpcPacket(String cmd,Object...content){
        super(cmd,content);
    }

    public boolean getSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isException() {
        return exception;
    }

    public void setException(boolean exception) {
        this.exception = exception;
    }

    public String[] getClassNameArray() {
        return classNameArray;
    }

    public void setClassNameArray(String[] classNameArray) {
        this.classNameArray = classNameArray;
    }

    public boolean isFromCall() {
        return fromCall;
    }

    public void setFromCall(boolean fromCall) {
        this.fromCall = fromCall;
    }

    public String getClassNames(){
        return Arrays.toString(classNameArray);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RpcPacket{");
        sb.append("fromCall=").append(fromCall);
        sb.append(",cmd=").append(super.cmd);
        sb.append(", sync=").append(sync);
        sb.append(", uuid='").append(uuid).append('\'');
        sb.append(", exception=").append(exception);
        sb.append(", classNameArray=").append(Arrays.toString(classNameArray));
        sb.append('}');
        return sb.toString();
    }
}

