# AniPals Backend Hosting

## Runtime

This backend is configured for Java 17:

```powershell
java -version
javac -version
```

Start locally:

```powershell
.\mvnw.cmd spring-boot:run
```

Build the deployable jar:

```powershell
.\mvnw.cmd -DskipTests package
```

The server listens on all network interfaces by default:

```properties
server.address=0.0.0.0
server.port=${PORT:8080}
```

## Required Environment Variables

Set these values on the machine or hosting provider that runs the backend:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://<db-host>:5432/anipals_db
SPRING_DATASOURCE_USERNAME=<db-user>
SPRING_DATASOURCE_PASSWORD=<db-password>
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,https://v6p9d9t4.ssl.hwcdn.net,https://*.itch.zone
PORT=8080
```

For the frontend build, point API calls at the public backend URL:

```text
VITE_API_URL=https://your-backend-domain.com/api
```

Realtime multiplayer clients should connect to:

```text
wss://your-backend-domain.com/ws
```

## Internet-Facing Options

1. Railway: create a new project, add a PostgreSQL database, deploy this `backend` directory from GitHub, and set the environment variables above. `railway.json` and `railpack.json` define the Maven build and jar start command.
   If your GitHub repo contains this backend in a subfolder, set the Railway service **Root Directory** to the folder that contains `pom.xml`, for example `/backend` or `/backend-20260529T140000Z-3-001/backend`. If you use Railway's config-file setting, point it to the same folder, for example `/backend/railway.json`.
2. Render/Fly/VM: deploy the backend jar to a Java app platform or VM, attach a managed PostgreSQL database, set the environment variables above, and allow inbound TCP traffic on the exposed `PORT`.
3. Home network: run the backend on a fixed local machine, create a router port-forward from public TCP `8080` to that machine's local `8080`, and allow Java through the OS firewall.
4. Temporary tunnel: run the backend locally, then expose `localhost:8080` with a tunnel such as Cloudflare Tunnel or ngrok. Use the tunnel URL in `VITE_API_URL`, use its `wss://` URL for `/ws`, and add the frontend origin to `APP_CORS_ALLOWED_ORIGINS`.

For production, use HTTPS in front of the backend. If you terminate TLS with a proxy such as Nginx, Caddy, a load balancer, or the hosting provider, forward traffic to the Spring Boot process on `localhost:8080` or the configured internal port.
