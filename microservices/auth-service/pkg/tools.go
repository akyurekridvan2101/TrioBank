package pkg

import (
	"golang.org/x/crypto/bcrypt"
)

func HashPassword(password string) (string, error) {
	temp, err := bcrypt.GenerateFromPassword([]byte(password), 10)
	if err != nil {
		return "", err
	}
	return string(temp), err
}

func HashedPasswordControl(password string, hashedPassword string) error {
	err := bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password))
	if err != nil {
		return err
	} else {
		return nil
	}
}
