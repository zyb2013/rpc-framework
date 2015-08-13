package common.server;

import com.xl.rpc.annotation.RpcControl;
import com.xl.rpc.annotation.RpcMethod;
import com.xl.rpc.annotation.RpcRequest;
import common.client.Command;

/**
 * Created by Administrator on 2015/8/7.
 */
@RpcControl("same")
public interface ISameControl {
    @RpcMethod(Command.SameControl_same)
    void sameRequest(@RpcRequest String param);
}
