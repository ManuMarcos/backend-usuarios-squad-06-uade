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