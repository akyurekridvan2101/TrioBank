package internal

import (
	"net/smtp"
)

func SendMail(channel chan error, senderMail string, senderPassword string, smtpHost string, smtpPort string, receiver Receiver) {
	content := "Merhaba,\n\nHesabınıza giris yapmak icin kullanacaginiz tek kullanimlik dogrulama kodunuz asagidadir:\n\n" + receiver.Code + "\n\nLütfen bu kodu 3 dakika icerisinde kullanin.\n\nIyi gunler dileriz,\nTRIO Bank Destek Ekibi"

	auth := smtp.PlainAuth("", senderMail, senderPassword, smtpHost)
	err := smtp.SendMail(smtpHost+":"+smtpPort, auth, senderMail, []string{receiver.Receiver}, []byte(content))
	channel <- err
}
