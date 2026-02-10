package view

import "server/internal/model"

type SolveResponse struct {
	JobID  string `json:"jobId"`
	Status string `json:"status"`
}

type JobStatusResponse struct {
	Status string       `json:"status"`
	Result *SolveResult `json:"result,omitempty"`
	Error  string       `json:"error,omitempty"`
}

type SolveResult struct {
	AnnotatedImageURL string            `json:"annotatedImageUrl"`
	Objects           []CelestialObject `json:"objects"`
}

type CelestialObject struct {
	Name          string   `json:"name"`
	Type          string   `json:"type"`
	Constellation string   `json:"constellation"`
	PixelX        *float64 `json:"pixelX,omitempty"`
	PixelY        *float64 `json:"pixelY,omitempty"`
}

type ErrorResponse struct {
	Error string `json:"error"`
}

type ObjectDetailResponse struct {
	Name          string `json:"name"`
	Type          string `json:"type"`
	Constellation string `json:"constellation"`
}

func FromSolveResult(r *model.SolveResult) *SolveResult {
	if r == nil {
		return nil
	}

	objects := make([]CelestialObject, len(r.Objects))
	for i, o := range r.Objects {
		objects[i] = CelestialObject{
			Name:          o.Name,
			Type:          o.Type,
			Constellation: o.Constellation,
			PixelX:        o.PixelX,
			PixelY:        o.PixelY,
		}
	}

	return &SolveResult{
		AnnotatedImageURL: r.AnnotatedImageURL,
		Objects:           objects,
	}
}
