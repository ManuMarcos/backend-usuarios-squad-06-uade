# ArreglaYa - Backend de Usuarios

Bienvenido al repositorio backend del servicio de Usuarios de ArreglaYa.  
Proyecto universitario UADE - 2do cuatrimestre 2025, materia Desarrollo de Aplicaciones 2.

El backend esta implementado con Java 21 y Spring Boot y expone un servicio REST para la gestion de usuarios.

---

## Entornos

| Entorno             | URL                                                                |
| ------------------- | ------------------------------------------------------------------ |
| ProducciA3n    | [https://4j0gfpnn0g.execute-api.us-east-2.amazonaws.com/prod/](https://4j0gfpnn0g.execute-api.us-east-2.amazonaws.com/prod/) |
| Desarrollo    | [https://4j0gfpnn0g.execute-api.us-east-2.amazonaws.com/dev/](https://4j0gfpnn0g.execute-api.us-east-2.amazonaws.com/dev/)  |

# dY"- DocumentaciA3n de la API de Usuarios �?" ArreglaYa



> Las suites ahora ejercitan Postgres y LDAP reales. Levanta los contenedores antes de lanzar Maven:
> ```
> docker compose up -d postgres-dev ldap-local
> ```
>
1. Verifica que tengas Java 21 disponible en tu PATH (el wrapper de Maven incluido en el repo ya lo gestiona).
2. Desde la raiz del proyecto ejecuta:
   - Linux / macOS: `./mvnw clean test`
   - Windows (PowerShell o CMD): `./mvnw.cmd clean test`
3. El detalle por clase queda en `target/surefire-reports/`. Para correr solo una clase usa `./mvnw -Dtest=NombreDeTest test`.
4. Para el informe navegable ejecuta `./mvnw surefire-report:report` (`./mvnw.cmd surefire-report:report` en Windows) y abre `target/site/surefire-report.html`.
5. La cobertura se genera con `./mvnw jacoco:report` y se consulta en `target/site/jacoco/index.html`.

Cuando termines podes liberar recursos con `docker compose stop postgres-dev ldap-local`.

### Colección Postman / Newman

Para ejercitar los flujos principales vía HTTP agregamos una colección en `postman/ArreglaYa-Usuarios.postman_collection.json` y un environment local en `postman/local.postman_environment.json`.

1. Levantá la API (`./mvnw spring-boot:run -Dspring-boot.run.profiles=local`) y los contenedores `postgres-dev` + `ldap-local`.
2. Importá ambos archivos en Postman y ejecutá la colección completa (los tests crean un usuario, inician sesión, validan permisos y token).
3. Para automatizarlo desde la terminal podés usar Newman:
   ```bash
   npx newman run postman/ArreglaYa-Usuarios.postman_collection.json \
     -e postman/local.postman_environment.json \
     --reporters cli,html \
     --reporter-html-export target/newman/report.html
   ```
   El informe HTML queda en `target/newman/report.html` y sirve como evidencia de los inputs y respuestas HTTP.

## Levantar el proyecto para pruebas manuales

1. Inicia las dependencias locales necesarias (PostgreSQL y LDAP) con Docker Compose:
   - `docker compose up postgres-local ldap-local -d`
2. Si queres usar configuraciones distintas a las de `application.properties`, defini las variables de entorno correspondientes (`SERVER_PORT`, `DATASOURCE_URL`, etc.).
3. Levanta la aplicacion Spring Boot usando el perfil `local` (lo que carga `application-local.properties`). Recorda mantener las comillas, ya que cada flag se pasa como argumento independiente:
   - Linux / macOS: `./mvnw "-Dspring-boot.run.profiles=local" "-DskipTests" "spring-boot:run"`
   - Windows (PowerShell/CMD): `.\mvnw.cmd "-Dspring-boot.run.profiles=local" "-DskipTests" "spring-boot:run"`
4. La API queda disponible en `http://localhost:8081` (o el puerto que definas con `SERVER_PORT` dentro del perfil local).

Para detener los contenedores de soporte usa `docker compose down` o `docker compose stop postgres-local ldap-local`.

## Documentacion de la API de Usuarios

La especificacion OpenAPI 3.0 se encuentra dentro del proyecto. Para consultarla:

1. Levanta la aplicacion siguiendo la seccion anterior.
2. Abre `http://localhost:8081/swagger-ui/index.html` y navega la documentacion interactiva.



## Casos cubiertos por las suites de test

- `UserServiceTest`:
  - Validaciones de registro manual (email, nombre, apellido, DNI, teléfono, direcciones).
  - Registro duplicado (local vs evento), login exitoso/inválido/inactivo, emisión de JWT.
  - Reset de contraseña vía LDAP, actualización parcial de datos (incluye que no se pueda cambiar rol/DNI) y activación/inactivación de usuarios.
- `UserControllerTest` / `SecurityIntegrationTest`:
  - Acceso a `/api/users` según rol y estado del token (sin token, cliente, admin, expirado).
  - Inserción y consulta de usuarios a través de la API REST.
- `PermissionServiceTest`, `PermissionServiceIntegrationTest`, `PermissionControllerTest` y `PermissionControllerIntegrationTest`:
  - Lectura y verificación de permisos agrupados por módulo, asignación/remoción de permisos y sincronización con roles.
  - Casos de uso funcionando sobre Postgres real (alta/baja de permisos, usuarios inactivos, endpoints HTTP `/api/permissions/...`).
- `LdapUserServiceTest` y `LdapUserServiceIntegrationTest`:
  - Creación de usuarios, autenticación y reseteo de contraseñas contra el OpenLDAP levantado por Docker, verificando también la limpieza de los DN.
- `EventMapperTest`, `IncomingEventProcessorTest`, `EventUserRegisterStrategyTest`, `EventUserUpdateStrategyTest`, `EventUserDeactivateStrategyTest`:
  - Mapeo de eventos de alta/modificación/baja, manejo de campos obligatorios y direcciones.
  - Procesamiento de eventos nuevos vs. duplicados, persistencia en `incoming_events`, envío de respuestas a CoreHub.
- `AddressMapperTest`:
  - Conversión completa entre `AddressInfo` y entidades `Address`, incluyendo casos nulos y la asociación con el usuario propietario.
- `JwtUtilTest`:
  - Generación, validación y parsing de JWT (email, rol, expiración, tokens corruptos).
- `CorePublisherServiceTest`:
  - Composición de payloads y headers para los eventos `user_created`, `user_deactivated` y `user_rejected`.
- `ValidatorsTest`:
  - Reglas de validación previas al registro (formato de email, obligatoriedad de campos, trims y mensajes de error).

Los reportes detallados quedan en `target/surefire-reports/` (resultado de cada clase) y `target/site/surefire-report.html` (resumen HTML generado con `mvnw surefire-report:report`).

### Resumen por flujo

- **Registro:** Validaciones de datos obligatorios, rechazo de duplicados (manual y por evento) y publicaciA3n del evento `user_created`.
- **Login:** AutenticaciA3n contra LDAP, manejo de usuarios inexistentes o inactivos y generaciA3n/validaciA3n de JWT (tokens corruptos/expirados).
- **ModificaciA3n:** ActualizaciA3n parcial (REST + eventos) sin permitir cambios de rol/DNI, sincronizaciA3n con LDAP y manejo de direcciones multiple.
- **Baja/alta:** Cambios en el flag `active` y propagaciA3n hacia Core/LDAP mediante eventos.
- **Permisos:** Lectura/gestión de permisos por módulo, sincronización con roles y asignación/remoción persistente usando `/api/permissions`.
- **Integración LDAP:** Altas, autenticación y reseteo de contraseñas validados contra el OpenLDAP del docker-compose (`ldap-local`).

- **Eventos externos:** Procesamiento idempotente de alta/modificación/baja, persistencia en `incoming_events`, reintento ante duplicados y envío a CoreHub.
- **Frontend (React)**:
  - Está en `frontend/Frontend-usuarios-squad-06-uade-main/arreglaya`.
  - Comandos típicos: `npm install`, `npm start` (abre `http://localhost:3000`).
  - Apunta al backend mediante `REACT_APP_API_BASE` (por defecto `http://localhost:8081`).
  - Autenticación con JWT guardado en `localStorage`; manejos de refresh/expiración en `src/api/http.ts`.
  - Páginas principales dentro de `src/pages` y componentes reutilizables en `src/components`.
