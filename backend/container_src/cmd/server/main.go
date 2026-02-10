package main

import (
	"context"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/go-chi/chi/v5"

	"server/internal/client/astrometry"
	"server/internal/client/gemini"
	"server/internal/client/kv"
	"server/internal/config"
	"server/internal/controller"
	"server/internal/service/object"
	"server/internal/service/solve"
)

func main() {
	cfg := config.Load()
	if cfg.AstrometryAPIKey == "" {
		log.Fatal("ASTROMETRY_API_KEY environment variable is required")
	}

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)

	astrometryClient := astrometry.NewClient(cfg.AstrometryAPIKey)
	kvClient := kv.NewClient(cfg.CloudflareAccountID, cfg.CloudflareNamespaceID, cfg.CloudflareAPIToken)
	geminiClient := gemini.NewClient(cfg.GeminiAPIKey)
	solveService := solve.NewService(astrometryClient)
	objectService := object.NewService(kvClient, geminiClient)
	solveController := controller.NewSolveController(solveService)
	objectController := controller.NewObjectController(objectService)

	router := chi.NewRouter()
	router.Get("/", func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte("StarSeek API"))
	})
	router.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte("ok"))
	})
	router.Post("/api/solve", solveController.SubmitImage)
	router.Get("/api/solve/{jobId}", solveController.GetSolveStatus)
	router.Get("/api/object/{name}", objectController.GetObjectDetail)

	server := &http.Server{
		Addr:         ":" + cfg.Port,
		Handler:      router,
		ReadTimeout:  30 * time.Second,
		WriteTimeout: 60 * time.Second,
	}

	go func() {
		log.Printf("Server listening on %s", server.Addr)
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			log.Fatal(err)
		}
	}()

	sig := <-stop
	log.Printf("Received signal (%s), shutting down server...", sig)

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := server.Shutdown(ctx); err != nil {
		log.Fatal(err)
	}

	log.Println("Server shutdown successfully")
}
