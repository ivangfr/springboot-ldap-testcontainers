#!/usr/bin/env bash

ldapadd -x -D "cn=admin,dc=mycompany,dc=com" -w admin -H ldap:// -f simple-service/src/main/resources/ldap-mycompany-com.ldif
