// SQLGenie backend: a Container App using Azure's real, ongoing free monthly
// grant (not a 12-month trial) - easily enough for a low-traffic demo when
// the app scales to zero when idle.
//
// This subscription is a Free Trial, which caps Container Apps Environments
// at 1 *per subscription*, not per region - a hard anti-abuse limit that
// isn't liftable via a normal quota-increase request on this tier. Rather
// than fight that limit, this template joins the environment from another
// project (Cartify) already on this subscription instead of provisioning
// its own - each app still gets its own container, scaling, secrets, and
// revision history; only the environment (the compute/network boundary,
// and its Log Analytics workspace) is shared.
//
// Cross-resource-group joins to that environment (deploying to sqlgenie-rg
// while referencing an environment in cartify-rg) consistently failed
// ManagedEnvironmentNotFound/LinkedAuthorizationFailed even after granting
// Contributor at both the single-resource and resource-group scope on
// cartify-rg - almost certainly the same tenant-specific RBAC quirk that
// also breaks the Azure CLI's --scope flag on this account (see
// INTERVIEW_PREP.md hurdle #15). Deploying this template *directly into
// cartify-rg* instead sidesteps the cross-RG authorization path entirely by
// making it a same-scope deployment, which is why the backend Container App
// resource physically lives in cartify-rg rather than sqlgenie-rg.
//
// No Azure Container Registry (GHCR is free for public images). No
// Azure-managed Postgres (Flexible Server has no perpetual free SKU) - the
// database is external (Neon), the backend just connects to it. No Key
// Vault - Container Apps' own built-in secrets are used instead.

@description('Azure region for the backend Container App - must match the shared environment\'s region')
param location string = 'eastus2'

@description('Base name used to derive resource names')
param appName string = 'sqlgenie'

@description('Name of the pre-existing Container Apps Environment (in this same resource group) that this app joins')
param sharedEnvironmentName string = 'cartify-env'

@description('Full GHCR image reference for the backend, e.g. ghcr.io/anuraggupta819/sqlgenie-backend:latest')
param backendImage string

@description('GHCR username for pulling the backend image')
param ghcrUsername string

@secure()
@description('GHCR personal access token with read:packages scope')
param ghcrToken string

@secure()
@description('Neon Postgres JDBC URL for the app_owner (read-write) role, e.g. jdbc:postgresql://<host>/sqlgenie?sslmode=require')
param dbUrl string

@secure()
param dbUsername string

@secure()
param dbPassword string

@secure()
@description('Same Neon instance, JDBC URL for the readonly_query_user role')
param readonlyDbUrl string

@secure()
param readonlyDbUsername string

@secure()
param readonlyDbPassword string

@secure()
@description('HS256 signing secret for JWTs - at least 256 bits')
param jwtSecret string

@secure()
param groqApiKey string

@description('The frontend Static Web App URL, for CORS')
param corsAllowedOrigin string

resource sharedEnvironment 'Microsoft.App/managedEnvironments@2023-05-01' existing = {
  name: sharedEnvironmentName
}

resource backendApp 'Microsoft.App/containerApps@2023-05-01' = {
  name: '${appName}-backend'
  location: location
  properties: {
    managedEnvironmentId: sharedEnvironment.id
    configuration: {
      ingress: {
        external: true
        targetPort: 8080
        transport: 'http'
        allowInsecure: false
      }
      registries: [
        {
          server: 'ghcr.io'
          username: ghcrUsername
          passwordSecretRef: 'ghcr-token'
        }
      ]
      secrets: [
        { name: 'ghcr-token', value: ghcrToken }
        { name: 'db-password', value: dbPassword }
        { name: 'readonly-db-password', value: readonlyDbPassword }
        { name: 'jwt-secret', value: jwtSecret }
        { name: 'groq-api-key', value: groqApiKey }
      ]
    }
    template: {
      containers: [
        {
          name: 'backend'
          image: backendImage
          resources: {
            cpu: json('0.25')
            memory: '0.5Gi'
          }
          env: [
            { name: 'SPRING_PROFILES_ACTIVE', value: 'prod' }
            { name: 'SPRING_DATASOURCE_URL', value: dbUrl }
            { name: 'SPRING_DATASOURCE_USERNAME', value: dbUsername }
            { name: 'SPRING_DATASOURCE_PASSWORD', secretRef: 'db-password' }
            { name: 'APP_DATASOURCE_READONLY_URL', value: readonlyDbUrl }
            { name: 'APP_DATASOURCE_READONLY_USERNAME', value: readonlyDbUsername }
            { name: 'APP_DATASOURCE_READONLY_PASSWORD', secretRef: 'readonly-db-password' }
            { name: 'APP_JWT_SECRET', secretRef: 'jwt-secret' }
            { name: 'GROQ_API_KEY', secretRef: 'groq-api-key' }
            { name: 'APP_CORS_ALLOWED_ORIGINS', value: corsAllowedOrigin }
          ]
          probes: [
            {
              type: 'Liveness'
              httpGet: {
                path: '/actuator/health'
                port: 8080
              }
              initialDelaySeconds: 20
              periodSeconds: 10
            }
          ]
        }
      ]
      scale: {
        // Scale-to-zero is deliberate, not a default left alone: it's what
        // keeps this within the free monthly grant regardless of how long
        // the deployment sits idle between demos. Trade-off is a cold start
        // (several seconds) on the first request after idle.
        minReplicas: 0
        maxReplicas: 2
      }
    }
  }
}

output backendUrl string = 'https://${backendApp.properties.configuration.ingress.fqdn}'
