{{/*
================================================================================
Common Helper Functions - Paylaşımlı Yardımcı Fonksiyonlar
================================================================================

AMAÇ:
  Tüm service'lerde tekrar eden kod parçalarını merkezileştirmek.
  Label'lar, isimler gibi standart değerleri tek yerden yönetmek.

NE ZAMAN KULLANILIR:
  • Birden fazla template'de aynı kod tekrarlanıyorsa
  • Standart Kubernetes label'ları için
  • DNS adresleri, connection string'ler gibi ortak değerler için

FARK: Service-Specific vs Common Helpers
  • {{ include "ledger-service.fullname" . }} → Sadece ledger-service'de
  • {{ include "common.fullname" . }}         → Tüm service'lerde

NOT:
  Her service kendi templates/_helpers.tpl dosyasına sahiptir (Helm best practice).
  Bu dosya ise tüm service'ler tarafından PAYLAŞILAN helper'ları içerir.

================================================================================
*/}}

{{/*
--------------------------------------------------------------------------------
Helper: common.name
--------------------------------------------------------------------------------
AMAÇ: Chart'ın kısa adını döner

KULLANIM:
  {{ include "common.name" . }}

ÇIKTI:
  ledger-service

KULLANIM YERİ:
  • Label'larda (app.kubernetes.io/name)
  • Log mesajlarında
  • Resource annotation'larında

NOT:
  values.yaml'da nameOverride varsa onu kullanır, yoksa Chart adı.
--------------------------------------------------------------------------------
*/}}
{{- define "common.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
--------------------------------------------------------------------------------
Helper: common.fullname
--------------------------------------------------------------------------------
AMAÇ: Benzersiz resource adı oluşturur (release name + chart name)

KULLANIM:
  {{ include "common.fullname" . }}

ÇIKTI:
  my-release-ledger-service

NEDEN KULLANILIR:
  Aynı chart'ı birden fazla kere deploy ettiğinde resource isimleri çakışmasın.
  Örnek: 
    - staging-ledger-service
    - production-ledger-service

KULLANIM YERİ:
  • Deployment, Service, Secret isimleri
  • ExternalSecret adı

NOT:
  • values.yaml'da fullnameOverride varsa onu kullanır
  • Aksi halde: release-name + chart-name
  • Maksimum 63 karakter (Kubernetes limiti)
--------------------------------------------------------------------------------
*/}}
{{- define "common.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
--------------------------------------------------------------------------------
Helper: common.chart
--------------------------------------------------------------------------------
AMAÇ: Chart adı ve versiyonunu birleştirir (label için)

KULLANIM:
  {{ include "common.chart" . }}

ÇIKTI:
  ledger-service-0.1.0

KULLANIM YERİ:
  • helm.sh/chart label'ı için
  • Versiyonlama ve tracking

NOT:
  "+" karakteri "_" ile değiştirilir (Kubernetes label kuralı)
--------------------------------------------------------------------------------
*/}}
{{- define "common.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
--------------------------------------------------------------------------------
Helper: common.labels
--------------------------------------------------------------------------------
AMAÇ: Standart Kubernetes label'larını oluşturur

KULLANIM:
  labels:
    {{- include "common.labels" . | nindent 4 }}

ÇIKTI:
  helm.sh/chart: ledger-service-0.1.0
  app.kubernetes.io/name: ledger-service
  app.kubernetes.io/instance: my-release
  app.kubernetes.io/version: "1.0.0"
  app.kubernetes.io/managed-by: Helm
  app.kubernetes.io/part-of: triobank

NEDEN ÖNEMLİ:
  • Kubernetes best practice (recommended labels)
  • kubectl ile filtreleme: kubectl get pods -l app.kubernetes.io/name=ledger-service
  • Monitoring ve logging için grouping
  • Service mesh integration

KULLANIM YERİ:
  • Deployment, Service, ConfigMap, Secret
  • Tüm Kubernetes resource'larda

REFERANS:
  https://kubernetes.io/docs/concepts/overview/working-with-objects/common-labels/
--------------------------------------------------------------------------------
*/}}
{{- define "common.labels" -}}
helm.sh/chart: {{ include "common.chart" . }}
{{ include "common.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: triobank
{{- end }}

{{/*
--------------------------------------------------------------------------------
Helper: common.selectorLabels
--------------------------------------------------------------------------------
AMAÇ: Pod selector için minimal label seti (deployment → pod matching)

KULLANIM:
  selector:
    matchLabels:
      {{- include "common.selectorLabels" . | nindent 6 }}

ÇIKTI:
  app.kubernetes.io/name: ledger-service
  app.kubernetes.io/instance: my-release

NEDEN AYRI:
  Deployment selector IMMUTABLE (değiştirilemez). Bu yüzden minimal tutulmalı.
  Version gibi değişken label'lar selector'da olmamalı.

FARK: common.labels vs common.selectorLabels
  • common.labels: Tüm label'lar (version, chart vs.) → metadata.labels
  • common.selectorLabels: Sadece sabit label'lar → spec.selector.matchLabels

KULLANIM YERİ:
  • Deployment spec.selector.matchLabels
  • Service spec.selector
--------------------------------------------------------------------------------
*/}}
{{- define "common.selectorLabels" -}}
app.kubernetes.io/name: {{ include "common.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
--------------------------------------------------------------------------------
Helper: common.kafka-bootstrap
--------------------------------------------------------------------------------
AMAÇ: Kafka cluster'ın bootstrap server adresini döner

KULLANIM:
  {{ include "common.kafka-bootstrap" . }}

ÇIKTI:
  triobank-cluster-kafka-bootstrap.infrastructure.svc.cluster.local:9092

NEDEN KULLANILIR:
  Kafka adresi tek yerden yönetilir. Değişirse sadece values.yaml güncellenir.

KULLANIM YERİ:
  • Kafka Connector (schema history için)
  • Producer/Consumer config
  • Debezium connector

OVERRIDE:
  values.yaml'da tanımlanabilir:
    kafka:
      brokers: "custom-kafka:9092"

NOT:
  triobank namespace'deki Kafka cluster'a işaret eder.
--------------------------------------------------------------------------------
*/}}
{{- define "common.kafka-bootstrap" -}}
{{- default "triobank-cluster-kafka-bootstrap.triobank.svc.cluster.local:9092" .Values.kafka.brokers }}
{{- end }}
