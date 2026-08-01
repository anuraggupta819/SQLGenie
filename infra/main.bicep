// SQLGenie deployment, designed to stay within Azure's perpetual free tiers:
// - Container Apps: real, ongoing free monthly grant (not a 12-month trial),
//   easily enough for a low-traffic demo when the app scales to zero when idle.
// - Static Web Apps: genuine perpetual Free SKU.
// - No Key Vault (Container Apps' own built-in secrets are used instead -
//   skips even Key Vault's small per-operation cost; fine for one app,
//   would reconsider for multiple services sharing secrets).
// - No Azure Container Registry (GHCR is free for public images).
// - No Azure-managed Postgres (Flexible Server has no perpetual free SKU) -
//   the database is external (Neon), the backend just connects to it.
//
// This subscription is a Free Trial, which caps Container Apps Environments
// at 1 *per subscription*, not per region - a hard anti-abuse limit that
// isn't liftable via a normal quota-increase request on this subscription
// tier. Rather than create a second environment (which the subscription
// would reject) or move the database/JWT-signing project boundary around to
// dodge it, this template joins the existing shared environment from
// another project on the same subscription. Each app inside a Container
// Apps Environment still gets its own container, scaling, secrets, and
// revision history - only the environment (the compute/network boundary,
// and its one Log Analytics workspace) is shared. This template therefore
// creates no Log Analytics workspace or managed environment of its own.

@description('Azure region for the backend Container App')
param location string = resourceGroup().location

@description('Azure region for the Static Web App - a much shorter allow-list than most resource types (e.g. centralus, eastus2, westus2, westeurope, eastasia), so it is deliberately independent of "location" rather than assumed to match it')
param staticWebAppLocation string = 'centralus'

@description('Resource group of the pre-existing Container Apps Environment this app joins (shared across projects due to the Free Trial subscription\'s 1-environment-per-subscription cap)')
param sharedEnvironmentResourceGroup string = 'cartify-rg'

@description('Name of the pre-existing Container Apps Environment this app joins')
param sharedEnvironmentName string = 'cartify-env'

@description('Base name used to derive resource names')
param appName string = 'sqlgenie'

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

resource staticWebApp 'Microsoft.Web/staticSites@2022-09-01' = {
  name: '${appName}-frontend'
  location: staticWebAppLocation
  sku: {
    name: 'Free'
    tier: 'Free'
  }
  properties: {
    // Content is deployed separately via GitHub Actions (Azure/static-web-apps-deploy),
    // not provisioned by this template - this just creates the hosting resource.
    buildProperties: {
      skipGithubActionWorkflowGeneration: true
    }
  }
}

resource backendApp 'Microsoft.App/containerApps@2023-05-01' = {
  name: '${appName}-backend'
  location: location
  properties: {
    managedEnvironmentId: resourceId(sharedEnvironmentResourceGroup, 'Microsoft.App/managedEnvironments', sharedEnvironmentName)
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
            { name: 'APP_CORS_ALLOWED_ORIGINS', value: 'https://${staticWebApp.properties.defaultHostname}' }
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
output frontendUrl string = 'https://${staticWebApp.properties.defaultHostname}'

// Deliberately no output for the Static Web App's deployment token - Bicep
// outputs land in plaintext in deployment history. The deploy workflow
// fetches it separately via `az staticwebapp secrets list` after this
// template has run, not through this template's outputs.
