package model

import "server/internal/model/data"

type CelestialObject struct {
	Name          string
	Type          string
	Constellation string
	PixelX        *float64
	PixelY        *float64
}

type SolveResult struct {
	AnnotatedImageURL string
	Objects           []CelestialObject
}

func GetCelestialObject(name string) (*CelestialObject, bool) {
	info, ok := data.GetObjectInfo(name)
	if !ok {
		return nil, false
	}

	return &CelestialObject{
		Name:          name,
		Type:          info.Type,
		Constellation: info.Constellation,
	}, true
}
