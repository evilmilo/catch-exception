package com.googlecode.catchexception.internal.cglib;

import org.mockito.Mockito;
import org.mockito.cglib.proxy.MethodInterceptor;
import org.mockito.cglib.proxy.MethodProxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class MockitoUtil {

    private static final String[] CGLIBHackerClassNames = {
            "org.mockito.internal.creation.cglib.CGLIBHacker",
            "org.mockito.internal.creation.cglib.CGLIBHacker"
    };

    private static final String[] delegatingMockitoMethodProxyClassNames = {
            "org.mockito.internal.creation.cglib.DelegatingMockitoMethodProxy",
            "org.mockito.internal.creation.DelegatingMockitoMethodProxy"
    };

    private static final String[] CGLIBHackerClassArgumentClassNames = {
            "org.mockito.cglib.proxy.MethodProxy",
            "org.mockito.internal.creation.MockitoMethodProxy"
    };

    private static final String[] classImposterizerClassNames = {
            "org.mockito.internal.creation.cglib.ClassImposterizer",
            "org.mockito.internal.creation.jmock.ClassImposterizer"
    };

    private static Constructor<?> delegatingMockitoMethodProxyConstructor;
    private static Method cglibMethod;
    private static Object cglibInstance;

    private static Method classImposterizerMethod;
    private static Object classImposterizerInstance;

    private static boolean wrapMethodProxy = true;

    static {

        ClassNotFoundException lastClassNotFoundException = null;
        int foundIndex = -1;

        for (int i = 0; i < CGLIBHackerClassNames.length; i++) {
            try {
                Class.forName(classImposterizerClassNames[i]);
                foundIndex = i;
                break;
            } catch (ClassNotFoundException e) {
                //try next
                lastClassNotFoundException = e;
            }
        }

        if (foundIndex < 0) {
            throw new IllegalStateException("Could not find required classes!", lastClassNotFoundException);
        }
        try {

            final Class<?> cgLibHackerClazz = Class.forName(CGLIBHackerClassNames[foundIndex]);
            final Class<?> delegatingMockitoMethodClazz = Class.forName(delegatingMockitoMethodProxyClassNames[foundIndex]);
            final Class<?> classImposterizerClazz = Class.forName(classImposterizerClassNames[foundIndex]);
            final Class<?> cgLibHackerArgumentClazz = Class.forName(CGLIBHackerClassArgumentClassNames[foundIndex]);

            if (cgLibHackerArgumentClazz.equals(MethodProxy.class)) {
                wrapMethodProxy = false;
            }


            final Method method = cgLibHackerClazz.getDeclaredMethod("setMockitoNamingPolicy", cgLibHackerArgumentClazz);
            method.setAccessible(true);

            final Constructor<?> constructor = delegatingMockitoMethodClazz.getDeclaredConstructor(MethodProxy.class);
            constructor.setAccessible(true);

            final Method method2 = classImposterizerClazz.getDeclaredMethod("imposterise", MethodInterceptor.class, Class.class, Class[].class);
            method2.setAccessible(true);

            classImposterizerInstance = getClassImposterizerInstance(classImposterizerClazz);
            classImposterizerMethod = method2;

            final Constructor<?> cgLibHackerConstructor = cgLibHackerClazz.getDeclaredConstructor();
            cgLibHackerConstructor.setAccessible(true);
            cglibInstance = cgLibHackerConstructor.newInstance();
            cglibMethod = method;

            delegatingMockitoMethodProxyConstructor = constructor;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        } catch  (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object getClassImposterizerInstance(Class classImposterizerClazz) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        try {
            final Field field = classImposterizerClazz.getDeclaredField("INSTANCE");
            field.setAccessible(true);
            final Object instance = field.get(null);
            return instance;
        } catch (NoSuchFieldException e) {
            //try other mechanism
        }

        final Class<?> instantiatorClazz = Class.forName("org.mockito.internal.creation.instance.Instantiator");
        final Class<?> instantiatorProviderClazz = Class.forName("org.mockito.internal.creation.instance.InstantiatorProvider");

        final Constructor<?> instantiatorProviderConstructor = instantiatorProviderClazz.getDeclaredConstructor();
        instantiatorProviderConstructor.setAccessible(true);
        final Object instantiatorProvider = instantiatorProviderConstructor.newInstance();

        Object instantiator = null;
        try {
            final Method getInstantiatorMethod = instantiatorProvider.getClass().getMethod("getInstantiator");
            instantiator = getInstantiatorMethod.invoke(instantiatorProvider);
        } catch (NoSuchMethodException e) {
            //try other mechanism
            Class mockCreationSettingsClazz = Class.forName("org.mockito.mock.MockCreationSettings");
            final Method getInstantiatorMethod = instantiatorProvider.getClass().getMethod("getInstantiator", mockCreationSettingsClazz);
            instantiator = getInstantiatorMethod.invoke(instantiatorProvider, mockCreationSettingsClazz.cast(Mockito.withSettings()));
        }

        final Constructor constructor = classImposterizerClazz.getDeclaredConstructor(instantiatorClazz);
        constructor.setAccessible(true);
        final Object instance = constructor.newInstance(instantiator);
        return instance;
    }

    public static void doMockitoBit(MethodProxy proxy) {
        try {
            Object argument = proxy;
            if (wrapMethodProxy) {
                argument = delegatingMockitoMethodProxyConstructor.newInstance(proxy);
            }
            cglibMethod.invoke(cglibInstance, argument);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException(e);
        }
    }

    public static Object doJMockitoBit(Class<?> targetClass, MethodInterceptor interceptor) {
        try {
            return classImposterizerMethod.invoke(classImposterizerInstance, interceptor, targetClass, new Class[0]);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException(e);
        }
    }
}
