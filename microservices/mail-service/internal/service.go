package internal

import (
	"context"
	"crypto/tls"
	"net/smtp"
	"os"
)

func NewSmtpPool(port, host string, auth smtp.Auth, size int) (*SmtpPool, error) {
	pool := make(chan *smtp.Client, size)

	smtpPool := SmtpPool{
		Connections: pool,
		smtpPort:    port,
		smtpHost:    host,
		auth:        auth,
	}

	for i := 0; i < size; i++ {
		client, err := smtp.Dial(host + ":" + port)
		if err != nil {
			smtpPool.Close()
			return nil, err
		}
		err = client.StartTLS(&tls.Config{
			ServerName: os.Getenv("SMTP_HOST"),
			MinVersion: tls.VersionTLS12,
		})
		if err != nil {
			smtpPool.Close()
			return nil, err
		}

		err = client.Auth(auth)
		if err != nil {
			smtpPool.Close()
			return nil, err
		}

		smtpPool.Connections <- client
	}
	return &smtpPool, nil
}
func (s *SmtpPool) Close() {
	close(s.Connections)
	for client := range s.Connections {
		_ = client.Quit()
		_ = client.Close()
	}
}
func (s *SmtpPool) NewConn(client *smtp.Client) (*smtp.Client, error) {
	_ = client.Quit()
	_ = client.Close()
	newConn, err := smtp.Dial(s.smtpHost + ":" + s.smtpPort)
	if err != nil {
		return nil, err
	}
	err = newConn.Auth(s.auth)
	if err != nil {
		return nil, err
	}
	s.Connections <- newConn
	return newConn, nil
}
func (s *SmtpPool) Acquire(ctx context.Context) (*smtp.Client, error) {
	select {
	case <-ctx.Done():
		return nil, ctx.Err()
	case x := <-s.Connections:
		if err := x.Noop(); err != nil {
			return s.NewConn(x)
		}
		return x, nil
	}
}
func (s *SmtpPool) Release(client *smtp.Client) {
	s.Connections <- client
}
func SendMail(ctx context.Context, channel chan error, s *SmtpPool, receiver Receiver) {
	client, err := s.Acquire(ctx)
	flag := false
	if err != nil {
		channel <- err
		return
	}
	defer func() {
		if flag == true {
			_, _ = s.NewConn(client)
		} else {
			s.Release(client)
		}

	}()
	err = client.Mail(os.Getenv("SENDER_MAIL"))
	if err != nil {
		channel <- err
		flag = true
		return
	}
	err = client.Rcpt(receiver.Receiver)
	if err != nil {
		channel <- err
		flag = true
		return
	}
	writer, err := client.Data()
	if err != nil {
		channel <- err
		return
	}
	defer writer.Close()
	data := "From: " + os.Getenv("SENDER_MAIL") + "\nTo: " + receiver.Receiver + "\nSubject: [ TRIO BANK ] Giris Dogrulama Kodunuz\nContent-Type: text/plain; charset=\"UTF-8\"\n\n\n\nMerhaba,\n\nHesabınıza giriş yapmak için kullanacagınız tek kullanımlık doğrulama kodunuz asağıdadır:\n\nKod:" + receiver.Code + "\n\nLütfen bu kodu 3 dakika icerisinde kullanin.\n\nIyi gunler dileriz,\nTrio Bank Destek Ekibi"
	_, err = writer.Write([]byte(data))
	if err != nil {
		channel <- err
		return
	}
	channel <- nil
}
