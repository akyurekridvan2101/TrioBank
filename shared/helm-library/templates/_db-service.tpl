{{/*
==============================================================================
TrioBank - Database Service Template
ExternalName Service for external databases (MSSQL, PostgreSQL, MongoDB, Redis)
==============================================================================

KULLANIM:
  Service chart'ında db-service.yaml oluştur:
  {{- include "common.database-services" . }}

AMAÇ:
  Kubernetes cluster dışındaki database'lere cluster içinden erişim sağlar.
  DNS abstraction ile pod'lar sanki database cluster içindeymiş gibi bağlanır.

VALUES EXAMPLE:
  database:
    enabled: true
    serviceName: "ledger-mssql"
    externalName: "host.docker.internal"  # Local dev
    port: 1433
    type: "mssql"

  databases:                               # Multiple databases
    mssql:
      enabled: true
      serviceName: "ledger-mssql"
      externalName: "prod-mssql.database.windows.net"
      port: 1433
      type: "mssql"
    redis:
      enabled: true
      serviceName: "ledger-redis"
      externalName: "prod-redis.cache.windows.net"
      port: 6379
      type: "redis"

DATABASE TYPES (auto-detected defaults):
  mssql      -> port: 1433
  postgresql -> port: 5432
  mongodb    -> port: 27017
  mysql      -> port: 3306
  redis      -> port: 6379

==============================================================================
*/}}

{{/*
Helper: Database type defaults
Returns default port and portName for common database types
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
Main Template: Database Services
Creates ExternalName services for all enabled databases
Supports both single database mode and multiple databases mode
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
Single Database Service Template
Creates one ExternalName Service for a database
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
