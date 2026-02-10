package kv

import (
	"context"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"
)

const baseURL = "https://api.cloudflare.com/client/v4/accounts"

type Client struct {
	httpClient  *http.Client
	accountID   string
	namespaceID string
	apiToken    string
}

func NewClient(accountID, namespaceID, apiToken string) *Client {
	return &Client{
		httpClient:  &http.Client{Timeout: 10 * time.Second},
		accountID:   accountID,
		namespaceID: namespaceID,
		apiToken:    apiToken,
	}
}

func (c *Client) Get(ctx context.Context, key string) (string, bool, error) {
	url := fmt.Sprintf("%s/%s/storage/kv/namespaces/%s/values/%s", baseURL, c.accountID, c.namespaceID, key)
	req, err := http.NewRequestWithContext(ctx, "GET", url, nil)
	if err != nil {
		return "", false, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Authorization", "Bearer "+c.apiToken)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return "", false, fmt.Errorf("request failed: %w", err)
	}

	defer resp.Body.Close()

	if resp.StatusCode == http.StatusNotFound {
		return "", false, nil
	}

	if resp.StatusCode != http.StatusOK {
		return "", false, fmt.Errorf("API returned status %d", resp.StatusCode)
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", false, fmt.Errorf("failed to read response: %w", err)
	}
	return string(body), true, nil
}

func (c *Client) Put(ctx context.Context, key, value string) error {
	url := fmt.Sprintf("%s/%s/storage/kv/namespaces/%s/values/%s", baseURL, c.accountID, c.namespaceID, key)
	req, err := http.NewRequestWithContext(ctx, "PUT", url, strings.NewReader(value))
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Authorization", "Bearer "+c.apiToken)
	req.Header.Set("Content-Type", "text/plain")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("request failed: %w", err)
	}

	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("API returned status %d", resp.StatusCode)
	}
	return nil
}
