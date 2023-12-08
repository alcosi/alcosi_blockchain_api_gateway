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