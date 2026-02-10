package config

import "os"

type Config struct {
	Port                  string
	AstrometryAPIKey      string
	GeminiAPIKey          string
	CloudflareAccountID   string
	CloudflareNamespaceID string
	CloudflareAPIToken    string
}

func Load() *Config {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	return &Config{
		Port:                  port,
		AstrometryAPIKey:      os.Getenv("ASTROMETRY_API_KEY"),
		GeminiAPIKey:          os.Getenv("GEMINI_API_KEY"),
		CloudflareAccountID:   os.Getenv("CLOUDFLARE_ACCOUNT_ID"),
		CloudflareNamespaceID: os.Getenv("CLOUDFLARE_NAMESPACE_ID"),
		CloudflareAPIToken:    os.Getenv("CLOUDFLARE_API_TOKEN"),
	}
}
