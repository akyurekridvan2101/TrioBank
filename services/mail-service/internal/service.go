package internal

import (
	"context"

	"github.com/resend/resend-go/v3"
)

func SendMail(ctx context.Context, c Client, r Receiver) error {
	body := `
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" />
  <title>Doğrulama Kodunuz</title>
</head>
<body style="font-family: Arial, sans-serif; background-color: #eef6ff; padding: 20px; margin: 0;">
  <div style="max-width: 500px; margin: auto; background: #ffffff; padding: 28px; border-radius: 10px; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">

    <!-- Logo -->
    <div style="text-align: center; margin-bottom: 20px;">
      <div style="font-size: 26px; font-weight: 900; color: #4da3ff;">
        Trio Bank
      </div>
    </div>

    <h2 style="color: #333; text-align: center; margin-top: 0;">
      Giriş Doğrulama Kodunuz
    </h2>

    <p style="font-size: 15px; color: #555; text-align: center;">
      Giriş işlemini tamamlamak için aşağıdaki doğrulama kodunu kullanın.
    </p>

    <div style="margin: 26px 0; text-align: center;">
      <div style="
        display: inline-block;
        font-size: 34px;
        font-weight: bold;
        letter-spacing: 8px;
        background: #e3f1ff;
        padding: 14px 28px;
        border-radius: 8px;
        color: #1c5fa8;
        border: 1px solid #c8e4ff;
      ">
        ` + r.Code + `
      </div>
    </div>

    <p style="font-size: 14px; color: #666; text-align: center;">
      Bu kod <strong>3 dakika</strong> boyunca geçerlidir.
    </p>

    <hr style="border: none; border-top: 1px solid #e5e5e5; margin: 30px 0;" />

    <p style="font-size: 12px; color: #999; text-align: center; line-height: 1.5;">
      Bu işlem Trio Bank tarafından başlatıldı. Siz gerçekleştirmediyseniz
      lütfen güvenlik ekibimizle iletişime geçin.
    </p>

  </div>
</body>
</html>

`

	req := &resend.SendEmailRequest{
		From:    "Trio Bank <onboarding@triobank.org>",
		To:      []string{r.Receiver},
		Subject: "Doğrulama Kodunuz",
		Html:    body,
	}

	_, err := c.MailClient.Emails.SendWithContext(ctx, req)
	if err != nil {
		return err
	}
	return nil
}
