package astrometry

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"net/url"
	"sync"
	"time"
)

const baseURL = "https://nova.astrometry.net/api"

var ErrNotFound = errors.New("resource not found")

type Client struct {
	httpClient *http.Client
	apiKey     string
	session    string
	sessionExp time.Time
	mu         sync.Mutex
}

func NewClient(apiKey string) *Client {
	return &Client{
		httpClient: &http.Client{Timeout: 60 * time.Second},
		apiKey:     apiKey,
	}
}

type LoginResponse struct {
	Status  string `json:"status"`
	Message string `json:"message"`
	Session string `json:"session"`
}

type UploadResponse struct {
	Status string `json:"status"`
	SubID  int    `json:"subid"`
	Hash   string `json:"hash"`
}

type SubmissionResponse struct {
	ProcessingStarted  string  `json:"processing_started"`
	ProcessingFinished string  `json:"processing_finished"`
	JobCalibrations    [][]int `json:"job_calibrations"`
	Jobs               []int   `json:"jobs"`
}

type JobResponse struct {
	Status string `json:"status"`
}

type Annotation struct {
	Type   string   `json:"type"`
	Names  []string `json:"names"`
	PixelX float64  `json:"pixelx"`
	PixelY float64  `json:"pixely"`
	Radius float64  `json:"radius"`
}

type AnnotationsResponse struct {
	Annotations []Annotation `json:"annotations"`
}

func (c *Client) Login(ctx context.Context) (string, error) {
	data := url.Values{}
	data.Set("request-json", fmt.Sprintf(`{"apikey":"%s"}`, c.apiKey))
	req, err := http.NewRequestWithContext(ctx, "POST", baseURL+"/login", bytes.NewBufferString(data.Encode()))
	if err != nil {
		return "", fmt.Errorf("failed to create login request: %w", err)
	}

	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return "", fmt.Errorf("login request failed: %w", err)
	}

	defer resp.Body.Close()

	var result LoginResponse
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return "", fmt.Errorf("failed to decode login response: %w", err)
	}

	if result.Status != "success" {
		return "", fmt.Errorf("login failed: %s", result.Message)
	}
	return result.Session, nil
}

func (c *Client) GetSession(ctx context.Context) (string, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.session != "" && time.Now().Before(c.sessionExp) {
		return c.session, nil
	}

	session, err := c.Login(ctx)
	if err != nil {
		return "", err
	}

	c.session = session
	c.sessionExp = time.Now().Add(30 * time.Minute)
	return session, nil
}

func (c *Client) Upload(ctx context.Context, session string, imageData []byte, filename string) (int, error) {
	var buf bytes.Buffer
	writer := multipart.NewWriter(&buf)
	requestJSON := fmt.Sprintf(`{"session":"%s","allow_commercial_use":"n","allow_modifications":"n"}`, session)
	if err := writer.WriteField("request-json", requestJSON); err != nil {
		return 0, fmt.Errorf("failed to write request-json field: %w", err)
	}

	part, err := writer.CreateFormFile("file", filename)
	if err != nil {
		return 0, fmt.Errorf("failed to create form file: %w", err)
	}

	if _, err := part.Write(imageData); err != nil {
		return 0, fmt.Errorf("failed to write image data: %w", err)
	}

	if err := writer.Close(); err != nil {
		return 0, fmt.Errorf("failed to close multipart writer: %w", err)
	}

	req, err := http.NewRequestWithContext(ctx, "POST", baseURL+"/upload", &buf)
	if err != nil {
		return 0, fmt.Errorf("failed to create upload request: %w", err)
	}

	req.Header.Set("Content-Type", writer.FormDataContentType())
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return 0, fmt.Errorf("upload request failed: %w", err)
	}

	defer resp.Body.Close()

	var result UploadResponse
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return 0, fmt.Errorf("failed to decode upload response: %w", err)
	}

	if result.Status != "success" {
		return 0, fmt.Errorf("upload failed with status: %s", result.Status)
	}
	return result.SubID, nil
}

func (c *Client) GetSubmission(ctx context.Context, subID int) (*SubmissionResponse, error) {
	req, err := http.NewRequestWithContext(ctx, "GET", fmt.Sprintf("%s/submissions/%d", baseURL, subID), nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create get submission request: %w", err)
	}

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("get submission request failed: %w", err)
	}

	defer resp.Body.Close()

	if resp.StatusCode == http.StatusNotFound {
		return nil, ErrNotFound
	}

	var result SubmissionResponse
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("failed to decode submission response: %w", err)
	}
	return &result, nil
}

func (c *Client) GetJob(ctx context.Context, jobID int) (*JobResponse, error) {
	req, err := http.NewRequestWithContext(ctx, "GET", fmt.Sprintf("%s/jobs/%d", baseURL, jobID), nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create get job request: %w", err)
	}

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("get job request failed: %w", err)
	}

	defer resp.Body.Close()
	if resp.StatusCode == http.StatusNotFound {
		return nil, ErrNotFound
	}

	var result JobResponse
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("failed to decode job response: %w", err)
	}
	return &result, nil
}

func (c *Client) GetAnnotations(ctx context.Context, jobID int) ([]Annotation, error) {
	req, err := http.NewRequestWithContext(ctx, "GET", fmt.Sprintf("%s/jobs/%d/annotations", baseURL, jobID), nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create get annotations request: %w", err)
	}

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("get annotations request failed: %w", err)
	}

	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read annotations response: %w", err)
	}

	var result AnnotationsResponse
	if err := json.Unmarshal(body, &result); err != nil {
		var annotations []Annotation
		if err := json.Unmarshal(body, &annotations); err != nil {
			return nil, fmt.Errorf("failed to decode annotations response: %w", err)
		}
		return annotations, nil
	}
	return result.Annotations, nil
}
