{{/*
================================================================================
Database Migration Job Template - Shared Reusable Component
================================================================================

AÇIKLAMA:
  Database migration'ları için reusable Kubernetes Job template'i.
  Helm hooks kullanarak install/upgrade öncesi otomatik çalıştırır.

KULLANIM:
  Service'in templates/ klasöründe bir migration-job.yaml oluştur:
  
  ```yaml
  {{- include "common.migrationJob" . }}
  ```

GEREKLILIKLER:
  1. Service klasöründe migrations/ dizini olmalı
  2. values.yaml'da migration.enabled ve migration.image tanımlı olmalı
  3. Vault'ta database credentials olmalı (secret.name referansı ile)

NE ZAMAN ÇALIŞIR:
  • helm install → Migration Job çalışır (pre-install hook)
  • helm upgrade → Migration Job çalışır (pre-upgrade hook)

AVANTAJLAR:
  ✓ Deployment'tan bağımsız (standalone Job pattern)
  ✓ Otomatik retry mekanizması (backoffLimit)
  ✓ Helm hooks ile otomatik tetikleme
  ✓ Otomatik cleanup (ttlSecondsAfterFinished)
  ✓ Migration history takibi (.Release.Revision ile versiyonlama)

CONFIGURATION (values.yaml):
  ```yaml
  migration:
    enabled: true
    image: "service-migrate:latest"
    imagePullPolicy: "IfNotPresent"
    jdbcParams: "encrypt=false;trustServerCertificate=true"
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
  ```

================================================================================
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

      containers:
      - name: db-migration
        image: {{ .Values.migration.image }}
        imagePullPolicy: {{ .Values.migration.imagePullPolicy | default "IfNotPresent" }}
        
        command:
        - migrate
        args:
        - -source
        - file:///migrations                    # Migration dosyaları image içinde
        - -database
        - {{ printf "sqlserver://$(DB_USERNAME):$(DB_PASSWORD)@%s:%d?database=%s&%s" 
            .Values.database.serviceName 
            (.Values.database.port | int)
            .Values.database.name 
            (.Values.migration.jdbcParams | default "encrypt=false;trustServerCertificate=true") }}
        - up                                    # Tüm pending migration'ları çalıştır
        
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
