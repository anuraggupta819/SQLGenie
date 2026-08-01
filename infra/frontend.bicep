// SQLGenie frontend hosting: a Static Web App on the perpetual Free SKU.
// Deployed into sqlgenie-rg. Split out from the backend template because the
// two now target different resource groups - see infra/backend.bicep for why.

@description('Azure region for the Static Web App - a much shorter allow-list than most resource types (e.g. centralus, eastus2, westus2, westeurope, eastasia)')
param location string = 'centralus'

@description('Base name used to derive resource names')
param appName string = 'sqlgenie'

resource staticWebApp 'Microsoft.Web/staticSites@2022-09-01' = {
  name: '${appName}-frontend'
  location: location
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

output frontendUrl string = 'https://${staticWebApp.properties.defaultHostname}'
