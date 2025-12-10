package internal

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"

	"github.com/TrioBank/triobank-platform/microservices/auth-service/config"
)

func SendMail(ctx context.Context, repo Repo, requestData smsRequestData, errChannel chan error) {
	url := "http://" + config.GetEnv("MAIL_SERVICE_PORT") + "/send"
	data, err := json.Marshal(requestData)
	if err != nil {
		errChannel <- err
		return
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, url, bytes.NewReader(data))
	if err != nil {
		errChannel <- err
		return
	}
	deadline, _ := ctx.Deadline()

	req.Header.Set("X-Request-Deadline", strconv.FormatInt(deadline.UnixMilli(), 10))
	req.Header.Set("X-Internal-Secret", config.GetEnv("SECRET_KEY"))

	response, err := repo.Client.Do(req)
	if err != nil {
		errChannel <- err
		return
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		errChannel <- fmt.Errorf("unexpected status: %d", response.StatusCode)
		return
	} else {
		errChannel <- nil
		return
	}
}
