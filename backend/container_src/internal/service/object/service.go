package object

import (
	"context"
	"log"

	"server/internal/model"
)

type KVClient interface {
	Get(ctx context.Context, key string) (string, bool, error)
	Put(ctx context.Context, key, value string) error
}

type GeminiClient interface {
	GenerateFunFact(ctx context.Context, objectName, objectType string) (string, error)
}

type ObjectDetail struct {
	Name          string
	Type          string
	Constellation string
	FunFact       string
}

type Service struct {
	kvClient     KVClient
	geminiClient GeminiClient
}

func NewService(kvClient KVClient, geminiClient GeminiClient) *Service {
	return &Service{kvClient: kvClient, geminiClient: geminiClient}
}

func (s *Service) GetObjectDetail(ctx context.Context, name string) (*ObjectDetail, error) {
	obj, ok := model.GetCelestialObject(name)
	if !ok {
		return nil, nil
	}

	funFact, err := s.getFunFact(ctx, obj.Name, obj.Type)
	if err != nil {
		return nil, err
	}

	return &ObjectDetail{
		Name:          obj.Name,
		Type:          obj.Type,
		Constellation: obj.Constellation,
		FunFact:       funFact,
	}, nil
}

func (s *Service) getFunFact(ctx context.Context, name, objectType string) (string, error) {
	cached, found, err := s.kvClient.Get(ctx, name)
	if err != nil {
		log.Printf("KV read failed for %s: %v", name, err)
	} else if found {
		return cached, nil
	}

	funFact, err := s.geminiClient.GenerateFunFact(ctx, name, objectType)
	if err != nil {
		return "", err
	}

	if err := s.kvClient.Put(ctx, name, funFact); err != nil {
		log.Printf("KV write failed for %s: %v", name, err)
	}
	return funFact, nil
}
