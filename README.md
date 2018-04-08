# springboot-ldap

## Goal

The goal of this project is to create a simple REST API and securing it with the Spring Security LDAP module.

## Start Environment

### Docker Compose

1. Open one terminal

2. Inside `/springboot-ldap/dev` folder run
```
docker-compose up
```

### LDAP

1. Access the link
```
https://localhost:6443
```

2. Login with the credentials
```
Login DN: cn=admin,dc=mycompany,dc=com
Password: admin
```

3. Import the file `ldap-mycompany-com.ldif`

This file has already a pre-defined structure for mycompany.com.
Basically, it has 2 groups (employees and clients) and 3 users (Bill Gates, Steve Jobs and Mark Cuban). Besides, it is defined that Bill Gates and Steve Jobs belong to employees group and Mark Cuban belongs to clients group.
```
Bill Gates > username: bgates, password: 123
Steve Jobs > username: sjobs, password: 123
Mark Cuban > username: mcuban, password: 123
```

### Spring Boot Application

1. Open a new terminal

2. Start `springboot-ldap` application

In `springboot-ldap` root folder, run those 2 commands:
```
mvn clean package
java -jar target/springboot-ldap-0.0.1-SNAPSHOT.jar
```

## Test

1. Open a new terminal

2. Call the endpoint `/api/public` using the cURL command bellow.
```
curl 'http://localhost:8080/api/public'
```
It will return:
```
It is public.
```

3. Try to call the endpoint `/api/private` using the cURL command bellow.
``` 
curl 'http://localhost:8080/api/private'
```
It will return:
```
"status":401,"error":"Unauthorized","message":"Full authentication is required to access this resource"
```

4. Call the endpoint `/api/private` using the cURL command bellow, but now informing username and password.
``` 
curl -u bgates:123 'http://localhost:8080/api/private'
```
It will return:
```
bgates, it is private.
```

5. Call the endpoint `/api/private` using the cURL command bellow, informing an invalid password.
``` 
curl -u bgates:124 'http://localhost:8080/api/private'
```
It will return:
```
"status":401,"error":"Unauthorized","message":"Bad credentials"
```

6. Call the endpoint `/api/private` using the cURL command bellow, informing a not existing user.
``` 
curl -u cslim:123 'http://localhost:8080/api/private'
```
It will return:
```
"status":401,"error":"Unauthorized","message":"Bad credentials"
```

## Useful Links
- https://spring.io/guides/gs/authenticating-ldap/
- http://www.opencodez.com/java/configure-ldap-authentication-using-spring-boot.htm

## How to make a LDAP dump using ldapsearch
```
ldapsearch -Wx -D "cn=admin,dc=mycompany,dc=com" -b "dc=mycompany,dc=com" -H ldap://localhost:389 -LLL > ldap_dump.ldif
```