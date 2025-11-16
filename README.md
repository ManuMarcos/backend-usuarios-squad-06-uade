# ArreglaYa - Backend de Usuarios

Bienvenido al repositorio backend del servicio de Usuarios de ArreglaYa.  
Proyecto universitario UADE - 2do cuatrimestre 2025, materia Desarrollo de Aplicaciones 2.

El backend esta implementado con Java 21 y Spring Boot y expone un servicio REST para la gestion de usuarios.

---

## Ejecutar los tests

1. Verifica que tengas Java 21 disponible en tu PATH (el wrapper de Maven incluido en el repo ya lo gestiona).
2. Desde la raiz del proyecto ejecuta uno de los siguientes comandos:
   - Linux / macOS: `./mvnw clean test`
   - Windows (PowerShell o CMD): `.\mvnw.cmd clean test`
3. Los reportes quedan en `target/surefire-reports/`.

Para correr una clase de pruebas puntual: `./mvnw -Dtest=NombreDeTest test`.

¿Querés validar contra los servicios Docker (PostgreSQL/LDAP)? Levantá los contenedores (`docker compose up -d postgres-local ldap-local`) y ejecuta los tests con el perfil `docker` sumado al `test`:

- Linux / macOS: `./mvnw "-Dspring.profiles.active=test,docker" test`
- Windows: `.\mvnw.cmd "-Dspring.profiles.active=test,docker" test`

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



apagar docker : docker compose down

levantar docker : docker compose up -d backend-app-dev postgres-dev ldap-dev

.\mvnw.cmd "-Dspring.profiles.active=test,docker" test

.\mvnw surefire-report:report

docker compose exec postgres-dev sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "\\dt"'

docker compose logs -f

## Ejecutar tests sin mocks (usando Postgres + LDAP reales)

1. Levantá los servicios de soporte definidos en Docker Compose (PostgreSQL y LDAP). Si querés todo el stack, podés usar:
   ```
   docker compose up -d postgres-dev ldap-dev
   ```
   (o `postgres-local`/`ldap-local` según el archivo `docker-compose.yml` que estés usando).
2. Ejecutá los tests activando simultáneamente los perfiles `test` y `docker` para que Spring Boot apunte a los contenedores:
   - Windows:
     ```
     .\mvnw.cmd "-Dspring.profiles.active=test,docker" test
     ```
   - Linux / macOS:
     ```
     ./mvnw "-Dspring.profiles.active=test,docker" test
     ```
   Podés limitarlo a una clase usando `-Dtest=NombreDeTest`.
3. Al terminar, apagá los servicios con `docker compose down` (o `docker compose stop postgres-dev ldap-dev` si querés dejarlos listos para reusar).

Estas ejecuciones usan los repositorios JPA y el cliente LDAP reales, así que validan la integración sin mocks.

## Casos cubiertos por las suites de test

- `UserServiceTest`:
  - Validaciones de registro manual (email, nombre, apellido, DNI, teléfono, direcciones).
  - Registro duplicado (local vs evento), login exitoso/inválido/inactivo, emisión de JWT.
  - Reset de contraseña vía LDAP, actualización parcial de datos (incluye que no se pueda cambiar rol/DNI) y activación/inactivación de usuarios.
- `UserControllerTest` / `SecurityIntegrationTest`:
  - Acceso a `/api/users` según rol y estado del token (sin token, cliente, admin, expirado).
  - Inserción y consulta de usuarios a través de la API REST.
- `PermissionServiceTest` y `PermissionControllerTest` + `PermissionControllerIntegrationTest`:
  - Lectura y verificación de permisos agrupados por módulo, asignación/remoción de permisos y sincronización con roles.
  - Endpoints HTTP `/api/permissions/...` con datos persistidos en la BD.
- `EventMapperTest`, `IncomingEventProcessorTest`, `EventUserRegisterStrategyTest`, `EventUserUpdateStrategyTest`, `EventUserDeactivateStrategyTest`:
  - Mapeo de eventos de alta/modificación/baja, manejo de campos obligatorios y direcciones.
  - Procesamiento de eventos nuevos vs. duplicados, persistencia en `incoming_events`, envío de respuestas a CoreHub.
- `JwtUtilTest`:
  - Generación, validación y parsing de JWT (email, rol, expiración, tokens corruptos).
- `CorePublisherServiceTest`:
  - Composición de payloads y headers para los eventos `user_created`, `user_deactivated` y `user_rejected`.
- `ValidatorsTest`:
  - Reglas de validación previas al registro (formato de email, obligatoriedad de campos, trims y mensajes de error).

Los reportes detallados quedan en `target/surefire-reports/` (resultado de cada clase) y `target/reports/surefire.html` (resumen HTML).

### Resumen por flujo

- **Registro:** Validaciones de datos obligatorios, rechazo de duplicados (manual y por evento) y publicaciA3n del evento `user_created`.
- **Login:** AutenticaciA3n contra LDAP, manejo de usuarios inexistentes o inactivos y generaciA3n/validaciA3n de JWT (tokens corruptos/expirados).
- **ModificaciA3n:** ActualizaciA3n parcial (REST + eventos) sin permitir cambios de rol/DNI, sincronizaciA3n con LDAP y manejo de direcciones multiple.
- **Baja/alta:** Cambios en el flag `active` y propagaciA3n hacia Core/LDAP mediante eventos.
- **Permisos:** Lectura/gestiA3n de permisos por mA3dulo y sincronizaciA3n con roles usando `/api/permissions`.
- **Eventos externos:** Procesamiento idempotente de alta/modificaciA3n/baja, persistencia en `incoming_events`, reintento ante duplicados y envA-o a CoreHub.
- **Frontend (React)**:
  - Está en `frontend/Frontend-usuarios-squad-06-uade-main/arreglaya`.
  - Comandos típicos: `npm install`, `npm start` (abre `http://localhost:3000`).
  - Apunta al backend mediante `REACT_APP_API_BASE` (por defecto `http://localhost:8081`).
  - Autenticación con JWT guardado en `localStorage`; manejos de refresh/expiración en `src/api/http.ts`.
  - Páginas principales dentro de `src/pages` y componentes reutilizables en `src/components`.
