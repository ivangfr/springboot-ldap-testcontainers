# springboot-ldap

The goal of this project is to create a simple [`Spring Boot`](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/) REST API, called `simple-service`, and secure it with `Spring Security LDAP` module.

## Application

- **simple-service**

  `Spring Boot` Java Web application that exposes two endpoints:
   - `/api/public`: that can be access by anyone, it is not secured;
   - `/api/private`: that can just be accessed by users authenticated with valid LDAP credentials.

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

The `LDIF` file we will use, `springboot-ldap/ldap/ldap-mycompany-com.ldif`, contains already a pre-defined structure for `mycompany.com`. Basically, it has 2 groups (`employees` and `clients`) and 3 users (`Bill Gates`, `Steve Jobs` and `Mark Cuban`). Besides, it is defined that `Bill Gates` and `Steve Jobs` belong to `employees` group and `Mark Cuban` belongs to `clients` group.
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

  ![openldap](images/openldap.png)

## Run application

- In a terminal, make use you are in `springboot-ldap` root folder

- Run the following command to start `simple-service`
  ```
  ./mvnw clean spring-boot:run --projects simple-service
  ```

## Testing using curl

1. Open a terminal

1. Call the endpoint `/api/public`
   ```
   curl -i http://localhost:8080/api/public
   ```

   It will return
   ```
   HTTP/1.1 200
   It is public.
   ```

1. Try to call the endpoint `/api/private` without credentials
   ``` 
   curl -i http://localhost:8080/api/private
   ```
   
   It will return
   ```
   HTTP/1.1 401
   {
     "timestamp": "2018-06-02T22:39:18.534+0000",
     "status": 401,
     "error": "Unauthorized",
     "message": "Unauthorized",
     "path": "/api/private"
   }
   ```

1. Call the endpoint `/api/private` again. This time informing `username` and `password`
   ``` 
   curl -i -u bgates:123 http://localhost:8080/api/private
   ```
   
   It will return
   ```
   HTTP/1.1 200
   bgates, it is private.
   ```

1. Call the endpoint `/api/private` informing an invalid password
   ``` 
   curl -i -u bgates:124 http://localhost:8080/api/private
   ```
   
   It will return
   ```
   HTTP/1.1 401 
   {
     "timestamp": "2018-06-02T22:42:29.221+0000",
     "status": 401,
     "error": "Unauthorized",
     "message": "Unauthorized",
     "path": "/api/private"
   }
   ```

1. Call the endpoint `/api/private` informing a non-existing user
   ``` 
   curl -i -u cslim:123 http://localhost:8080/api/private
   ```
   
   It will return
   ```
   HTTP/1.1 401
   {
     "timestamp": "2018-06-02T22:44:13.617+0000",
     "status": 401,
     "error": "Unauthorized",
     "message": "Unauthorized",
     "path": "/api/private"
   }
   ```

## Testing using Swagger

1. Access 1

   ![swagger](images/swagger.png)

1. Click on `simple-service-controller` to open it.

1. Click on `GET /api/public`, then on `Try it out` button and, finally, on `Execute` button. It will return
   ```
   Code: 200
   Response Body: It is public.
   ```

1. Click on `GET /api/private`, it is a secured endpoint. Then, click on `Try it out` button and, finally, on `Execute` button. A window will appear to inform the username and password. Type
   ```
   username: bgates
   password: 123
   ```
   
   It will return
   ```
   Code: 200
   Response Body: bgates, it is private.
   ```

## Shutdown

- Go to the terminal where `simple-service` is running and press `Ctrl+C`

- In `springboot-ldap` root folder, run the command below to stop and remove docker-compose containers, networks and volumes
  ```
  docker-compose down -v
  ```
