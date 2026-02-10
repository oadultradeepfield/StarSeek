package config

import "os"

type Config struct {
	Port             string
	AstrometryAPIKey string
}

func Load() *Config {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	return &Config{
		Port:             port,
		AstrometryAPIKey: os.Getenv("ASTROMETRY_API_KEY"),
	}
}
