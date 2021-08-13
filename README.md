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

## Cleanup

To remove the Docker image created by this project, go to a terminal and run the following command
```
docker rmi ivanfranchin/simple-service:1.0.0
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
  ERROR 1 --- [nio-8080-exec-5] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception   [Filter execution threw an exception] with root cause
  
  com.oracle.svm.core.jdk.UnsupportedFeatureError: JDK11OrLater: Target_java_lang_ClassLoader.trySetObjectField(String name, Object obj)
  	at com.oracle.svm.core.util.VMError.unsupportedFeature(VMError.java:88) ~[na:na]
  	at java.lang.ClassLoader.trySetObjectField(ClassLoader.java:253) ~[na:na]
  	at java.lang.ClassLoader.createOrGetClassLoaderValueMap(ClassLoader.java:2993) ~[na:na]
  	at java.lang.System$2.createOrGetClassLoaderValueMap(System.java:2126) ~[na:na]
  	at jdk.internal.loader.AbstractClassLoaderValue.map(AbstractClassLoaderValue.java:266) ~[na:na]
  	at jdk.internal.loader.AbstractClassLoaderValue.computeIfAbsent(AbstractClassLoaderValue.java:189) ~[na:na]
  	at javax.naming.spi.NamingManager.getInitialContext(NamingManager.java:722) ~[na:na]
  	at javax.naming.InitialContext.getDefaultInitCtx(InitialContext.java:305) ~[na:na]
  	at javax.naming.InitialContext.init(InitialContext.java:236) ~[na:na]
  	at javax.naming.ldap.InitialLdapContext.<init>(InitialLdapContext.java:154) ~[na:na]
  	at org.springframework.ldap.core.support.LdapContextSource.getDirContextInstance(LdapContextSource.java:42) ~[com.mycompany.simpleservice.  SimpleServiceApplication:2.3.4.RELEASE]
  	at org.springframework.ldap.core.support.AbstractContextSource.createContext(AbstractContextSource.java:350) ~[na:na]
  	at org.springframework.ldap.core.support.AbstractContextSource.doGetContext(AbstractContextSource.java:146) ~[na:na]
  	at org.springframework.ldap.core.support.AbstractContextSource.getContext(AbstractContextSource.java:137) ~[na:na]
  	at org.springframework.security.ldap.authentication.BindAuthenticator.bindWithDn(BindAuthenticator.java:104) ~[na:na]
  	at org.springframework.security.ldap.authentication.BindAuthenticator.bindWithDn(BindAuthenticator.java:93) ~[na:na]
  	at org.springframework.security.ldap.authentication.BindAuthenticator.authenticate(BindAuthenticator.java:74) ~[na:na]
  	at org.springframework.security.ldap.authentication.LdapAuthenticationProvider.doAuthentication(LdapAuthenticationProvider.java:174) ~[na:na]
  	at org.springframework.security.ldap.authentication.AbstractLdapAuthenticationProvider.authenticate(AbstractLdapAuthenticationProvider.java:81) ~[na:na]
  	at org.springframework.security.authentication.ProviderManager.authenticate(ProviderManager.java:182) ~[na:na]
  	at org.springframework.security.authentication.ProviderManager.authenticate(ProviderManager.java:201) ~[na:na]
  	at org.springframework.security.web.authentication.www.BasicAuthenticationFilter.doFilterInternal(BasicAuthenticationFilter.java:155) ~[na:na]
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[na:na]
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336) ~[na:na]
  	at org.springframework.security.web.authentication.logout.LogoutFilter.doFilter(LogoutFilter.java:103) ~[na:na]
  	at org.springframework.security.web.authentication.logout.LogoutFilter.doFilter(LogoutFilter.java:89) ~[na:na]
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336) ~[na:na]
  	at org.springframework.web.filter.CorsFilter.doFilterInternal(CorsFilter.java:91) ~[na:na]
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[na:na]
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336) ~[na:na]
  	at org.springframework.security.web.header.HeaderWriterFilter.doHeadersAfter(HeaderWriterFilter.java:90) ~[na:na]
  	at org.springframework.security.web.header.HeaderWriterFilter.doFilterInternal(HeaderWriterFilter.java:75) ~[na:na]
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[na:na]
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336) ~[na:na]
  	at org.springframework.security.web.context.SecurityContextPersistenceFilter.doFilter(SecurityContextPersistenceFilter.java:110) ~[na:na]
  	at org.springframework.security.web.context.SecurityContextPersistenceFilter.doFilter(SecurityContextPersistenceFilter.java:80) ~[na:na]
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336) ~[na:na]
  	at org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter.doFilterInternal(WebAsyncManagerIntegrationFilter.java:55) ~[na:na]
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[na:na]
  	at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(FilterChainProxy.java:336) ~[na:na]
  	at org.springframework.security.web.FilterChainProxy.doFilterInternal(FilterChainProxy.java:211) ~[na:na]
  	at org.springframework.security.web.FilterChainProxy.doFilter(FilterChainProxy.java:183) ~[na:na]
  	at org.springframework.web.filter.DelegatingFilterProxy.invokeDelegate(DelegatingFilterProxy.java:358) ~[na:na]
  	at org.springframework.web.filter.DelegatingFilterProxy.doFilter(DelegatingFilterProxy.java:271) ~[na:na]
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:190) ~[na:na]
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:163) ~[na:na]
  	at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.  3.9]
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[na:na]
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:190) ~[na:na]
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:163) ~[na:na]
  	at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.3.9]
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[na:na]
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:190) ~[na:na]
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:163) ~[na:na]
  	at org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter.doFilterInternal(WebMvcMetricsFilter.java:96) ~[na:na]
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[na:na]
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:190) ~[na:na]
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:163) ~[na:na]
  	at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[com.mycompany.simpleservice.  SimpleServiceApplication:5.3.9]
  	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[na:na]
  	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:190) ~[na:na]
  	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:163) ~[na:na]
  	at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:202) ~[na:na]
  	at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:97) ~[na:na]
  	at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:542) ~[na:na]
  	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:143) ~[na:na]
  	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92) ~[com.mycompany.simpleservice.SimpleServiceApplication:9.0.50]
  	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:78) ~[na:na]
  	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:357) ~[na:na]
  	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:382) ~[na:na]
  	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65) ~[na:na]
  	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:893) ~[na:na]
  	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1723) ~[na:na]
  	at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49) ~[na:na]
  	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128) ~[na:na]
  	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628) ~[na:na]
  	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) ~[na:na]
  	at java.lang.Thread.run(Thread.java:829) ~[na:na]
  	at com.oracle.svm.core.thread.JavaThreads.threadStartRoutine(JavaThreads.java:567) ~[na:na]
  	at com.oracle.svm.core.posix.thread.PosixJavaThreads.pthreadStartRoutine(PosixJavaThreads.java:192) ~[na:na]
  ```
