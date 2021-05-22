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

## Using Tracing Agent to generate the missing configuration for native image

> **IMPORTANT**: The environment variable `JAVA_HOME` must be set to a `GraalVM` installation directory ([Install GraalVM](https://www.graalvm.org/docs/getting-started/#install-graalvm)), and the `native-image` tool must be installed ([Install Native Image](https://www.graalvm.org/reference-manual/native-image/#install-native-image)).

> **TIP**: For more information `Tracing Agent` see [Spring Native documentation](https://docs.spring.io/spring-native/docs/current/reference/htmlsingle/#tracing-agent)

- Run the following steps in a terminal and inside `springboot-keycloak-openldap` root folder
  ```
  mkdir -p simple-service/src/main/resources/META-INF/native-image
  
  ./mvnw clean package --projects simple-service -DskipTests
  
  cd simple-service
  
  java -jar -agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image target/simple-service-1.0.0.jar
  ```

- Once the application is running, exercise it by calling its endpoints using `curl` and `Swagger` so that `Tracing Agent` observes the behavior of the application running on Java HotSpot VM and writes configuration files for reflection, JNI, resource, and proxy usage to automatically configure the native image generator.

- It should generate `JSON` files in `simple-service/src/main/resources/META-INF/native-image` such as: `jni-config.json`, `proxy-config.json`, `reflect-config.json`, `resource-config.json` and `serialization-config.json`.

## Issues

The Docker native image is built successfully. However, the following exception is thrown at application startup
```
ERROR 1 --- [           main] o.s.boot.SpringApplication               : Application run failed

org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'springSecurityFilterChain' defined in class path resource [org/springframework/security/config/annotation/web/configuration/WebSecurityConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [javax.servlet.Filter]: Factory method 'springSecurityFilterChain' threw exception; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'mvcHandlerMappingIntrospector' defined in class path resource [org/springframework/boot/autoconfigure/web/servlet/WebMvcAutoConfiguration$EnableWebMvcConfiguration.class]: Invocation of init method failed; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'resourceHandlerMapping' defined in class path resource [org/springframework/boot/autoconfigure/web/servlet/WebMvcAutoConfiguration$EnableWebMvcConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.web.servlet.HandlerMapping]: Factory method 'resourceHandlerMapping' threw exception; nested exception is io.github.classgraph.ClassGraphException: Uncaught exception during scan
	at org.springframework.beans.factory.support.ConstructorResolver.instantiate(ConstructorResolver.java:658) ~[na:na]
	at org.springframework.beans.factory.support.ConstructorResolver.instantiateUsingFactoryMethod(ConstructorResolver.java:486) ~[na:na]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.instantiateUsingFactoryMethod(AbstractAutowireCapableBeanFactory.java:1334) ~[na:na]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1177) ~[na:na]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:564) ~[na:na]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:524) ~[na:na]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:335) ~[na:na]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[na:na]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:333) ~[na:na]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:208) ~[na:na]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:322) ~[na:na]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:208) ~[na:na]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:944) ~[na:na]
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:918) ~[na:na]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:583) ~[na:na]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:145) ~[na:na]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:758) ~[com.mycompany.simpleservice.SimpleServiceApplication:2.5.0]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:438) ~[com.mycompany.simpleservice.SimpleServiceApplication:2.5.0]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:337) ~[com.mycompany.simpleservice.SimpleServiceApplication:2.5.0]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1336) ~[com.mycompany.simpleservice.SimpleServiceApplication:2.5.0]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1325) ~[com.mycompany.simpleservice.SimpleServiceApplication:2.5.0]
	at com.mycompany.simpleservice.SimpleServiceApplication.main(SimpleServiceApplication.java:10) ~[com.mycompany.simpleservice.SimpleServiceApplication:na]
Caused by: org.springframework.beans.BeanInstantiationException: Failed to instantiate [javax.servlet.Filter]: Factory method 'springSecurityFilterChain' threw exception; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'mvcHandlerMappingIntrospector' defined in class path resource [org/springframework/boot/autoconfigure/web/servlet/WebMvcAutoConfiguration$EnableWebMvcConfiguration.class]: Invocation of init method failed; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'resourceHandlerMapping' defined in class path resource [org/springframework/boot/autoconfigure/web/servlet/WebMvcAutoConfiguration$EnableWebMvcConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.web.servlet.HandlerMapping]: Factory method 'resourceHandlerMapping' threw exception; nested exception is io.github.classgraph.ClassGraphException: Uncaught exception during scan
	at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:185) ~[na:na]
	at org.springframework.beans.factory.support.ConstructorResolver.instantiate(ConstructorResolver.java:653) ~[na:na]
	... 21 common frames omitted
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'mvcHandlerMappingIntrospector' defined in class path resource [org/springframework/boot/autoconfigure/web/servlet/WebMvcAutoConfiguration$EnableWebMvcConfiguration.class]: Invocation of init method failed; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'resourceHandlerMapping' defined in class path resource [org/springframework/boot/autoconfigure/web/servlet/WebMvcAutoConfiguration$EnableWebMvcConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.web.servlet.HandlerMapping]: Factory method 'resourceHandlerMapping' threw exception; nested exception is io.github.classgraph.ClassGraphException: Uncaught exception during scan
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1786) ~[na:na]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:602) ~[na:na]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:524) ~[na:na]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:335) ~[na:na]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[na:na]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:333) ~[na:na]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:213) ~[na:na]
	at org.springframework.context.support.AbstractApplicationContext.getBean(AbstractApplicationContext.java:1160) ~[na:na]
	at org.springframework.security.config.annotation.web.configurers.CorsConfigurer$MvcCorsFilter.getMvcCorsFilter(CorsConfigurer.java:111) ~[na:na]
	at org.springframework.security.config.annotation.web.configurers.CorsConfigurer$MvcCorsFilter.access$000(CorsConfigurer.java:94) ~[na:na]
	at org.springframework.security.config.annotation.web.configurers.CorsConfigurer.getCorsFilter(CorsConfigurer.java:89) ~[na:na]
	at org.springframework.security.config.annotation.web.configurers.CorsConfigurer.configure(CorsConfigurer.java:67) ~[na:na]
	at org.springframework.security.config.annotation.web.configurers.CorsConfigurer.configure(CorsConfigurer.java:41) ~[na:na]
	at org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder.configure(AbstractConfiguredSecurityBuilder.java:349) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.5.0]
	at org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder.doBuild(AbstractConfiguredSecurityBuilder.java:303) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.5.0]
	at org.springframework.security.config.annotation.AbstractSecurityBuilder.build(AbstractSecurityBuilder.java:38) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.5.0]
	at org.springframework.security.config.annotation.web.builders.WebSecurity.performBuild(WebSecurity.java:285) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.5.0]
	at org.springframework.security.config.annotation.web.builders.WebSecurity.performBuild(WebSecurity.java:83) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.5.0]
	at org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder.doBuild(AbstractConfiguredSecurityBuilder.java:305) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.5.0]
	at org.springframework.security.config.annotation.AbstractSecurityBuilder.build(AbstractSecurityBuilder.java:38) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.5.0]
	at org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration.springSecurityFilterChain(WebSecurityConfiguration.java:127) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.5.0]
	at java.lang.reflect.Method.invoke(Method.java:566) ~[na:na]
	at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:154) ~[na:na]
	... 22 common frames omitted
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'resourceHandlerMapping' defined in class path resource [org/springframework/boot/autoconfigure/web/servlet/WebMvcAutoConfiguration$EnableWebMvcConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.web.servlet.HandlerMapping]: Factory method 'resourceHandlerMapping' threw exception; nested exception is io.github.classgraph.ClassGraphException: Uncaught exception during scan
	at org.springframework.beans.factory.support.ConstructorResolver.instantiate(ConstructorResolver.java:658) ~[na:na]
	at org.springframework.beans.factory.support.ConstructorResolver.instantiateUsingFactoryMethod(ConstructorResolver.java:638) ~[na:na]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.instantiateUsingFactoryMethod(AbstractAutowireCapableBeanFactory.java:1334) ~[na:na]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1177) ~[na:na]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:564) ~[na:na]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:524) ~[na:na]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:335) ~[na:na]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[na:na]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:333) ~[na:na]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:208) ~[na:na]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.getBeansOfType(DefaultListableBeanFactory.java:671) ~[na:na]
	at org.springframework.context.support.AbstractApplicationContext.getBeansOfType(AbstractApplicationContext.java:1308) ~[na:na]
	at org.springframework.beans.factory.BeanFactoryUtils.beansOfTypeIncludingAncestors(BeanFactoryUtils.java:378) ~[na:na]
	at org.springframework.web.servlet.handler.HandlerMappingIntrospector.initHandlerMappings(HandlerMappingIntrospector.java:227) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.3.7]
	at org.springframework.web.servlet.handler.HandlerMappingIntrospector.afterPropertiesSet(HandlerMappingIntrospector.java:123) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.3.7]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1845) ~[na:na]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1782) ~[na:na]
	... 44 common frames omitted
Caused by: org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.web.servlet.HandlerMapping]: Factory method 'resourceHandlerMapping' threw exception; nested exception is io.github.classgraph.ClassGraphException: Uncaught exception during scan
	at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:185) ~[na:na]
	at org.springframework.beans.factory.support.ConstructorResolver.instantiate(ConstructorResolver.java:653) ~[na:na]
	... 60 common frames omitted
Caused by: io.github.classgraph.ClassGraphException: Uncaught exception during scan
	at io.github.classgraph.ClassGraph.scan(ClassGraph.java:1319) ~[na:na]
	at io.github.classgraph.ClassGraph.scan(ClassGraph.java:1337) ~[na:na]
	at io.github.classgraph.ClassGraph.scan(ClassGraph.java:1350) ~[na:na]
	at org.webjars.WebJarAssetLocator.scanForWebJars(WebJarAssetLocator.java:144) ~[com.mycompany.simpleservice.SimpleServiceApplication:na]
	at org.webjars.WebJarAssetLocator.<init>(WebJarAssetLocator.java:150) ~[com.mycompany.simpleservice.SimpleServiceApplication:na]
	at org.springframework.web.servlet.resource.WebJarsResourceResolver.<init>(WebJarsResourceResolver.java:61) ~[na:na]
	at org.springframework.web.servlet.config.annotation.ResourceChainRegistration.getResourceResolvers(ResourceChainRegistration.java:114) ~[na:na]
	at org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration.getRequestHandler(ResourceHandlerRegistration.java:195) ~[na:na]
	at org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry.getHandlerMapping(ResourceHandlerRegistry.java:171) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.3.7]
	at org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport.resourceHandlerMapping(WebMvcConfigurationSupport.java:597) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.3.7]
	at java.lang.reflect.Method.invoke(Method.java:566) ~[na:na]
	at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:154) ~[na:na]
	... 61 common frames omitted
Caused by: java.lang.IllegalArgumentException: Exception while invoking method "list"
	at nonapi.io.github.classgraph.utils.ReflectionUtils.invokeMethod(ReflectionUtils.java:268) ~[na:na]
	at nonapi.io.github.classgraph.utils.ReflectionUtils.invokeMethod(ReflectionUtils.java:301) ~[na:na]
	at io.github.classgraph.ModuleReaderProxy.list(ModuleReaderProxy.java:107) ~[na:na]
	at io.github.classgraph.ClasspathElementModule.scanPaths(ClasspathElementModule.java:277) ~[na:na]
	at io.github.classgraph.Scanner$5.processWorkUnit(Scanner.java:1026) ~[na:na]
	at io.github.classgraph.Scanner$5.processWorkUnit(Scanner.java:1020) ~[na:na]
	at nonapi.io.github.classgraph.concurrency.WorkQueue.runWorkLoop(WorkQueue.java:246) ~[na:na]
	at nonapi.io.github.classgraph.concurrency.WorkQueue.runWorkQueue(WorkQueue.java:161) ~[na:na]
	at io.github.classgraph.Scanner.processWorkUnits(Scanner.java:342) ~[na:na]
	at io.github.classgraph.Scanner.openClasspathElementsThenScan(Scanner.java:1018) ~[na:na]
	at io.github.classgraph.Scanner.call(Scanner.java:1078) ~[na:na]
	at io.github.classgraph.Scanner.call(Scanner.java:78) ~[na:na]
	at java.util.concurrent.FutureTask.run(FutureTask.java:264) ~[na:na]
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128) ~[na:na]
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628) ~[na:na]
	at java.lang.Thread.run(Thread.java:834) ~[na:na]
	at com.oracle.svm.core.thread.JavaThreads.threadStartRoutine(JavaThreads.java:519) ~[na:na]
	at com.oracle.svm.core.posix.thread.PosixJavaThreads.pthreadStartRoutine(PosixJavaThreads.java:192) ~[na:na]
Caused by: java.lang.reflect.InvocationTargetException: null
	at java.lang.reflect.Method.invoke(Method.java:566) ~[na:na]
	at nonapi.io.github.classgraph.utils.ReflectionUtils.invokeMethod(ReflectionUtils.java:260) ~[na:na]
	... 17 common frames omitted
Caused by: com.oracle.svm.core.jdk.UnsupportedFeatureError: Unsupported method jdk.internal.module.SystemModuleFinders$SystemImage.reader() is reachable
	at com.oracle.svm.core.util.VMError.unsupportedFeature(VMError.java:87) ~[na:na]
	at jdk.internal.module.SystemModuleFinders$SystemImage.reader(SystemModuleFinders.java:385) ~[na:na]
	at jdk.internal.module.SystemModuleFinders$ModuleContentSpliterator.<init>(SystemModuleFinders.java:508) ~[na:na]
	at jdk.internal.module.SystemModuleFinders$SystemModuleReader.list(SystemModuleFinders.java:483) ~[na:na]
	... 19 common frames omitted
```