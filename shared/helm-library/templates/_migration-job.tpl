{{/*
==============================================================================
TrioBank - Database Migration Job Template
Kubernetes Job for automated database schema migrations
==============================================================================

KULLANIM:
  Service templates/ klasöründe migration-job.yaml oluştur:
  {{- include "common.migrationJob" . }}

AMAÇ:
  Database migration'larını Helm hooks kullanarak otomatik çalıştırır.
  Helm install/upgrade öncesi migration Job'ı tetiklenir.

NE ZAMAN ÇALIŞIR:
  • helm install → Migration Job (pre-install hook)
  • helm upgrade → Migration Job (pre-upgrade hook)

VALUES EXAMPLE:
  migration:
    enabled: true
    image: "service-migrate:latest"
    imagePullPolicy: "IfNotPresent"
    jdbcParams: "encrypt=false;trustServerCertificate=true"
    backoffLimit: 3
    ttlSecondsAfterFinished: 3600
    resources:
      limits:
        cpu: "200m"
        memory: "128Mi"
      requests:
        cpu: "50m"
        memory: "64Mi"
  
  database:
    serviceName: "service-mssql"
    port: 1433
    name: "service_db"
  
  secret:
    name: "service-db-credentials"
    usernameKey: "username"  # optional, defaults to "username"
    passwordKey: "password"  # optional, defaults to "password"

==============================================================================
*/}}

{{- define "common.migrationJob" -}}
{{- if .Values.migration.enabled }}
---
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ include (printf "%s.fullname" .Chart.Name) . }}-migration-{{ .Release.Revision }}
  namespace: {{ .Release.Namespace }}
  labels:
    {{- include (printf "%s.labels" .Chart.Name) . | nindent 4 }}
    app.kubernetes.io/component: database-migration
  annotations:
    helm.sh/hook-delete-policy: before-hook-creation
spec:
  backoffLimit: {{ .Values.migration.backoffLimit | default 3 }}
  ttlSecondsAfterFinished: {{ .Values.migration.ttlSecondsAfterFinished | default 3600 }}
  
  template:
    metadata:
      labels:
        app.kubernetes.io/name: {{ include (printf "%s.name" .Chart.Name) . }}
        app.kubernetes.io/component: database-migration
        app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
    spec:
      restartPolicy: Never

      {{- if .Values.migration.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml .Values.migration.imagePullSecrets | nindent 8 }}
      {{- end }}
      
      # Create database if not exists
      initContainers:
      - name: create-db
        image: mcr.microsoft.com/mssql-tools
        imagePullPolicy: IfNotPresent
        command:
        - /bin/bash
        - -c
        - |
          /opt/mssql-tools/bin/sqlcmd -S $(DB_HOST),$(DB_PORT) -U $(DB_USERNAME) -P $(DB_PASSWORD) -Q "IF NOT EXISTS(SELECT * FROM sys.databases WHERE name = '$(DB_NAME)') BEGIN CREATE DATABASE [$(DB_NAME)]; END"
        env:
        - name: DB_HOST
          value: {{ .Values.database.serviceName }}
        - name: DB_PORT
          value: {{ .Values.database.port | quote }}
        - name: DB_NAME
          value: {{ .Values.database.name }}
        {{- $secretName := "" -}}
        {{- $usernameKey := "username" -}}
        {{- $passwordKey := "password" -}}
        {{- if .Values.secret -}}
          {{- $secretName = .Values.secret.name -}}
          {{- $usernameKey = .Values.secret.usernameKey | default "username" -}}
          {{- $passwordKey = .Values.secret.passwordKey | default "password" -}}
        {{- else if .Values.externalSecrets -}}
          {{- $secretName = (index .Values.externalSecrets 0).targetSecret -}}
        {{- end }}
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: {{ $secretName }}
              key: {{ $usernameKey }}
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: {{ $secretName }}
              key: {{ $passwordKey }}

      # Run migrations
      containers:
      - name: db-migration
        image: {{ .Values.migration.image }}
        imagePullPolicy: {{ .Values.migration.imagePullPolicy | default "IfNotPresent" }}
        
        command:
        - migrate
        args:
        - -source
        - file:///migrations
        - -database
        - {{ printf "sqlserver://$(DB_USERNAME):$(DB_PASSWORD)@%s:%d?database=%s&%s" 
            .Values.database.serviceName 
            (.Values.database.port | int)
            .Values.database.name 
            (.Values.migration.jdbcParams | default "encrypt=false;trustServerCertificate=true") }}
        - up
        
        {{- $secretName := "" -}}
        {{- $usernameKey := "username" -}}
        {{- $passwordKey := "password" -}}
        {{- if .Values.secret -}}
          {{- $secretName = .Values.secret.name -}}
          {{- $usernameKey = .Values.secret.usernameKey | default "username" -}}
          {{- $passwordKey = .Values.secret.passwordKey | default "password" -}}
        {{- else if .Values.externalSecrets -}}
          {{- $secretName = (index .Values.externalSecrets 0).targetSecret -}}
        {{- end }}
        
        env:
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: {{ $secretName }}
              key: {{ $usernameKey }}
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: {{ $secretName }}
              key: {{ $passwordKey }}
        
        resources:
          limits:
            cpu: {{ .Values.migration.resources.limits.cpu | default "200m" }}
            memory: {{ .Values.migration.resources.limits.memory | default "128Mi" }}
          requests:
            cpu: {{ .Values.migration.resources.requests.cpu | default "50m" }}
            memory: {{ .Values.migration.resources.requests.memory | default "64Mi" }}
{{- end }}
{{- end }}
