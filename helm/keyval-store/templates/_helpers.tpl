{{/*
Expand the name of the chart.
*/}}
{{- define "keyval-store.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "keyval-store.fullname" -}}
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
Create chart name and version as used by the chart label.
*/}}
{{- define "keyval-store.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "keyval-store.labels" -}}
helm.sh/chart: {{ include "keyval-store.chart" . }}
{{ include "keyval-store.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "keyval-store.selectorLabels" -}}
app.kubernetes.io/name: {{ include "keyval-store.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "keyval-store.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "keyval-store.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the headless service name
*/}}
{{- define "keyval-store.headlessServiceName" -}}
{{- printf "%s-headless" (include "keyval-store.fullname" .) }}
{{- end }}

{{/*
Create the seed nodes list for cluster formation
*/}}
{{- define "keyval-store.seedNodes" -}}
{{- $fullname := include "keyval-store.fullname" . -}}
{{- $headlessService := include "keyval-store.headlessServiceName" . -}}
{{- $namespace := .Release.Namespace -}}
{{- $replicaCount := int .Values.replicaCount -}}
{{- range $i := until $replicaCount -}}
{{- if gt $i 0 -}},{{- end -}}
{{- printf "%s-%d.%s.%s.svc.cluster.local:8080" $fullname $i $headlessService $namespace -}}
{{- end -}}
{{- end }}
