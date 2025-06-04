# Vault-Backend
This is the backend of the Vault project: a small and simple encrypted file hosting. Written for the Web Technologies exam @ UniMoRe in Java using (Javalin)[https://javalin.io] and Hibernate.

## Configuration
Running the project will generate a .yml configuration file to edit.
```yaml
config_version: 1.0.0
webserver:
    # port of the server
    port: 8181
    # domains for cors rules
    allowed_domains:
    - http://localhost:3000

# mariadb server credentials
# (mysql is untested, but it may work)
mariadb:
    url: jdbc:mariadb://wsl:3306/hibernate
    user: root
    password: assword

# Where are we storing
uploads:
    mount_point: uploads
    max_size: 5.0E7

# Where are we storing avatars on disk
pfp:
    mount_point: ./pfps
```
