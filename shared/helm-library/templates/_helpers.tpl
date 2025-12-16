{{/*
==============================================================================
TrioBank - Common Helper Functions
Tüm servislerde kullanılan ortak Helm template helper fonksiyonları
==============================================================================
*/}}

{{/*
Chart adını döner (values.yaml'daki nameOverride ile override edilebilir)
Usage: {{ include "common.name" . }}
*/}}
{{- define "common.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Benzersiz resource adı oluşturur (release + chart name)
Usage: {{ include "common.fullname" . }}
Output: my-release-ledger-service
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
Chart adı ve versiyonunu birleştirir
Usage: {{ include "common.chart" . }}
Output: ledger-service-0.1.0
*/}}
{{- define "common.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Standart Kubernetes label'larını oluşturur
Kubernetes recommended labels: https://kubernetes.io/docs/concepts/overview/working-with-objects/common-labels/
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
Pod selector için minimal label seti
Deployment selector IMMUTABLE olduğu için version gibi değişken label'lar içermez
*/}}
{{- define "common.selectorLabels" -}}
app.kubernetes.io/name: {{ include "common.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "common.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "common.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Kafka cluster bootstrap server adresi
values.yaml'da kafka.brokers ile override edilebilir
Default: triobank namespace'deki Kafka cluster
*/}}
{{- define "common.kafka-bootstrap" -}}
{{- default "kafka-kafka-bootstrap.triobank.svc.cluster.local:9092" .Values.kafka.brokers }}
{{- end }}
