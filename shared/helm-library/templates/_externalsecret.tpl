{{/*
==============================================================================
TrioBank - ExternalSecret Template
Vault secret sync
==============================================================================

KULLANIM:
  Service chart'ında externalsecret.yaml oluştur:
  {{- include "common.external-secrets" . }}

VALUES EXAMPLE:
  externalSecrets:
    - name: db-credentials
      enabled: true
      vaultPath: secret/dev/services/auth
      targetSecret: auth-db-credentials
      refreshInterval: "1h"
      fields:
        - secretKey: MONGO_USERNAME
          property: mongo_username
        - secretKey: MONGO_PASSWORD
          property: mongo_password

==============================================================================
*/}}

{{- define "common.external-secrets" -}}
{{- if .Values.externalSecrets }}
{{- range .Values.externalSecrets }}
{{- if .enabled }}
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: {{ include "common.fullname" $ }}-{{ .name }}
  namespace: {{ $.Release.Namespace }}
  labels:
    {{- include "common.labels" $ | nindent 4 }}
    triobank.com/secret-type: {{ .name }}
spec:
  refreshInterval: {{ .refreshInterval | default "1h" }}
  secretStoreRef:
    name: vault-backend
    kind: ClusterSecretStore
  target:
    name: {{ .targetSecret }}
    creationPolicy: Owner
  data:
  {{- range .fields }}
  - secretKey: {{ .secretKey }}
    remoteRef:
      key: {{ $.vaultPath }}
      property: {{ .property }}
  {{- end }}
---
{{- end }}
{{- end }}
{{- end }}
{{- end -}}

