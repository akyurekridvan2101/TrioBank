package internal

import "errors"

var (
	ErrCodeIsNotCorrect = errors.New("verification code is incorrect")
	ErrCodeNotFound     = errors.New("session not found")

	ErrUserAlreadyExist = errors.New("the user already exist")

	ErrTokenIsNotExist  = errors.New("the token is not found")
	ErrTokenIsNotActive = errors.New("the token is not active")
	ErrTokenExpired     = errors.New("the token expired")

	ErrOldPasswordInvalid = errors.New("old password not right")
)
