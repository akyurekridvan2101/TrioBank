package internal

import (
	"context"
	"os"

	"github.com/resend/resend-go/v3"
)

func SendMail(ctx context.Context, r Receiver) error {
	client := resend.NewClient(os.Getenv("RESEND_API_KEY"))

	body := `
<h2>Doğrulama Kodunuz</h2>
<p>Giriş işlemini tamamlamak için aşağıdaki doğrulama kodunu kullanın:</p>
<h1 style="letter-spacing: 4px;">` + r.Code + `</h1>
<p>Bu kod 3 dakika boyunca geçerlidir.</p>
`

	req := &resend.SendEmailRequest{
		From:    "Trio Bank <onboarding@triobank.com>",
		To:      []string{r.Receiver},
		Subject: "Doğrulama Kodunuz",
		Html:    body,
	}

	_, err := client.Emails.SendWithContext(ctx, req)
	if err != nil {
		return err
	}
	return nil
}
