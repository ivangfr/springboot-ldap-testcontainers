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
  ./mvnw clean package spring-boot:run --projects simple-service -DskipTests
  ```

## Run application as Docker container

- ### Build Docker Image
  
  - In a terminal, make sure you are in `springboot-ldap` root folder 
  - Run the following script
    - JVM
      ```
      ./docker-build.sh
      ```
    - Native (it’s not working yet, see [Issues](#issues))
      ```
      ./docker-build.sh native
      ```

- ### Environment Variable

  | Environment Variable | Description                                             |
  | -------------------- | ------------------------------------------------------- |
  | `LDAP_HOST`          | Specify host of the `LDAP` to use (default `localhost`) |
  | `LDAP_PORT`          | Specify port of the `LDAP` to use (default `389`)       |

- ### Start Docker Container
  
  In a terminal, run the following command
  ```
  docker run -d --rm --name simple-service -p 8080:8080 \
    -e LDAP_HOST=openldap \
    --network springboot-ldap_default \
    docker.mycompany.com/simple-service:1.0.0
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

- To stop `simple-service` application
  - If it was started with `Maven`, go to the terminal where it is running and press `Ctrl+C`
  - If it was started as a Docker container, run in a terminal the command below
    ```
    docker stop simple-service
    ```
- To stop and remove docker-compose containers, network and volumes, in a terminal and inside `springboot-ldap` root folder, run the following command
  ```
  docker-compose down -v
  ```

## Issues

After building successfully `simple-service` Docker native image, the following exception is thrown at runtime. It's related to `springdoc-openapi`
```
ERROR 1 --- [           main] o.s.boot.SpringApplication               : Application run failed

java.lang.IllegalStateException: Error processing condition on org.springdoc.core.SpringDocConfiguration.springdocBeanFactoryPostProcessor
	at org.springframework.boot.autoconfigure.condition.SpringBootCondition.matches(SpringBootCondition.java:60) ~[com.mycompany.simpleservice.SimpleServiceApplication:na]
	at org.springframework.context.annotation.ConditionEvaluator.shouldSkip(ConditionEvaluator.java:108) ~[na:na]
	at org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader.loadBeanDefinitionsForBeanMethod(ConfigurationClassBeanDefinitionReader.java:193) ~[na:na]
	at org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader.loadBeanDefinitionsForConfigurationClass(ConfigurationClassBeanDefinitionReader.java:153) ~[na:na]
	at org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader.loadBeanDefinitions(ConfigurationClassBeanDefinitionReader.java:129) ~[na:na]
	at org.springframework.context.annotation.ConfigurationClassPostProcessor.processConfigBeanDefinitions(ConfigurationClassPostProcessor.java:343) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.3.6]
	at org.springframework.context.annotation.ConfigurationClassPostProcessor.postProcessBeanDefinitionRegistry(ConfigurationClassPostProcessor.java:247) ~[com.mycompany.simpleservice.SimpleServiceApplication:5.3.6]
	at org.springframework.context.support.PostProcessorRegistrationDelegate.invokeBeanDefinitionRegistryPostProcessors(PostProcessorRegistrationDelegate.java:311) ~[na:na]
	at org.springframework.context.support.PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(PostProcessorRegistrationDelegate.java:112) ~[na:na]
	at org.springframework.context.support.AbstractApplicationContext.invokeBeanFactoryPostProcessors(AbstractApplicationContext.java:746) ~[na:na]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:564) ~[na:na]
	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:144) ~[na:na]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:782) ~[com.mycompany.simpleservice.SimpleServiceApplication:2.4.5]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:774) ~[com.mycompany.simpleservice.SimpleServiceApplication:2.4.5]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:439) ~[com.mycompany.simpleservice.SimpleServiceApplication:2.4.5]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:339) ~[com.mycompany.simpleservice.SimpleServiceApplication:2.4.5]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1340) ~[com.mycompany.simpleservice.SimpleServiceApplication:2.4.5]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1329) ~[com.mycompany.simpleservice.SimpleServiceApplication:2.4.5]
	at com.mycompany.simpleservice.SimpleServiceApplication.main(SimpleServiceApplication.java:10) ~[com.mycompany.simpleservice.SimpleServiceApplication:na]
Caused by: java.lang.IllegalStateException: java.io.FileNotFoundException: class path resource [org/springdoc/core/CacheOrGroupedOpenApiCondition$OnCacheDisabled.class] cannot be opened because it does not exist
	at org.springframework.boot.autoconfigure.condition.AbstractNestedCondition$MemberConditions.getMetadata(AbstractNestedCondition.java:149) ~[na:na]
	at org.springframework.boot.autoconfigure.condition.AbstractNestedCondition$MemberConditions.getMemberConditions(AbstractNestedCondition.java:121) ~[na:na]
	at org.springframework.boot.autoconfigure.condition.AbstractNestedCondition$MemberConditions.<init>(AbstractNestedCondition.java:114) ~[na:na]
	at org.springframework.boot.autoconfigure.condition.AbstractNestedCondition.getMatchOutcome(AbstractNestedCondition.java:62) ~[com.mycompany.simpleservice.SimpleServiceApplication:na]
	at org.springframework.boot.autoconfigure.condition.SpringBootCondition.matches(SpringBootCondition.java:47) ~[com.mycompany.simpleservice.SimpleServiceApplication:na]
	... 18 common frames omitted
Caused by: java.io.FileNotFoundException: class path resource [org/springdoc/core/CacheOrGroupedOpenApiCondition$OnCacheDisabled.class] cannot be opened because it does not exist
	at org.springframework.core.io.ClassPathResource.getInputStream(ClassPathResource.java:187) ~[na:na]
	at org.springframework.core.type.classreading.SimpleMetadataReader.getClassReader(SimpleMetadataReader.java:55) ~[na:na]
	at org.springframework.core.type.classreading.SimpleMetadataReader.<init>(SimpleMetadataReader.java:49) ~[na:na]
	at org.springframework.core.type.classreading.SimpleMetadataReaderFactory.getMetadataReader(SimpleMetadataReaderFactory.java:103) ~[na:na]
	at org.springframework.core.type.classreading.SimpleMetadataReaderFactory.getMetadataReader(SimpleMetadataReaderFactory.java:81) ~[na:na]
	at org.springframework.boot.autoconfigure.condition.AbstractNestedCondition$MemberConditions.getMetadata(AbstractNestedCondition.java:146) ~[na:na]
	... 22 common frames omitted
```