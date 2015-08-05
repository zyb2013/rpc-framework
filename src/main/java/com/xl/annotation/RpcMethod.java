package com.xl.annotation;

import java.lang.annotation.*;

/**
 * Created by Administrator on 2015/7/10.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface RpcMethod {
    String value();
}