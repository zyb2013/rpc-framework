package com.xl.rpc.internal;

import com.xl.rpc.dispatch.method.BeanAccess;
import org.springframework.context.ApplicationContext;

/**
 * Created by Administrator on 2015/7/13.
 */
public class SpringBeanAccess implements BeanAccess {
    @Override
    public <T> T getBean(Class<T> clazz) throws IllegalAccessException, InstantiationException {
        ApplicationContext context=InternalContainer.getInstance().getSpringContext();
        if(context==null){
            throw new NullPointerException("SpringContext has not init");
        }
        return context.getBean(clazz);
    }
}