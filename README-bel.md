# API Gateway
[GitHub Repository](https://github.com/alcosi/alcosi_blockchain_api_gateway)

**Spring Boot Starter/Application** + фільтры для **Spring Cloud Gateway**

| Функцыянал                      | Апісанне                                                            |
|---------------------------------|---------------------------------------------------------------------|
| **Канфігураванне роутаў**       | Роўты можна наладжваць праз `env.variables` або `.properties` файлы |
| **Даданне ключоў аўтарызацыі**  | Дадае API-ключы пры маршрутызацыі запытаў                           |
| **Лагаванне запытаў**           | Запіс запытаў у логі і БД                                           |
| **Шыфраванне/дэшыфраванне**     | Падтрымка шыфравання і дэшыфравання запытаў                         |
| **Multipart to JSON**           | Канвертуе `multipart/form-data` у `JSON`                            |
| **Аўтарызацыя перад роўцінгам** | Праверка аўтарызацыі запыта перад перасылкай                        |
| **Лагаванне запытаў у БД**      | Дапаўняе лагаванне захаваннем у базу дадзеных                       |

### 📌 Прыклад канфігурацыі роута:
```properties
filter.config.path.proxy.profile-change-role=\
  {\
    "order":320,\
    "addBasePath":true,\
    "apiKey":"${PLACEHOLDER_PROFILE_API_KEY_MASKED}",\
    "microserviceUri":"${PLACEHOLDER_PROFILE_URI}",\
    "decryptResponse":false,\
    "matches":[\
        {\
         "methods":["PUT"],\
         "path":"/profile/profile/*/role",\
         "authorities":[{"list":["REGISTER_EXPERT","REGISTER_INVESTOR"],"checkMode":"ANY"}]\
        }\
    ]\
  }

