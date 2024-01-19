The application is a ready API Gateway with implemented features, including:

- Logging
- Authorization
- OpenAPI and Swagger distribution
- Authentication and authorization with Ethereum wallets, including refresh tokens.

Customization is possible by configuring HTTP requests before/after each of the login steps.

To customize the application, follow these steps:

1. Put a JAR with a custom implementation of interfaces in `/opt/external-jar`. You can set the path using the environment variable `external.jar.directory.path`.

2. To develop new implementations of interfaces, you can connect the current project as a compile-time dependency.

All classes and methods are made open to facilitate customization. Implementations of interfaces are loaded only if no other (external) implementation is found, simplifying the development of add-ons.

The application is tested and recommended to run in a Docker container. You can find an example configuration in the `docker-compose` directory.



Error table
| Http code | Body code | Description | Endpoint |
| :---: | :---: | :---: | :---: |
| 401 | 401100 | Wrong signer of message | /v1/auth/login/{wallet} POST |
| 401 | 401101 | Nonce is not saved | /v1/auth/login/{wallet} POST |
| 401 | 401102 | Not valid RT(Refresh token) data | /v1/auth/login/{wallet} PUT |
| 401 | 401110 |Wallet is wrong for this JWT | /v1/auth/login/{wallet} PUT |
| 401 | 401120 |Wrong token type. (Not Bearerer JWT) | Any authorised|
| 500 | 5000 | Unknown |

Add to API_OPENAPI directory your openApi (default openapi.yaml)


# Project Compilation

## Gradle Usage and Build Configuration
1. Gradle is used for compilation, and the build itself is described in `build.gradle.kts`.
    - The Gradle version is stored in `gradle/wrapper/gradle-wrapper.properties`. The required version can be downloaded by Gradle using the command `gradle wrapper`.

## Package Authentication
2. There are packages that require authentication, including libraries/plugins developed by us which will go into OpenSource. Even when they are publicly available, a login to GitHub is necessary.
    - Example from the build file:
      ```kotlin
      url = uri("https://maven.pkg.github.com/alcosi/alcosi_commons_library")
      credentials {
          username = "${System.getenv()["GIHUB_PACKAGE_USERNAME"]}"
          password = "${System.getenv()["GIHUB_PACKAGE_TOKEN"]}"
      }
      ```
    - For local builds, credentials are taken from environment variables. Under CI/CD, your own accounts should be used, and these variables must be substituted accordingly.

## Docker Image Building
3. In Gradle, a plugin for building Docker images (com.bmuschko.docker-remote-api) is connected.
    - Access to the repository through the plugin is configured as follows:
      ```kotlin
      docker {
          registryCredentials {
              url.set("https://${dockerRegistry}")
              username.set(dockerUsername)
              password.set(dockerPass)
          }
      }
      ```
    - Credentials are set in variables and taken from the environment variables `DOCKER_USERNAME` and `DOCKER_PASSWORD`.

## Gradle Build Commands
4. Compilation through Gradle is performed with the command `gradle *taskName*`.
    - For manual compilation, typically the task `gradle pushDockerImage` is executed, which includes compilation, jar file assembly, Docker image creation, and pushing the image.
    - Simple jar file compilation can be achieved with the task `gradle bootJar`.

## Docker Compose Configuration
5. Example of a docker-compose file `docker-compose/dev/docker-compose.yml`. In the test/prod version, memory limits and other restrictions apply.
    - Environment variables are stored in `envs/dev.env`.
    - For production environment variables (like DB password), alternative secure storage solutions are needed as storing them in the repository is not recommended.