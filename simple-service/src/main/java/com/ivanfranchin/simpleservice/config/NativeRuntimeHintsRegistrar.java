package com.ivanfranchin.simpleservice.config;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSourceAware;
import org.springframework.core.DecoratingProxy;
import org.springframework.security.authentication.AuthenticationManager;

public class NativeRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.proxies().registerJdkProxy(
                AuthenticationManager.class,
                MessageSourceAware.class,
                InitializingBean.class,
                SpringProxy.class,
                Advised.class,
                DecoratingProxy.class);
        hints.reflection()
                .registerType(
                        TypeReference.of("org.springdoc.core.providers.SpringWebProvider$$SpringCGLIB$$0"),
                        hint -> hint
                                .withField("CGLIB$FACTORY_DATA")
                                .withField("CGLIB$CALLBACK_FILTER")
                );
    }
}