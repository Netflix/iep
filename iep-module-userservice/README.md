
## Description

Sets up a user service that can be used to validate email addresses for internal users.
It depends on having a source of all valid users that can be polled via a simple HTTP
endpoint.

Sample usage:

```java
class Foo {
  
  private final UserService service;
  
  @Inject
  public Foo(UserService service) {
    this.service = service;
  }
  
  public void doSomething(String email) {
    // Check if email is valid
    if (!service.isValidEmail(email)) {
      System.out.println("invalid email: " + email);
    }
    
    // Return a valid email or null if none was found. This can be used if there is a
    // rewrite, e.g., to map a former employee that left to another user.
    String validEmail = service.toValidEmail(email);
    if (validEmail == null) {
      System.out.println("invalid email: " + email);
    } else {
      // Do something with the valid email
    }
  }
}
```

## Gradle

```
compile "com.netflix.iep:iep-module-userservice:${version_iep}"
```