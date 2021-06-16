# springboot-ldap

The goal of this project is to create a simple [`Spring Boot`](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/) REST API, called `simple-service`, and secure it with `Spring Security LDAP` module.

## Application

- ### simple-service

  `Spring Boot` Java Web application that exposes two endpoints:
   - `GET /api/public`: that can be access by anyone, it is not secured;
   - `GET /api/private`: that can just be accessed by users authenticated with valid LDAP credentials.

## Prerequisites

- [`Java 11+`](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
- [`Docker`](https://www.docker.com/)
- [`Docker-Compose`](https://docs.docker.com/compose/install/)

## Start Environment

- Open a terminal and inside `springboot-ldap` root folder run
  ```
  docker-compose up -d
  ```

- Check their status by running
  ```
  docker-compose ps
  ```

## Import OpenLDAP Users

The `LDIF` file we will use, `springboot-ldap/ldap/ldap-mycompany-com.ldif`, contains a pre-defined structure for `mycompany.com`. Basically, it has 2 groups (`employees` and `clients`) and 3 users (`Bill Gates`, `Steve Jobs` and `Mark Cuban`). Besides, it's defined that `Bill Gates` and `Steve Jobs` belong to `employees` group and `Mark Cuban` belongs to `clients` group.
```
Bill Gates > username: bgates, password: 123
Steve Jobs > username: sjobs, password: 123
Mark Cuban > username: mcuban, password: 123
```

There are two ways to import those users: by running a script; or by using `phpldapadmin`

### Import users running a script

- In a terminal, make use you are in `springboot-ldap` root folder

- Run the following script
  ```
  ./import-openldap-users.sh
  ```
  
- Check users imported using [`ldapsearch`](https://linux.die.net/man/1/ldapsearch)
  ```
  ldapsearch -x -D "cn=admin,dc=mycompany,dc=com" \
    -w admin -H ldap://localhost:389 \
    -b "ou=users,dc=mycompany,dc=com" \
    -s sub "(uid=*)"
  ```

### Import users using phpldapadmin

- Access https://localhost:6443

- Login with the following credentials
  ```
  Login DN: cn=admin,dc=mycompany,dc=com
  Password: admin
  ```

- Import the file `springboot-ldap/ldap/ldap-mycompany-com.ldif`

- You should see something like

  ![phpldapadmin](images/phpldapadmin.png)

## Run application with Maven

- In a terminal, make use you are in `springboot-ldap` root folder

- Run the following command to start `simple-service`
  ```
  ./mvnw clean spring-boot:run --projects simple-service
  ```

## Run application as Docker container

- In a terminal, make sure you are in `springboot-ldap` root folder

- Build Docker Image
  - JVM
    ```
    ./docker-build.sh
    ```
  - Native
    ```
    ./docker-build.sh native
    ```

- Environment Variables

  | Environment Variable | Description                                             |
  | -------------------- | ------------------------------------------------------- |
  | `LDAP_HOST`          | Specify host of the `LDAP` to use (default `localhost`) |
  | `LDAP_PORT`          | Specify port of the `LDAP` to use (default `389`)       |

- Run Docker Container
  > **Warning:** Native is not working yet, see [Issues](#issues)
  ```
  docker run --rm --name simple-service -p 8080:8080 \
    -e LDAP_HOST=openldap \
    --network springboot-ldap_default \
    ivanfranchin/simple-service:1.0.0
  ```

## Testing using curl

1. Open a terminal

1. Call the endpoint `/api/public`
   ```
   curl -i localhost:8080/api/public
   ```

   It should return
   ```
   HTTP/1.1 200
   It is public.
   ```

1. Try to call the endpoint `/api/private` without credentials
   ``` 
   curl -i localhost:8080/api/private
   ```
   
   It should return
   ```
   HTTP/1.1 401
   { "timestamp": "...", "status": 401, "error": "Unauthorized", "message": "Unauthorized", "path": "/api/private" }
   ```

1. Call the endpoint `/api/private` again. This time informing `username` and `password`
   ``` 
   curl -i -u bgates:123 localhost:8080/api/private
   ```
   
   It should return
   ```
   HTTP/1.1 200
   bgates, it is private.
   ```

1. Call the endpoint `/api/private` informing an invalid password
   ``` 
   curl -i -u bgates:124 localhost:8080/api/private
   ```
   
   It should return
   ```
   HTTP/1.1 401 
   ```

1. Call the endpoint `/api/private` informing a non-existing user
   ``` 
   curl -i -u cslim:123 localhost:8080/api/private
   ```
   
   It should return
   ```
   HTTP/1.1 401
   ```

## Testing using Swagger

1. Access http://localhost:8080/swagger-ui.html

   ![swagger](images/simple-service-swagger.png)

1. Click `GET /api/public` to open it; then, click `Try it out` button and, finally, `Execute` button.

   It should return
   ```
   Code: 200
   Response Body: It is public.
   ```

1. Click `Authorize` button (green-white one, located at top-right of the page)

1. In the form that opens, provide the `Bill Gates` credentials, i.e, username `bgates` and password `123`. Then, click `Authorize` button, and to finalize, click `Close` button

1. Click `GET /api/private` to open it; then click `Try it out` button and, finally, `Execute` button.

   It should return
   ```
   Code: 200
   Response Body: bgates, it is private.
   ```

## Shutdown

- To stop `simple-service` application, go to the terminal where it is running and press `Ctrl+C`
- To stop and remove docker-compose containers, network and volumes, in a terminal and inside `springboot-ldap` root folder, run the following command
  ```
  docker-compose down -v
  ```
- To remove Docker images created by this project, run
  ```
  ./remove-docker-images.sh
  ```

## Using Tracing Agent to generate the missing configuration for native image

> **IMPORTANT**: The environment variable `JAVA_HOME` must be set to a `GraalVM` installation directory ([Install GraalVM](https://www.graalvm.org/docs/getting-started/#install-graalvm)), and the `native-image` tool must be installed ([Install Native Image](https://www.graalvm.org/reference-manual/native-image/#install-native-image)).

> **TIP**: For more information `Tracing Agent` see [Spring Native documentation](https://docs.spring.io/spring-native/docs/current/reference/htmlsingle/#tracing-agent)

- Run the following steps in a terminal and inside `springboot-keycloak-openldap` root folder
  ```
  ./mvnw clean package --projects simple-service
  cd simple-service
  java -jar -agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image target/simple-service-1.0.0.jar
  ```
- Once the application is running, exercise it by calling its endpoints using `curl` and `Swagger` so that `Tracing Agent` observes the behavior of the application running on Java HotSpot VM and writes configuration files for reflection, JNI, resource, and proxy usage to automatically configure the native image generator.
- It should generate `JSON` files in `simple-service/src/main/resources/META-INF/native-image` such as: `jni-config.json`, `proxy-config.json`, `reflect-config.json`, `resource-config.json` and `serialization-config.json`.

## Issues

The Docker native image is built and startup successfully. However, I am facing the following exceptions:

- When calling the private endpoint informing valid or invalid credentials for the basic authentication, the application returns `401` and the following exception is logged
  ```
  SEVERE: Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Filter execution threw an exception] with root cause
  com.oracle.svm.core.jdk.UnsupportedFeatureError: JDK11OrLater: Target_java_lang_ClassLoader.trySetObjectField(String name, Object obj)
  	at com.oracle.svm.core.util.VMError.unsupportedFeature(VMError.java:87)
  	at java.lang.ClassLoader.trySetObjectField(ClassLoader.java:330)
  	at java.lang.ClassLoader.createOrGetClassLoaderValueMap(ClassLoader.java:2993)
  	at java.lang.System$2.createOrGetClassLoaderValueMap(System.java:2126)
  	at jdk.internal.loader.AbstractClassLoaderValue.map(AbstractClassLoaderValue.java:266)
  	at jdk.internal.loader.AbstractClassLoaderValue.computeIfAbsent(AbstractClassLoaderValue.java:189)
  	at javax.naming.spi.NamingManager.getInitialContext(NamingManager.java:722)
  	at javax.naming.InitialContext.getDefaultInitCtx(InitialContext.java:305)
  	at javax.naming.InitialContext.init(InitialContext.java:236)
  	at javax.naming.ldap.InitialLdapContext.<init>(InitialLdapContext.java:154)
  	at org.springframework.ldap.core.support.LdapContextSource.getDirContextInstance(LdapContextSource.java:42)
  	at org.springframework.ldap.core.support.AbstractContextSource.createContext(AbstractContextSource.java:350)
  	at org.springframework.ldap.core.support.AbstractContextSource.doGetContext(AbstractContextSource.java:146)
  	at org.springframework.ldap.core.support.AbstractContextSource.getContext(AbstractContextSource.java:137)
  	at org.springframework.security.ldap.authentication.BindAuthenticator.bindWithDn(BindAuthenticator.java:104)
  	at org.springframework.security.ldap.authentication.BindAuthenticator.bindWithDn(BindAuthenticator.java:93)
  	at org.springframework.security.ldap.authentication.BindAuthenticator.authenticate(BindAuthenticator.java:74)
  	at org.springframework.security.ldap.authentication.LdapAuthenticationProvider.doAuthentication(LdapAuthenticationProvider.java:174)
  	at org.springframework.security.ldap.authentication.AbstractLdapAuthenticationProvider.authenticate(AbstractLdapAuthenticationProvider.java:81)
  	at org.springframework.security.authentication.ProviderManager.authenticate(ProviderManager.java:182)
  	at org.springframework.security.authentication.ProviderManager.authenticate(ProviderManager.java:201)
  	at org.springframework.security.web.authentication.www.BasicAuthenticationFilter.doFilterInternal(BasicAuthenticationFilter.java:155)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.authentication.logout.LogoutFilter.doFilter(LogoutFilter.java:103)
  	at org.springframework.security.web.authentication.logout.LogoutFilter.doFilter(LogoutFilter.java:89)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.web.filter.CorsFilter.doFilterInternal(CorsFilter.java:91)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.header.HeaderWriterFilter.doHeadersAfter(HeaderWriterFilter.java:90)
  	at org.springframework.security.web.header.HeaderWriterFilter.doFilterInternal(HeaderWriterFilter.java:75)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.context.SecurityContextPersistenceFilter.doFilter(SecurityContextPersistenceFilter.java:110)
  	at org.springframework.security.web.context.SecurityContextPersistenceFilter.doFilter(SecurityContextPersistenceFilter.java:80)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter.doFilterInternal(WebAsyncManagerIntegrationFilter.java:55)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.FilterChainProxy.doFilterInternal(FilterChainProxy.java:211)
  	at org.springframework.security.web.FilterChainProxy.doFilter(FilterChainProxy.java:183)
  	at org.springframework.web.filter.DelegatingFilterProxy.invokeDelegate(DelegatingFilterProxy.java:358)
  	at org.springframework.web.filter.DelegatingFilterProxy.doFilter(DelegatingFilterProxy.java:271)
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189)
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
  	at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189)
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
  	at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189)
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
  	at org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter.doFilterInternal(WebMvcMetricsFilter.java:96)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189)
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
  	at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189)
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
  	at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:202)
  	at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:97)
  	at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:542)
  	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:143)
  	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92)
  	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:78)
  	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:357)
  	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:374)
  	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65)
  	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:893)
  	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1707)
  	at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49)
  	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
  	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
  	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
  	at java.lang.Thread.run(Thread.java:829)
  	at com.oracle.svm.core.thread.JavaThreads.threadStartRoutine(JavaThreads.java:553)
  	at com.oracle.svm.core.posix.thread.PosixJavaThreads.pthreadStartRoutine(PosixJavaThreads.java:192)
  ```

- When accessing Swagger website
  ```
  SEVERE: Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Handler dispatch failed; nested exception is com.oracle.svm.core.jdk.UnsupportedFeatureError: Proxy class defined by interfaces [interface org.springframework.web.bind.annotation.PathVariable, interface org.springframework.core.annotation.SynthesizedAnnotation] not found. Generating proxy classes at runtime is not supported. Proxy classes need to be defined at image build time by specifying the list of interfaces that they implement. To define proxy classes use -H:DynamicProxyConfigurationFiles=<comma-separated-config-files> and -H:DynamicProxyConfigurationResources=<comma-separated-config-resources> options.] with root cause
  com.oracle.svm.core.jdk.UnsupportedFeatureError: Proxy class defined by interfaces [interface org.springframework.web.bind.annotation.PathVariable, interface org.springframework.core.annotation.SynthesizedAnnotation] not found. Generating proxy classes at runtime is not supported. Proxy classes need to be defined at image build time by specifying the list of interfaces that they implement. To define proxy classes use -H:DynamicProxyConfigurationFiles=<comma-separated-config-files> and -H:DynamicProxyConfigurationResources=<comma-separated-config-resources> options.
  	at com.oracle.svm.core.util.VMError.unsupportedFeature(VMError.java:87)
  	at com.oracle.svm.reflect.proxy.DynamicProxySupport.getProxyClass(DynamicProxySupport.java:113)
  	at java.lang.reflect.Proxy.getProxyConstructor(Proxy.java:66)
  	at java.lang.reflect.Proxy.newProxyInstance(Proxy.java:1006)
  	at org.springframework.core.annotation.SynthesizedMergedAnnotationInvocationHandler.createProxy(SynthesizedMergedAnnotationInvocationHandler.java:271)
  	at org.springframework.core.annotation.TypeMappedAnnotation.createSynthesized(TypeMappedAnnotation.java:335)
  	at org.springframework.core.annotation.AbstractMergedAnnotation.synthesize(AbstractMergedAnnotation.java:210)
  	at org.springframework.core.annotation.AnnotationUtils.synthesizeAnnotation(AnnotationUtils.java:1194)
  	at org.springframework.core.annotation.AnnotationUtils.synthesizeAnnotationArray(AnnotationUtils.java:1280)
  	at org.springframework.core.annotation.SynthesizingMethodParameter.adaptAnnotationArray(SynthesizingMethodParameter.java:104)
  	at org.springframework.core.MethodParameter.getParameterAnnotations(MethodParameter.java:645)
  	at org.springframework.web.method.HandlerMethod$HandlerMethodParameter.getParameterAnnotations(HandlerMethod.java:502)
  	at org.springframework.core.MethodParameter.getParameterAnnotation(MethodParameter.java:668)
  	at org.springframework.core.MethodParameter.hasParameterAnnotation(MethodParameter.java:683)
  	at org.springframework.web.method.annotation.ModelFactory.findSessionAttributeArguments(ModelFactory.java:182)
  	at org.springframework.web.method.annotation.ModelFactory.initModel(ModelFactory.java:114)
  	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:871)
  	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:808)
  	at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87)
  	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1063)
  	at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:963)
  	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006)
  	at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:898)
  	at javax.servlet.http.HttpServlet.service(HttpServlet.java:626)
  	at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883)
  	at javax.servlet.http.HttpServlet.service(HttpServlet.java:733)
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:227)
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
  	at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53)
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189)
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:327)
  	at org.springframework.security.web.access.intercept.FilterSecurityInterceptor.invoke(FilterSecurityInterceptor.java:115)
  	at org.springframework.security.web.access.intercept.FilterSecurityInterceptor.doFilter(FilterSecurityInterceptor.java:81)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.access.ExceptionTranslationFilter.doFilter(ExceptionTranslationFilter.java:121)
  	at org.springframework.security.web.access.ExceptionTranslationFilter.doFilter(ExceptionTranslationFilter.java:115)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.session.SessionManagementFilter.doFilter(SessionManagementFilter.java:126)
  	at org.springframework.security.web.session.SessionManagementFilter.doFilter(SessionManagementFilter.java:81)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.authentication.AnonymousAuthenticationFilter.doFilter(AnonymousAuthenticationFilter.java:105)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter.doFilter(SecurityContextHolderAwareRequestFilter.java:149)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.savedrequest.RequestCacheAwareFilter.doFilter(RequestCacheAwareFilter.java:63)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.authentication.www.BasicAuthenticationFilter.doFilterInternal(BasicAuthenticationFilter.java:149)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.authentication.logout.LogoutFilter.doFilter(LogoutFilter.java:103)
  	at org.springframework.security.web.authentication.logout.LogoutFilter.doFilter(LogoutFilter.java:89)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.web.filter.CorsFilter.doFilterInternal(CorsFilter.java:91)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.header.HeaderWriterFilter.doHeadersAfter(HeaderWriterFilter.java:90)
  	at org.springframework.security.web.header.HeaderWriterFilter.doFilterInternal(HeaderWriterFilter.java:75)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.context.SecurityContextPersistenceFilter.doFilter(SecurityContextPersistenceFilter.java:110)
  	at org.springframework.security.web.context.SecurityContextPersistenceFilter.doFilter(SecurityContextPersistenceFilter.java:80)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter.doFilterInternal(WebAsyncManagerIntegrationFilter.java:55)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336)
  	at org.springframework.security.web.FilterChainProxy.doFilterInternal(FilterChainProxy.java:211)
  	at org.springframework.security.web.FilterChainProxy.doFilter(FilterChainProxy.java:183)
  	at org.springframework.web.filter.DelegatingFilterProxy.invokeDelegate(DelegatingFilterProxy.java:358)
  	at org.springframework.web.filter.DelegatingFilterProxy.doFilter(DelegatingFilterProxy.java:271)
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189)
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
  	at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189)
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
  	at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189)
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
  	at org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter.doFilterInternal(WebMvcMetricsFilter.java:96)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189)
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
  	at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201)
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189)
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
  	at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:202)
  	at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:97)
  	at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:542)
  	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:143)
  	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92)
  	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:78)
  	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:357)
  	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:374)
  	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65)
  	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:893)
  	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1707)
  	at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49)
  	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
  	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
  	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
  	at java.lang.Thread.run(Thread.java:829)
  	at com.oracle.svm.core.thread.JavaThreads.threadStartRoutine(JavaThreads.java:553)
  	at com.oracle.svm.core.posix.thread.PosixJavaThreads.pthreadStartRoutine(PosixJavaThreads.java:192)
  ```