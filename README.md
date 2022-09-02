# springboot-ldap-testcontainers

The goal of this project is to create a simple [`Spring Boot`](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/) REST API, called `simple-service`, and secure it with `Spring Security LDAP` module. We will use [`Testcontainers`](https://www.testcontainers.org/) for integration testing. 

## Application

- ### simple-service

  `Spring Boot` Java Web application that exposes two endpoints:
   - `GET /api/public`: that can be access by anyone, it is not secured;
   - `GET /api/private`: that can just be accessed by users authenticated with valid LDAP credentials.

## Prerequisites

- [`Java 17+`]( https://www.oracle.com/java/technologies/downloads/#java17)
- [`Docker`](https://www.docker.com/)
- [`Docker-Compose`](https://docs.docker.com/compose/install/)

## Start Environment

Open a terminal and inside `springboot-ldap-testcontainers` root folder run
```
docker-compose up -d
```

## Import OpenLDAP Users

The `LDIF` file we will use, `simple-service/src/main/resources/ldap-mycompany-com.ldif`, contains a pre-defined structure for `mycompany.com`. Basically, it has 2 groups (`employees` and `clients`) and 3 users (`Bill Gates`, `Steve Jobs` and `Mark Cuban`). Besides, it's defined that `Bill Gates` and `Steve Jobs` belong to `employees` group and `Mark Cuban` belongs to `clients` group.
```
Bill Gates > username: bgates, password: 123
Steve Jobs > username: sjobs, password: 123
Mark Cuban > username: mcuban, password: 123
```

There are two ways to import those users: by running a script; or by using `phpldapadmin`

### Import users running a script

- In a terminal, make use you are in `springboot-ldap-testcontainers` root folder

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

- Import the file `simple-service/src/main/resources/ldap-mycompany-com.ldif`

- You should see something like

  ![phpldapadmin](documentation/phpldapadmin.jpeg)

## Run application with Maven

- In a terminal, make use you are in `springboot-ldap-testcontainers` root folder

- Run the following command to start `simple-service`
  ```
  ./mvnw clean spring-boot:run --projects simple-service
  ```

## Run application as Docker container

- In a terminal, make sure you are in `springboot-ldap-testcontainers` root folder

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
  |----------------------|---------------------------------------------------------|
  | `LDAP_HOST`          | Specify host of the `LDAP` to use (default `localhost`) |
  | `LDAP_PORT`          | Specify port of the `LDAP` to use (default `389`)       |

- Run Docker Container
  ```
  docker run --rm --name simple-service -p 8080:8080 \
    -e LDAP_HOST=openldap \
    --network springboot-ldap-testcontainers_default \
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

   ![swagger](documentation/simple-service-swagger.jpeg)

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
- To stop and remove docker-compose containers, network and volumes, in a terminal and inside `springboot-ldap-testcontainers` root folder, run the following command
  ```
  docker-compose down -v
  ```

## Running Test Cases

- In a terminal, make sure you are inside `springboot-ldap-testcontainers` root folder

- Run the command below to start the **Unit Tests**
  ```
  ./mvnw clean test --projects simple-service
  ```

- Run the command below to start the **Unit** and **Integration Tests**
  > **Note**: `Testcontainers` will start automatically `OpenLDAP` Docker container before some tests begin and will shut it down when the tests finish.
  ```
  ./mvnw clean verify --projects simple-service
  ```

## Cleanup

To remove the Docker image created by this project, go to a terminal and, inside `springboot-ldap-testcontainers` root folder, run the following script
```
./remove-docker-images.sh
```

## Issues

When using the IDE to run the test cases, it works. When trying to run tests in a terminal, the exception below is thrown. Besides, looks like `spring-native` and `testcontainers` are not working well together. I've removed `spring-native` from the project and running test in the terminal worked! 
```
java.lang.IllegalStateException: Failed to prepare test context using [WebMergedContextConfiguration@332a7fce testClass = SimpleServiceApplicationIT, locations = '{}', classes = '{class com.ivanfranchin.simpleservice.SimpleServiceApplication}', contextInitializerClasses = '[]', activeProfiles = '{}', propertySourceLocations = '{}', propertySourceProperties = '{org.springframework.boot.test.context.SpringBootTestContextBootstrapper=true, server.port=0}', contextCustomizers = set[org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizer@2ddc9a9f, org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory$DuplicateJsonObjectContextCustomizer@740cae06, org.springframework.boot.test.mock.mockito.MockitoContextCustomizer@0, org.springframework.boot.test.web.client.TestRestTemplateContextCustomizer@7bba5817, org.springframework.boot.test.autoconfigure.actuate.metrics.MetricsExportContextCustomizerFactory$DisableMetricExportContextCustomizer@17f9d882, org.springframework.boot.test.autoconfigure.properties.PropertyMappingContextCustomizer@0, org.springframework.boot.test.autoconfigure.web.servlet.WebDriverContextCustomizerFactory$Customizer@72cc7e6f, org.springframework.test.context.support.DynamicPropertiesContextCustomizer@8f479d5, org.springframework.boot.test.context.SpringBootTestArgs@1, org.springframework.boot.test.context.SpringBootTestWebEnvironment@2c1b194a], resourceBasePath = 'src/main/webapp', contextLoader = 'org.springframework.boot.test.context.SpringBootContextLoader', parent = [null]]
	at org.springframework.aot.test.boot.SpringBootAotTestContextProcessor.prepareTestContext(SpringBootAotTestContextProcessor.java:63)
	at org.springframework.aot.test.context.bootstrap.generator.TestContextConfigurationDescriptor.parseTestContext(TestContextConfigurationDescriptor.java:60)
	at org.springframework.aot.test.context.bootstrap.generator.TestContextAotProcessor.generateTestContext(TestContextAotProcessor.java:109)
	at org.springframework.aot.test.context.bootstrap.generator.TestContextAotProcessor.generateTestContexts(TestContextAotProcessor.java:81)
	at org.springframework.aot.test.build.TestContextBootstrapContributor.contribute(TestContextBootstrapContributor.java:61)
	at org.springframework.aot.build.BootstrapCodeGenerator.generate(BootstrapCodeGenerator.java:91)
	at org.springframework.aot.build.BootstrapCodeGenerator.generate(BootstrapCodeGenerator.java:71)
	at org.springframework.aot.test.build.GenerateTestBootstrapCommand.call(GenerateTestBootstrapCommand.java:111)
	at org.springframework.aot.test.build.GenerateTestBootstrapCommand.call(GenerateTestBootstrapCommand.java:47)
	at picocli.CommandLine.executeUserObject(CommandLine.java:1953)
	at picocli.CommandLine.access$1300(CommandLine.java:145)
	at picocli.CommandLine$RunLast.executeUserObjectOfLastSubcommandWithSameParent(CommandLine.java:2352)
	at picocli.CommandLine$RunLast.handle(CommandLine.java:2346)
	at picocli.CommandLine$RunLast.handle(CommandLine.java:2311)
	at picocli.CommandLine$AbstractParseResultHandler.execute(CommandLine.java:2179)
	at picocli.CommandLine.execute(CommandLine.java:2078)
	at org.springframework.aot.test.build.GenerateTestBootstrapCommand.main(GenerateTestBootstrapCommand.java:116)
Caused by: java.lang.IllegalStateException: Mapped port can only be obtained after the container is started
	at org.testcontainers.shaded.com.google.common.base.Preconditions.checkState(Preconditions.java:174)
	at org.testcontainers.containers.ContainerState.getMappedPort(ContainerState.java:149)
	at com.ivanfranchin.simpleservice.SimpleServiceApplicationIT.dynamicProperties(SimpleServiceApplicationIT.java:54)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:568)
	at org.springframework.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:282)
	at org.springframework.test.context.support.DynamicPropertiesContextCustomizer.lambda$buildDynamicPropertiesMap$3(DynamicPropertiesContextCustomizer.java:84)
	at java.base/java.lang.Iterable.forEach(Iterable.java:75)
	at org.springframework.test.context.support.DynamicPropertiesContextCustomizer.buildDynamicPropertiesMap(DynamicPropertiesContextCustomizer.java:82)
	at org.springframework.test.context.support.DynamicPropertiesContextCustomizer.customizeContext(DynamicPropertiesContextCustomizer.java:72)
	at org.springframework.boot.test.context.SpringBootContextLoader$ContextCustomizerAdapter.initialize(SpringBootContextLoader.java:326)
	at org.springframework.boot.SpringApplication.applyInitializers(SpringApplication.java:607)
	at org.springframework.aot.test.boot.BuildTimeTestSpringApplication.prepareContext(BuildTimeTestSpringApplication.java:87)
	at org.springframework.aot.test.boot.BuildTimeTestSpringApplication.run(BuildTimeTestSpringApplication.java:80)
	at org.springframework.aot.test.boot.BuildTimeTestSpringApplication.run(BuildTimeTestSpringApplication.java:49)
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:132)
	at org.springframework.aot.test.boot.SpringBootBuildTimeConfigContextLoader.loadContext(SpringBootBuildTimeConfigContextLoader.java:34)
	at org.springframework.aot.test.boot.SpringBootAotTestContextProcessor.prepareTestContext(SpringBootAotTestContextProcessor.java:60)
	... 16 more
[ERROR]
org.apache.maven.plugin.MojoExecutionException: Could not exec java
    at org.springframework.aot.maven.AbstractBootstrapMojo.forkJvm (AbstractBootstrapMojo.java:197)
    at org.springframework.aot.maven.TestGenerateMojo.execute (TestGenerateMojo.java:151)
    at org.apache.maven.plugin.DefaultBuildPluginManager.executeMojo (DefaultBuildPluginManager.java:137)
    at org.apache.maven.lifecycle.internal.MojoExecutor.doExecute2 (MojoExecutor.java:370)
    at org.apache.maven.lifecycle.internal.MojoExecutor.doExecute (MojoExecutor.java:351)
    at org.apache.maven.lifecycle.internal.MojoExecutor.execute (MojoExecutor.java:215)
    at org.apache.maven.lifecycle.internal.MojoExecutor.execute (MojoExecutor.java:171)
    at org.apache.maven.lifecycle.internal.MojoExecutor.execute (MojoExecutor.java:163)
    at org.apache.maven.lifecycle.internal.LifecycleModuleBuilder.buildProject (LifecycleModuleBuilder.java:117)
    at org.apache.maven.lifecycle.internal.LifecycleModuleBuilder.buildProject (LifecycleModuleBuilder.java:81)
    at org.apache.maven.lifecycle.internal.builder.singlethreaded.SingleThreadedBuilder.build (SingleThreadedBuilder.java:56)
    at org.apache.maven.lifecycle.internal.LifecycleStarter.execute (LifecycleStarter.java:128)
    at org.apache.maven.DefaultMaven.doExecute (DefaultMaven.java:294)
    at org.apache.maven.DefaultMaven.doExecute (DefaultMaven.java:192)
    at org.apache.maven.DefaultMaven.execute (DefaultMaven.java:105)
    at org.apache.maven.cli.MavenCli.execute (MavenCli.java:960)
    at org.apache.maven.cli.MavenCli.doMain (MavenCli.java:293)
    at org.apache.maven.cli.MavenCli.main (MavenCli.java:196)
    at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0 (Native Method)
    at jdk.internal.reflect.NativeMethodAccessorImpl.invoke (NativeMethodAccessorImpl.java:77)
    at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke (DelegatingMethodAccessorImpl.java:43)
    at java.lang.reflect.Method.invoke (Method.java:568)
    at org.codehaus.plexus.classworlds.launcher.Launcher.launchEnhanced (Launcher.java:282)
    at org.codehaus.plexus.classworlds.launcher.Launcher.launch (Launcher.java:225)
    at org.codehaus.plexus.classworlds.launcher.Launcher.mainWithExitCode (Launcher.java:406)
    at org.codehaus.plexus.classworlds.launcher.Launcher.main (Launcher.java:347)
    at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0 (Native Method)
    at jdk.internal.reflect.NativeMethodAccessorImpl.invoke (NativeMethodAccessorImpl.java:77)
    at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke (DelegatingMethodAccessorImpl.java:43)
    at java.lang.reflect.Method.invoke (Method.java:568)
    at org.apache.maven.wrapper.BootstrapMainStarter.start (BootstrapMainStarter.java:47)
    at org.apache.maven.wrapper.WrapperExecutor.execute (WrapperExecutor.java:156)
    at org.apache.maven.wrapper.MavenWrapperMain.main (MavenWrapperMain.java:72)
Caused by: org.apache.maven.plugin.MojoExecutionException: Bootstrap code generator finished with exit code: 1
    at org.springframework.aot.maven.AbstractBootstrapMojo.forkJvm (AbstractBootstrapMojo.java:190)
    at org.springframework.aot.maven.TestGenerateMojo.execute (TestGenerateMojo.java:151)
    at org.apache.maven.plugin.DefaultBuildPluginManager.executeMojo (DefaultBuildPluginManager.java:137)
    at org.apache.maven.lifecycle.internal.MojoExecutor.doExecute2 (MojoExecutor.java:370)
    at org.apache.maven.lifecycle.internal.MojoExecutor.doExecute (MojoExecutor.java:351)
    at org.apache.maven.lifecycle.internal.MojoExecutor.execute (MojoExecutor.java:215)
    at org.apache.maven.lifecycle.internal.MojoExecutor.execute (MojoExecutor.java:171)
    at org.apache.maven.lifecycle.internal.MojoExecutor.execute (MojoExecutor.java:163)
    at org.apache.maven.lifecycle.internal.LifecycleModuleBuilder.buildProject (LifecycleModuleBuilder.java:117)
    at org.apache.maven.lifecycle.internal.LifecycleModuleBuilder.buildProject (LifecycleModuleBuilder.java:81)
    at org.apache.maven.lifecycle.internal.builder.singlethreaded.SingleThreadedBuilder.build (SingleThreadedBuilder.java:56)
    at org.apache.maven.lifecycle.internal.LifecycleStarter.execute (LifecycleStarter.java:128)
    at org.apache.maven.DefaultMaven.doExecute (DefaultMaven.java:294)
    at org.apache.maven.DefaultMaven.doExecute (DefaultMaven.java:192)
    at org.apache.maven.DefaultMaven.execute (DefaultMaven.java:105)
    at org.apache.maven.cli.MavenCli.execute (MavenCli.java:960)
    at org.apache.maven.cli.MavenCli.doMain (MavenCli.java:293)
    at org.apache.maven.cli.MavenCli.main (MavenCli.java:196)
    at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0 (Native Method)
    at jdk.internal.reflect.NativeMethodAccessorImpl.invoke (NativeMethodAccessorImpl.java:77)
    at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke (DelegatingMethodAccessorImpl.java:43)
    at java.lang.reflect.Method.invoke (Method.java:568)
    at org.codehaus.plexus.classworlds.launcher.Launcher.launchEnhanced (Launcher.java:282)
    at org.codehaus.plexus.classworlds.launcher.Launcher.launch (Launcher.java:225)
    at org.codehaus.plexus.classworlds.launcher.Launcher.mainWithExitCode (Launcher.java:406)
    at org.codehaus.plexus.classworlds.launcher.Launcher.main (Launcher.java:347)
    at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0 (Native Method)
    at jdk.internal.reflect.NativeMethodAccessorImpl.invoke (NativeMethodAccessorImpl.java:77)
    at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke (DelegatingMethodAccessorImpl.java:43)
    at java.lang.reflect.Method.invoke (Method.java:568)
    at org.apache.maven.wrapper.BootstrapMainStarter.start (BootstrapMainStarter.java:47)
    at org.apache.maven.wrapper.WrapperExecutor.execute (WrapperExecutor.java:156)
    at org.apache.maven.wrapper.MavenWrapperMain.main (MavenWrapperMain.java:72)
```
