# springboot-ldap

## Goal

The goal of this project is to create a simple REST API and secure it with the Spring Security LDAP module.

## Start Environment

### Docker Compose

1. Open one terminal

2. Inside `/springboot-ldap` root folder run
```
docker-compose up -d
```
> To stop and remove containers, networks, images, and volumes type:
> ```
> docker-compose down -v
> ```

### [OpenLDAP](https://www.openldap.org/)

1. Access the link: https://localhost:6443

2. Login with the credentials
```
Login DN: cn=admin,dc=mycompany,dc=com
Password: admin
```

3. Import the file `ldap-mycompany-com.ldif` that is in `src/main/java/resources`

This file has already a pre-defined structure for mycompany.com.
Basically, it has 2 groups (employees and clients) and 3 users (Bill Gates, Steve Jobs and Mark Cuban).
Besides, it is defined that Bill Gates and Steve Jobs belong to employees group and Mark Cuban belongs to clients group.
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
mvn clean spring-boot:run
```

## Testing using cUrl

1. Open a new terminal

2. Call the endpoint `/api/public` using the cURL command bellow.
```
curl -i http://localhost:8080/api/public
```
It will return:
```
Code: 200
Response Body: It is public.
```

3. Try to call the endpoint `/api/private` using the cURL command bellow.
``` 
curl -i http://localhost:8080/api/private
```
It will return:
```
Code: 401
Reponse Body:
{
  "timestamp": "2018-06-02T22:39:18.534+0000",
  "status": 401,
  "error": "Unauthorized",
  "message": "Unauthorized",
  "path": "/api/private"
}
```

4. Call the endpoint `/api/private` using the cURL command bellow. However, now informing `username` and `password`.
``` 
curl -i -u bgates:123 http://localhost:8080/api/private
```
It will return:
```
Code: 200
Response Body: bgates, it is private.
```

5. Call the endpoint `/api/private` using the cURL command bellow, informing an invalid password.
``` 
curl -i -u bgates:124 http://localhost:8080/api/private
```
It will return:
```
Code: 401
Response Body: 
{
  "timestamp": "2018-06-02T22:42:29.221+0000",
  "status": 401,
  "error": "Unauthorized",
  "message": "Unauthorized",
  "path": "/api/private"
}
```

6. Call the endpoint `/api/private` using the cURL command bellow, informing a non-existing user.
``` 
curl -i -u cslim:123 http://localhost:8080/api/private
```
It will return:
```
Code: 401
Response Body:
{
  "timestamp": "2018-06-02T22:44:13.617+0000",
  "status": 401,
  "error": "Unauthorized",
  "message": "Unauthorized",
  "path": "/api/private"
}
```

## Testing using Swagger

1. Access the link
```
http://localhost:8080/swagger-ui.html
```

2. Click on `application-controller` to open it.

3. Click on `GET /api/public`, click on `Try it out` button and, finally, click on `Execute` button
It will return:
```
Code: 200
Response Body: It is public.
```

4. Now, click on `GET /api/private`, it is a secured endpoint. Then, click on `Try it out` button and, finally, click on `Execute` button

5. A window will appear to inform the username and password. Type
```
username: bgates
password: 123
```
It will return:
```
Code: 200
Response Body: bgates, it is private.
```

## References

- https://spring.io/guides/gs/authenticating-ldap/
- http://www.opencodez.com/java/configure-ldap-authentication-using-spring-boot.htm

## How to make a LDAP dump using ldapsearch
```
ldapsearch -Wx -D "cn=admin,dc=mycompany,dc=com" -b "dc=mycompany,dc=com" -H ldap://localhost:389 -LLL > ldap_dump.ldif
```