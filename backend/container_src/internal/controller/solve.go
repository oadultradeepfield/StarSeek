package controller

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"

	"server/internal/model"
	"server/internal/service/solve"
	"server/internal/view"
)

type SolveService interface {
	SubmitImage(ctx context.Context, imageData []byte, filename string) (int, error)
	GetJobStatus(ctx context.Context, subID int) (*solve.JobStatus, error)
}

type SolveController struct {
	service SolveService
}

func NewSolveController(service SolveService) *SolveController {
	return &SolveController{service: service}
}

func (c *SolveController) SubmitImage(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseMultipartForm(32 << 20); err != nil {
		writeError(w, http.StatusBadRequest, "Failed to parse multipart form")
		return
	}

	file, header, err := r.FormFile("image")
	if err != nil {
		writeError(w, http.StatusBadRequest, "No image provided")
		return
	}

	defer file.Close()

	imageData, err := io.ReadAll(file)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "Failed to read image")
		return
	}

	subID, err := c.service.SubmitImage(r.Context(), imageData, header.Filename)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "Failed to process image")
		return
	}

	writeJSON(w, http.StatusOK, view.SolveResponse{
		JobID:  strconv.Itoa(subID),
		Status: solve.StatusProcessing,
	})
}

func (c *SolveController) GetSolveStatus(w http.ResponseWriter, r *http.Request) {
	jobID := chi.URLParam(r, "jobId")
	if jobID == "" {
		writeError(w, http.StatusBadRequest, "Job ID required")
		return
	}

	subID, err := strconv.Atoi(jobID)
	if err != nil {
		writeError(w, http.StatusBadRequest, "Invalid job ID")
		return
	}

	status, err := c.service.GetJobStatus(r.Context(), subID)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "Failed to check job status")
		return
	}

	if status == nil {
		writeError(w, http.StatusNotFound, "Job not found")
		return
	}

	writeJSON(w, http.StatusOK, view.JobStatusResponse{
		Status: status.Status,
		Result: view.FromSolveResult(status.Result),
		Error:  status.Error,
	})
}

type ObjectController struct{}

func NewObjectController() *ObjectController {
	return &ObjectController{}
}

func (c *ObjectController) GetObjectDetail(w http.ResponseWriter, r *http.Request) {
	name := chi.URLParam(r, "name")
	if name == "" {
		writeError(w, http.StatusBadRequest, "Object name required")
		return
	}

	obj, ok := model.GetCelestialObject(name)
	if !ok {
		writeError(w, http.StatusNotFound, "Object not found")
		return
	}

	writeJSON(w, http.StatusOK, view.ObjectDetailResponse{
		Name:          obj.Name,
		Type:          obj.Type,
		Constellation: obj.Constellation,
	})
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(v)
}

func writeError(w http.ResponseWriter, status int, message string) {
	writeJSON(w, status, view.ErrorResponse{Error: message})
}
