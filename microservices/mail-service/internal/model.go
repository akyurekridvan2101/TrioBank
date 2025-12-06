package internal

import "net/smtp"

type Receiver struct {
	Receiver string `json:"receiver"`
	Code     string `json:"code"`
}

type SmtpPool struct {
	Connections chan *smtp.Client
	smtpPort    string
	smtpHost    string
	auth        smtp.Auth
}
