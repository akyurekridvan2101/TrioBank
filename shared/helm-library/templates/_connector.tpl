{{/*
================================================================================
Debezium CDC Connector Template - SQL Server Outbox Pattern
================================================================================

AMAÇ:
  Outbox tablosundaki değişiklikleri Change Data Capture (CDC) ile yakalar,
  Kafka topic'lerine event olarak gönderir.

KULLANIM:
  1. Service chart'ında connector.yaml oluştur:
     {{- if .Values.connector.enabled }}
     {{ include "common.connector" . }}
     {{- end }}

  2. values.yaml'da gerekli tüm değerleri tanımla (örnek aşağıda)

GEREKLİ VALUES (values.yaml):
  connector:
    enabled: true                          # Connector'ı aktif et
    name: "ledger-cdc-connector"           # Connector adı
    secretVolumeName: "mssql-credentials"  # DB credential secret'ı
    
    database:
      hostname: "ledger-mssql.triobank"        # DB host
      port: 1433                           # DB port
      names: "ledger_db"                   # DB adı
      serverName: "ledger"                 # Server logical name (default: topic.prefix)
      encrypt: "false"                     # SSL (local: false)
      decimalHandling: "string"            # Decimal format: string, double, precise
      trustServerCertificate: "true"       # SSL sertifika kontrolü
    
    table:
      include: "dbo.outbox_events"         # İzlenecek tablo
    
    topic:
      prefix: "ledger"                     # Kafka topic prefix
    
    snapshot:
      mode: "initial"                      # İlk çalıştırmada snapshot al
    
    outbox:
      routeByField: "aggregate_type"       # Routing için hangi kolon
      topicReplacement: "triobank.local.ledger.${routedByValue}.v1"
      fields:
        id: "id"                           # Event ID kolonu
        key: "aggregate_id"                # Kafka key kolonu
        type: "type"                       # Event type kolonu
        payload: "payload"                 # Event payload kolonu
        timestamp: "created_at"            # Timestamp kolonu
      tombstoneOnEmpty: "false"            # Boş payload = tombstone?
      expandJsonPayload: "true"            # JSON payload'ı genişlet
    
    performance:
      pollInterval: "500"                  # Polling aralığı (ms)
      maxBatchSize: "2048"                 # Batch boyutu
    
    errors:
      tolerance: "all"                     # Hata toleransı
      logEnable: "true"                    # Hataları logla
      logMessages: "true"                  # Hata mesajlarını logla
    
    heartbeat:
      interval: "60000"                    # Heartbeat aralığı (ms)
      topicPrefix: "__debezium-heartbeat"  # Heartbeat topic prefix

ÇIKTI:
  KafkaConnector resource (infrastructure namespace'de oluşur)
  
ÖNEMLİ:
  - Secret'lar infrastructure namespace'de hazır olmalı
  - Database'de CDC aktif olmalı (sp_cdc_enable_db)
  - Outbox tablosu CDC için aktif olmalı (sp_cdc_enable_table)
  - Kafka Connect cluster hazır olmalı (strimzi.io/cluster: triobank-connect)

================================================================================
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
    strimzi.io/cluster: triobank-connect
spec:
  class: io.debezium.connector.sqlserver.SqlServerConnector
  tasksMax: 1
  config:
    database.hostname: {{ .Values.connector.database.hostname }}
    database.port: {{ .Values.connector.database.port | quote }}
    database.names: {{ .Values.connector.database.names | quote }}
    database.user: ${directory:/opt/kafka/external-configuration/{{ .Values.connector.secretVolumeName }}:username}
    database.password: ${directory:/opt/kafka/external-configuration/{{ .Values.connector.secretVolumeName }}:password}
    
    database.encrypt: {{ .Values.connector.database.encrypt | default "false" | quote }}
    database.trustServerCertificate: {{ .Values.connector.database.trustServerCertificate | default "true" | quote }}
    
    table.include.list: {{ .Values.connector.table.include | quote }}
    topic.prefix: {{ .Values.connector.topic.prefix }}
    database.server.name: {{ .Values.connector.database.serverName | default .Values.connector.topic.prefix }}
    
    decimal.handling.mode: {{ .Values.connector.database.decimalHandling | default "string" | quote }}
    
    snapshot.mode: {{ .Values.connector.snapshot.mode | default "initial" }}
    
    schema.history.internal.kafka.bootstrap.servers: {{ include "common.kafka-bootstrap" . }}
    schema.history.internal.kafka.topic: {{ printf "%s.schema-changes" .Values.connector.topic.prefix }}
    
    transforms: "outbox"
    transforms.outbox.type: "io.debezium.transforms.outbox.EventRouter"
    
    transforms.outbox.route.by.field: {{ .Values.connector.outbox.routeByField | default "aggregate_type" | quote }}
    transforms.outbox.route.topic.replacement: {{ .Values.connector.outbox.topicReplacement | default (printf "triobank.local.%s.${routedByValue}.v1" .Values.connector.topic.prefix) | quote }}
    
    transforms.outbox.table.field.event.id: {{ .Values.connector.outbox.fields.id | default "id" | quote }}
    transforms.outbox.table.field.event.key: {{ .Values.connector.outbox.fields.key | default "aggregate_id" | quote }}
    transforms.outbox.table.field.event.type: {{ .Values.connector.outbox.fields.type | default "type" | quote }}
    transforms.outbox.table.field.event.payload: {{ .Values.connector.outbox.fields.payload | default "payload" | quote }}
    transforms.outbox.table.field.event.timestamp: {{ .Values.connector.outbox.fields.timestamp | default "created_at" | quote }}
    
    transforms.outbox.route.tombstone.on.empty.payload: {{ .Values.connector.outbox.tombstoneOnEmpty | default "false" | quote }}
    transforms.outbox.table.expand.json.payload: {{ .Values.connector.outbox.expandJsonPayload | default "true" | quote }}
    
    poll.interval.ms: {{ .Values.connector.performance.pollInterval | default "500" | quote }}
    max.batch.size: {{ .Values.connector.performance.maxBatchSize | default "2048" | quote }}
    
    errors.tolerance: {{ .Values.connector.errors.tolerance | default "all" }}
    errors.log.enable: {{ .Values.connector.errors.logEnable | default "true" | quote }}
    errors.log.include.messages: {{ .Values.connector.errors.logMessages | default "true" | quote }}
    
    heartbeat.interval.ms: {{ .Values.connector.heartbeat.interval | default "60000" | quote }}
    heartbeat.topics.prefix: {{ .Values.connector.heartbeat.topicPrefix | default "__debezium-heartbeat" }}
    
    tombstones.on.delete: {{ .Values.connector.tombstones | default "true" | quote }}
    
    {{- with .Values.connector.additionalConfig }}
    {{- toYaml . | nindent 4 }}
    {{- end }}
{{- end }}
{{- end }}
