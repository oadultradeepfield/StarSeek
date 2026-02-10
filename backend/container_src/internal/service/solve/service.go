package solve

import (
	"context"
	"errors"

	"server/internal/client/astrometry"
	"server/internal/model"
)

const (
	StatusProcessing = "processing"
	StatusSuccess    = "success"
	StatusFailed     = "failed"
)

type AstrometryClient interface {
	GetSession(ctx context.Context) (string, error)
	Upload(ctx context.Context, session string, imageData []byte, filename string) (int, error)
	GetSubmission(ctx context.Context, subID int) (*astrometry.SubmissionResponse, error)
	GetJob(ctx context.Context, jobID int) (*astrometry.JobResponse, error)
	GetAnnotations(ctx context.Context, jobID int) ([]astrometry.Annotation, error)
}

type Service struct {
	client AstrometryClient
}

func NewService(client AstrometryClient) *Service {
	return &Service{client: client}
}

func (s *Service) SubmitImage(ctx context.Context, imageData []byte, filename string) (int, error) {
	session, err := s.client.GetSession(ctx)
	if err != nil {
		return 0, err
	}
	return s.client.Upload(ctx, session, imageData, filename)
}

type JobStatus struct {
	Status string
	Result *model.SolveResult
	Error  string
}

func (s *Service) GetJobStatus(ctx context.Context, subID int) (*JobStatus, error) {
	submission, err := s.client.GetSubmission(ctx, subID)
	if err != nil {
		if errors.Is(err, astrometry.ErrNotFound) {
			return nil, nil
		}
		return nil, err
	}

	if len(submission.Jobs) == 0 || submission.Jobs[0] == 0 {
		return &JobStatus{Status: StatusProcessing}, nil
	}

	actualJobID := submission.Jobs[0]
	job, err := s.client.GetJob(ctx, actualJobID)
	if err != nil {
		if errors.Is(err, astrometry.ErrNotFound) {
			return &JobStatus{Status: StatusProcessing}, nil
		}
		return nil, err
	}

	switch job.Status {
	case "success":
		annotations, err := s.client.GetAnnotations(ctx, actualJobID)
		if err != nil {
			return nil, err
		}

		return &JobStatus{
			Status: StatusSuccess,
			Result: TransformAnnotations(annotations, job.ObjectsInField, actualJobID),
		}, nil
	case "failure":
		return &JobStatus{
			Status: StatusFailed,
			Error:  "Could not identify objects in image",
		}, nil
	default:
		return &JobStatus{Status: StatusProcessing}, nil
	}
}
