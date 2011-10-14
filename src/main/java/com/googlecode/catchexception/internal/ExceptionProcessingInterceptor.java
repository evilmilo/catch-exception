/**
 * Copyright (C) 2011 rwoo@gmx.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.catchexception.internal;

import java.lang.reflect.Method;

import org.mockito.cglib.proxy.MethodInterceptor;
import org.mockito.cglib.proxy.MethodProxy;

/**
 * This {@link AbstractExceptionProcessingInvocationHandler} implements
 * {@link MethodInterceptor} for Mockito's cglib variant.
 * 
 * @author rwoo
 * @param <E>
 *            The type of the exception that shall be caught and (optionally)
 *            verified
 */
public class ExceptionProcessingInterceptor<E extends Exception>
        extends AbstractExceptionProcessingInvocationHandler<E> implements
        MethodInterceptor {

    @SuppressWarnings("javadoc")
    public ExceptionProcessingInterceptor(Object target,
            Class<E> clazz, boolean assertException) {
        super(target, clazz, assertException);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.mockito.cglib.proxy.MethodInterceptor#intercept(java.lang.Object,
     * java.lang.reflect.Method, java.lang.Object[],
     * org.mockito.cglib.proxy.MethodProxy)
     */
    public Object intercept(Object obj, Method method, Object[] args,
            MethodProxy proxy) throws Throwable {

        beforeInvocation();

        try {

            Object retval = proxy.invoke(target, args);

            return afterInvocation(retval);

        } catch (Exception e) {

            return afterInvocationThrowsException(e, method);
        }
    }
}