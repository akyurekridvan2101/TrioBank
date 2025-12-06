package internal

import "errors"

var (
	ErrCodeIsNotCorrect = errors.New("verification code is incorrect")
	ErrCodeNotFound     = errors.New("session not found")

	ErrUserAlreadyExist = errors.New("the user already exist")
)
