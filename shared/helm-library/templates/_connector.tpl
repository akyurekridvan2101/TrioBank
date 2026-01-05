{{/*
==============================================================================
TrioBank - Debezium CDC Connector Template
SQL Server Change Data Capture with Outbox Pattern
==============================================================================

KULLANIM:
  Service chart'ında connector.yaml oluştur:
  {{- if .Values.connector.enabled }}
  {{ include "common.connector" . }}
  {{- end }}

AMAÇ:
  Outbox tablosundaki değişiklikleri CDC ile yakalar ve Kafka topic'lerine event olarak gönderir.

GEREKSINIMLER:
  - Database'de CDC aktif olmalı (sp_cdc_enable_db)
  - Outbox tablosu CDC için aktif olmalı (sp_cdc_enable_table)
  - Secret'lar infrastructure namespace'de hazır olmalı
  - Kafka Connect cluster hazır olmalı

VALUES EXAMPLE:
  connector:
    enabled: true
    name: "ledger-cdc-connector"
    secretVolumeName: "mssql-credentials"
    
    database:
      hostname: "ledger-mssql.triobank"
      port: 1433
      names: "ledger_db"
      serverName: "ledger"
      encrypt: "false"
      trustServerCertificate: "true"
    
    table:
      include: "dbo.outbox_events"
    
    topic:
      prefix: "ledger"
    
    outbox:
      routeByField: "aggregate_type"
      topicReplacement: "ledger.${routedByValue}.v1"
      fields:
        id: "id"
        key: "aggregate_id"
        type: "type"
        payload: "payload"
        timestamp: "created_at"
    
    performance:
      pollInterval: "500"
      maxBatchSize: "2048"

==============================================================================
*/}}

{{- define "common.connector" -}}
{{- if .Values.connector.enabled }}

apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaConnector
metadata:
  name: {{ .Values.connector.name }}
  namespace: triobank
  labels:
    {{- include "common.labels" . | nindent 4 }}
    app.kubernetes.io/component: cdc-connector
    triobank.com/source-service: {{ include "common.name" . }}
    strimzi.io/cluster: connect
spec:
  class: io.debezium.connector.sqlserver.SqlServerConnector
  tasksMax: 1
  config:
    # Database connection
    database.hostname: {{ .Values.connector.database.hostname }}
    database.port: {{ .Values.connector.database.port | quote }}
    database.names: {{ .Values.connector.database.names | quote }}
    database.user: ${directory:/mnt/secrets/{{ .Values.connector.secretVolumeName }}:username}
    database.password: ${directory:/mnt/secrets/{{ .Values.connector.secretVolumeName }}:password}
    database.encrypt: {{ .Values.connector.database.encrypt | default "false" | quote }}
    database.trustServerCertificate: {{ .Values.connector.database.trustServerCertificate | default "true" | quote }}
    
    # Table and topic configuration
    table.include.list: {{ .Values.connector.table.include | quote }}
    topic.prefix: {{ .Values.connector.topic.prefix }}
    database.server.name: {{ .Values.connector.database.serverName | default .Values.connector.topic.prefix }}
    decimal.handling.mode: {{ .Values.connector.database.decimalHandling | default "string" | quote }}
    snapshot.mode: {{ .Values.connector.snapshot.mode | default "initial" }}
    
    # Schema history
    schema.history.internal.kafka.bootstrap.servers: {{ include "common.kafka-bootstrap" . }}
    schema.history.internal.kafka.topic: {{ printf "%s.schema-changes" .Values.connector.topic.prefix }}
    
    # Outbox transform configuration
    transforms: "outbox"
    transforms.outbox.type: "io.debezium.transforms.outbox.EventRouter"
    transforms.outbox.route.by.field: {{ .Values.connector.outbox.routeByField | default "aggregate_type" | quote }}
    transforms.outbox.route.topic.replacement: {{ .Values.connector.outbox.topicReplacement | default (printf "%s.${routedByValue}.v1" .Values.connector.topic.prefix) | quote }}
    transforms.outbox.table.field.event.id: {{ .Values.connector.outbox.fields.id | default "id" | quote }}
    transforms.outbox.table.field.event.key: {{ .Values.connector.outbox.fields.key | default "aggregate_id" | quote }}
    transforms.outbox.table.field.event.type: {{ .Values.connector.outbox.fields.type | default "type" | quote }}
    transforms.outbox.table.field.event.payload: {{ .Values.connector.outbox.fields.payload | default "payload" | quote }}
    transforms.outbox.table.field.event.timestamp: {{ .Values.connector.outbox.fields.timestamp | default "created_at" | quote }}
    transforms.outbox.route.tombstone.on.empty.payload: {{ .Values.connector.outbox.tombstoneOnEmpty | default "false" | quote }}
    transforms.outbox.table.expand.json.payload: {{ .Values.connector.outbox.expandJsonPayload | default "true" | quote }}
    
    # Performance tuning
    poll.interval.ms: {{ .Values.connector.performance.pollInterval | default "500" | quote }}
    max.batch.size: {{ .Values.connector.performance.maxBatchSize | default "2048" | quote }}
    
    # Error handling
    errors.tolerance: {{ .Values.connector.errors.tolerance | default "all" }}
    errors.log.enable: {{ .Values.connector.errors.logEnable | default "true" | quote }}
    errors.log.include.messages: {{ .Values.connector.errors.logMessages | default "true" | quote }}
    
    # Heartbeat
    heartbeat.interval.ms: {{ .Values.connector.heartbeat.interval | default "60000" | quote }}
    heartbeat.topics.prefix: {{ .Values.connector.heartbeat.topicPrefix | default "__debezium-heartbeat" }}
    
    # Tombstones
    tombstones.on.delete: {{ .Values.connector.tombstones | default "true" | quote }}
    
    {{- with .Values.connector.additionalConfig }}
    {{- toYaml . | nindent 4 }}
    {{- end }}
{{- end }}
{{- end }}
