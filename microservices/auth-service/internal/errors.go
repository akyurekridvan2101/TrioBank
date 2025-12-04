package internal

import "errors"

var (
	ErrCodeIsNotCorrect = errors.New("verification code is incorrect")
	ErrCodeNotFound     = errors.New("session not found")
)
