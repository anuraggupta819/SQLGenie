# Deploying SQLGenie to Azure

Designed to run at **zero ongoing cost** — see the comment block at the top of
[`infra/main.bicep`](../infra/main.bicep) for exactly which Azure free tiers this relies on and
why each one was chosen. Nothing in this doc provisions anything by itself; it's a guide for you
to run when you're ready, via the manual `Deploy to Azure` GitHub Actions workflow.

## 1. One-time setup

### Database (Neon, free tier)

1. Create a free project at [neon.tech](https://neon.tech).
2. Open the SQL editor (or connect via `psql` using the connection string Neon gives you) and run:

   ```sql
   CREATE ROLE app_owner LOGIN PASSWORD 'choose-a-strong-password';
   CREATE ROLE readonly_query_user LOGIN PASSWORD 'choose-a-different-strong-password';
   GRANT CREATE ON DATABASE neondb TO app_owner;
   ```

   (Replace `neondb` with your actual database name if different — Neon shows it in the
   connection string.) Everything else — the `app`/`target` schemas and the read-only role's
   grants — is created automatically by Flyway on first startup, exactly like local dev.
3. Note two JDBC URLs, both pointing at the same database, differing only in credentials:
   - `jdbc:postgresql://<neon-host>/<db>?sslmode=require` (Neon requires SSL)

### Azure (federated login, no long-lived secret)

```bash
az ad app create --display-name sqlgenie-deploy
# note the appId from the output, then:
az ad sp create --id <appId>
az account show --query id -o tsv   # your subscription ID
az role assignment create --assignee <appId> --role Contributor \
  --scope /subscriptions/<subscription-id>
az ad app federated-credential create --id <appId> --parameters '{
  "name": "github-deploy",
  "issuer": "https://token.actions.githubusercontent.com",
  "subject": "repo:anuraggupta819/SQLGenie:ref:refs/heads/master",
  "audiences": ["api://AzureADTokenExchange"]
}'
```

This lets GitHub Actions authenticate to Azure via OIDC — no client secret stored anywhere,
nothing to rotate or leak.

### GitHub repo secrets

Settings → Secrets and variables → Actions → New repository secret:

| Secret | Value |
|---|---|
| `AZURE_CLIENT_ID` | the `appId` from above |
| `AZURE_TENANT_ID` | `az account show --query tenantId -o tsv` |
| `AZURE_SUBSCRIPTION_ID` | `az account show --query id -o tsv` |
| `AZURE_LOCATION` | an Azure region, e.g. `eastus` |
| `NEON_DB_URL` | `jdbc:postgresql://<neon-host>/<db>?sslmode=require` |
| `NEON_DB_USERNAME` | `app_owner` |
| `NEON_DB_PASSWORD` | the password you set for `app_owner` |
| `NEON_READONLY_DB_URL` | same as `NEON_DB_URL` |
| `NEON_READONLY_DB_USERNAME` | `readonly_query_user` |
| `NEON_READONLY_DB_PASSWORD` | the password you set for `readonly_query_user` |
| `PROD_JWT_SECRET` | a random 32+ byte string (e.g. `openssl rand -base64 32`) |
| `GROQ_API_KEY` | your Groq API key |

`GITHUB_TOKEN` (used for both GHCR push and as the GHCR pull credential on the Container App)
is provided automatically by Actions — nothing to add for that one.

## 2. Deploy

Actions tab → **Deploy to Azure** → **Run workflow**. This builds and pushes the backend image
to GHCR, deploys `infra/main.bicep` (creates the Container Apps environment, the backend
Container App, and the Static Web App), then builds and deploys the frontend.

Re-running the same workflow later updates the deployment in place — Bicep deployments are
idempotent, and a new backend image just creates a new Container App revision.

## 3. Verify it's actually free

- Azure Portal → Cost Management → Cost analysis, scoped to the `sqlgenie-rg` resource group.
- Consider setting a budget alert (Cost Management → Budgets) at a low threshold (e.g. $1) as a
  safety net — should never fire given the architecture, but costs nothing to have.
- The backend scales to zero when idle (`minReplicas: 0` in `main.bicep`) — the only way to
  exceed the Container Apps free grant would be sustained heavy traffic, not an idle demo app.

## 4. Tear down

When you're done demoing and want to guarantee zero footprint:

```bash
az group delete --name sqlgenie-rg --yes
```

Neon's project can similarly be deleted from its dashboard if you want the database gone too.
