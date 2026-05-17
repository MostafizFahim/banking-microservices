# Deployment Guide

This application should use:

- Supabase for the production PostgreSQL database
- Railway for the Spring Boot backend API
- Vercel for the Angular frontend

Supabase Edge Functions deploy TypeScript functions on a Deno-compatible runtime. This backend is a long-running Java/Spring Boot service, so Railway is the correct production host for the API while Supabase provides the database.

## 1. Create Supabase Database

Create a Supabase project and copy the PostgreSQL connection details from the Supabase dashboard.

For the backend, configure the JDBC URL in this shape:

```text
jdbc:postgresql://db.mtjwllwyiycctdvinnyv.supabase.co:5432/postgres?sslmode=require
```

If you use Supabase's pooled connection string, convert the `postgresql://...` URL to JDBC format and keep `sslmode=require`.

Set these Railway variables from Supabase:

```text
JDBC_DATABASE_URL=jdbc:postgresql://db.mtjwllwyiycctdvinnyv.supabase.co:5432/postgres?sslmode=require
DATABASE_USER=postgres
DATABASE_PASSWORD=<supabase-db-password>
```

## 2. Deploy Backend on Railway

Create a Railway service from the GitHub repository.

Use these Railway settings:

```text
Root Directory: /backend
Config File: /backend/railway.json
```

The backend Dockerfile already starts the app with the production Spring profile.

Add these Railway variables:

```text
SPRING_PROFILES_ACTIVE=prod
JDBC_DATABASE_URL=jdbc:postgresql://db.mtjwllwyiycctdvinnyv.supabase.co:5432/postgres?sslmode=require
DATABASE_USER=postgres
DATABASE_PASSWORD=<supabase-db-password>
ADMIN_SECRET_KEY=<strong-admin-registration-secret>
JWT_SECRET=<stable-strong-jwt-secret>
CORS_ALLOWED_ORIGINS=https://banking-microservices-eta.vercel.app,https://*.vercel.app
```

After deployment, generate a Railway public domain. The backend API base URL will look like:

```text
https://banking-api-production-247d.up.railway.app/api
```

## 3. Point Frontend to Railway

Update the production Angular API URL:

```ts
// banking-ui/src/environments/environment.prod.ts
export const environment = {
  production: true,
  apiUrl: 'https://banking-api-production-247d.up.railway.app/api',
  wsUrl: ''
};
```

Redeploy the frontend on Vercel after this change.

## 4. Register Production Admin

Use the configured `ADMIN_SECRET_KEY` to create the first admin after the backend is live:

```powershell
Invoke-RestMethod `
  -Uri "https://banking-api-production-247d.up.railway.app/api/auth/register-admin" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"change-this-password","email":"admin@example.com","adminKey":"<ADMIN_SECRET_KEY>"}'
```

Use a real password and email for production. Do not commit production secrets.

## 5. Migration Notes

- Fresh Supabase databases should let Hibernate create the current tables with `spring.jpa.hibernate.ddl-auto=update`.
- If you manually migrate an older Render database schema, make sure account ownership allows multiple accounts per user. Do not add a unique constraint to `accounts.email`.
- Keep `JWT_SECRET` stable across Railway redeploys. Changing it invalidates every existing login token.
- After switching backend hosts, users should log out and log in again so the frontend stores a fresh JWT from the new backend.
