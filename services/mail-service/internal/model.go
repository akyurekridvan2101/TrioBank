package internal

import "github.com/resend/resend-go/v3"

type Receiver struct {
	Receiver string `json:"receiver"`
	Code     string `json:"code"`
}

type Client struct {
	MailClient *resend.Client
}
