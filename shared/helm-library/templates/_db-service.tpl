{{/*
================================================================================
Database Service Template - ExternalName Service Abstraction
================================================================================

AÇIKLAMA:
  Kubernetes cluster DIŞINDA çalışan database'lere (MSSQL, MongoDB, Redis, PostgreSQL vs)
  cluster içinden erişim için DNS abstraction sağlar.

KULLANIM:
  Service chart'ında db-service.yaml oluştur:
  
  ```yaml
  {{- include "common.database-services" . }}
  ```

NE YAPAR:
  ExternalName Service'ler oluşturur. Pod'lar dış database'lere sanki 
  cluster içindeymiş gibi bağlanır.

DNS ÇALIŞMA PRENSİBİ:
  Pod içinden:        ledger-mssql.triobank.svc.cluster.local:1433
                      ↓
  Kubernetes DNS:     ExternalName Service (CNAME)
                      ↓
  Gerçek Endpoint:    host.docker.internal (local)
                      prod-mssql.azure.com (production)

CONFIGURATION (values.yaml):

  database:
    enabled: true
    serviceName: "ledger-mssql"
    port: 1433
    type: "mssql"
    externalName: "host.docker.internal"
  
  databases:
    mssql:
      enabled: true
      serviceName: "ledger-mssql"
      port: 1433
      type: "mssql"
      externalName: "prod-mssql.database.windows.net"
      annotations:
        description: "Primary transactional database"
    
    mongodb:
      enabled: true
      serviceName: "ledger-mongo"
      port: 27017
      type: "mongodb"
      externalName: "prod-mongo.cosmos.azure.com"
    
    redis:
      enabled: true
      serviceName: "ledger-redis"
      port: 6379
      type: "redis"
      externalName: "prod-redis.cache.windows.net"

DATABASE TYPES (Auto Port/PortName Detection):
  Type        Default Port    PortName
  ────────────────────────────────────
  mssql       1433           mssql
  postgresql  5432           postgresql
  mongodb     27017          mongodb
  mysql       3306           mysql
  redis       6379           redis
  postgres    5432           postgresql

ENVIRONMENT EXAMPLES:

  Local Development:
    externalName: host.docker.internal (Docker Desktop)
    externalName: host.minikube.internal (Minikube)
  
  Azure Cloud:
    externalName: mydb.database.windows.net (Azure SQL)
    externalName: mydb.cosmos.azure.com (Cosmos DB)
    externalName: myredis.redis.cache.windows.net (Azure Cache)
  
  AWS Cloud:
    externalName: mydb.xxxxx.us-east-1.rds.amazonaws.com (RDS)
    externalName: myredis.xxxxx.cache.amazonaws.com (ElastiCache)
  
  On-Premise:
    externalName: db.company.internal
    externalName: 192.168.1.100 (IP address)

BEST PRACTICES:
  ✓ Never hardcode credentials in values
  ✓ Use Vault/ExternalSecrets for secrets
  ✓ Set proper firewall rules for external DB access
  ✓ Use SSL/TLS for production databases
  ✓ Monitor connection pools and latency

================================================================================
*/}}

{{/*
================================================================================
Helper: Database Type Defaults
Returns default port and portName for common database types
================================================================================
*/}}
{{- define "common.db-defaults" -}}
{{- $type := . -}}
{{- if eq $type "mssql" }}
port: 1433
portName: mssql
{{- else if or (eq $type "postgresql") (eq $type "postgres") }}
port: 5432
portName: postgresql
{{- else if eq $type "mongodb" }}
port: 27017
portName: mongodb
{{- else if eq $type "mysql" }}
port: 3306
portName: mysql
{{- else if eq $type "redis" }}
port: 6379
portName: redis
{{- else }}
port: 0
portName: unknown
{{- end }}
{{- end -}}

{{/*
================================================================================
Main Template: Database Services (Multiple Databases Support)
Creates ExternalName services for all enabled databases
================================================================================
*/}}
{{- define "common.database-services" -}}

{{- /* Single database mode (backward compatibility) */ -}}
{{- if .Values.database -}}
{{- if .Values.database.enabled -}}
{{- include "common.db-service-single" (dict "config" .Values.database "root" .) -}}
{{- end -}}
{{- end -}}

{{- /* Multiple databases mode (recommended) */ -}}
{{- if .Values.databases -}}
{{- range $name, $config := .Values.databases -}}
{{- if $config.enabled -}}
{{ include "common.db-service-single" (dict "config" $config "root" $ "name" $name) }}
---
{{- end -}}
{{- end -}}
{{- end -}}

{{- end -}}

{{/*
================================================================================
Single Database Service Template
Creates one ExternalName Service for a database
================================================================================
*/}}
{{- define "common.db-service-single" -}}
{{- $config := .config -}}
{{- $root := .root -}}
{{- $name := .name | default "database" -}}

{{- /* Determine port and names based on type */ -}}
{{- $port := 1433 -}}
{{- $portName := "mssql" -}}

{{- if eq ($config.type | default "mssql") "postgresql" -}}
  {{- $port = 5432 -}}
  {{- $portName = "postgresql" -}}
{{- else if eq ($config.type | default "mssql") "mongodb" -}}
  {{- $port = 27017 -}}
  {{- $portName = "mongodb" -}}
{{- else if eq ($config.type | default "mssql") "redis" -}}
  {{- $port = 6379 -}}
  {{- $portName = "redis" -}}
{{- end -}}

{{- /* Override if provided */ -}}
{{- if $config.port -}}{{- $port = $config.port -}}{{- end -}}
{{- if $config.portName -}}{{- $portName = $config.portName -}}{{- end -}}

apiVersion: v1
kind: Service
metadata:
  name: {{ $config.serviceName }}
  namespace: {{ $root.Release.Namespace }}
  labels:
    helm.sh/chart: {{ include "common.chart" $root }}
    app.kubernetes.io/name: {{ include "common.name" $root }}
    app.kubernetes.io/instance: {{ $root.Release.Name }}
    app.kubernetes.io/managed-by: {{ $root.Release.Service }}
    app.kubernetes.io/component: database
    triobank.com/database-type: {{ $config.type | default "mssql" }}
  {{- with $config.annotations }}
  annotations:
    {{- toYaml . | nindent 4 }}
  {{- end }}
spec:
  type: ExternalName
  externalName: {{ required "externalName is required" $config.externalName }}
  ports:
  - port: {{ $port | int }}
    targetPort: {{ $port | int }}
    protocol: TCP
    name: {{ $portName }}
{{- end -}}
