package com.xiaoluo.rpc.dispatch.method;

import com.xiaoluo.rpc.annotation.RpcService;
import com.xiaoluo.rpc.annotation.RpcMethod;
import com.xiaoluo.rpc.codec.RpcPacket;
import com.xiaoluo.rpc.exception.MethodInterceptException;
import com.xiaoluo.rpc.internal.PrototypeBeanAccess;
import com.xiaoluo.rpc.registry.RegistryNode;
import com.xiaoluo.rpc.registry.server.RegistryManager;
import com.xiaoluo.rpc.dispatch.ISession;
import com.xiaoluo.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by Caedmon on 2015/8/22.
 */
public class ReflectRpcMethodDispatcher extends RpcMethodDispatcher {
    private Map<String,MethodInvoker> methodInvokerMap =new HashMap<>();
    private static final Logger log= LoggerFactory.getLogger(ReflectRpcMethodDispatcher.class);
    public ReflectRpcMethodDispatcher(int threadSize) {
        this(new PrototypeBeanAccess(), threadSize);
    }
    public ReflectRpcMethodDispatcher(BeanAccess beanAccess, int threadSize) {
        super(beanAccess, threadSize);
    }

    public void loadClasses(Class... classes) throws Exception {
        for(Class controlClass:classes){
            loadClass(controlClass);
        }
    }
    private void loadClass(Class controlClass){
        Class controlInterface=null;
        for(Class c:controlClass.getInterfaces()){
            if(ClassUtils.hasAnnotation(c, RpcService.class)){
                controlInterface=c;
                break;
            }
        }
        List<Method> rpcMethods= ClassUtils.findMethodsByAnnotation(controlInterface, RpcMethod.class);
        for(Method method:rpcMethods){
            RpcMethod ma=method.getAnnotation(RpcMethod.class);
            String cmd=ma.value();
            Class[] paramTypes=method.getParameterTypes();
            List<String> classNames=new ArrayList<>(paramTypes.length);
            int i=0;
            for(Class paramType:paramTypes){
                if(ISession.class.isAssignableFrom(paramType)){
                    continue;
                }
                classNames.add(paramType.getName());
            }
            MethodInvoker invoker=new MethodInvoker(beanAccess,method,controlClass);
            if(methodInvokerMap.containsKey(cmd)){
                //log.warn("Rpc method has exists:cmd={}",cmd);
                return;
            }
            methodInvokerMap.put(cmd, invoker);
            log.info("{} Rpc register method : {}=>{}.{}()",this,cmd,controlClass.getName(),method.getName());
        }
    }
    public void processClientRequest(ISession session, RpcPacket packet){
        String cmd=packet.getCmd();
        MethodInvoker invoker= methodInvokerMap.get(cmd);
        if(invoker==null){
            throw new IllegalArgumentException(this.toString()+" Rpc no method exists:method = "+cmd+"-"+packet.getClassNames());
        }
        Object[] requestParams=packet.getParams();
        Method method=invoker.getMethod();
        Class[] methodParamTypes=method.getParameterTypes();
        Object[] invokeParams=new Object[methodParamTypes.length];
        int requestParamPos=0;
        Object response=null;
        Class responseType=method.getReturnType();
        for(int invokeParamPos=0;invokeParamPos<methodParamTypes.length;invokeParamPos++){
            if(ISession.class.isAssignableFrom(methodParamTypes[invokeParamPos])){
                invokeParams[invokeParamPos]=session;
                continue;
            }else{
                invokeParams[invokeParamPos]=requestParams[requestParamPos];
                requestParamPos++;
            }

        }
        try{
            Object reason=before(session,packet);
            if (reason==null) {
                try{
                    for(RegistryNode node: RegistryManager.getInstance().getAllNodeList()){
                        if(node.getSession()==session){
                            log.debug("Rpc process client request:source={},cmd = {}", node.getKey(), cmd);
                        }
                    }
                }catch (Throwable e){
                    log.error("Get rpc command error cmd={}",cmd,e);
                }
                response=invoker.invoke(invokeParams);
                after(session,packet);
            }else{
                throw new MethodInterceptException(reason.getClass().getName());
            }

        }catch (Throwable e){
            log.error("Rpc process client request error:cmd = {}", cmd, e);
            notifyError(session, packet, e);
            if(e instanceof InvocationTargetException&&e.getCause()!=null){
                response=e.getCause();
            }else{
                response=e;
            }

        }
        //log.info("Rpc process client request success:cmd={}",cmd);
        packet.setParams(new Object[]{response});
        packet.setClassNameArray(new String[]{responseType.getName()});
        responseToClient(session, packet);
    }

    public Map<String, MethodInvoker> getMethodInvokerMap() {
        return methodInvokerMap;
    }
}
