package model

import "server/internal/model/data"

type CelestialObject struct {
	Name          string
	Type          string
	Constellation string
	DisplayName   string
	PixelX        *float64
	PixelY        *float64
}

func (o CelestialObject) GetDisplayName() string {
	if o.DisplayName != "" {
		return o.DisplayName
	}
	return o.Name
}

type SolveResult struct {
	Objects []CelestialObject
}

func GetCelestialObject(name string) (*CelestialObject, bool) {
	info, ok := data.GetObjectInfo(name)
	if !ok {
		return nil, false
	}

	return &CelestialObject{
		Name:          info.Name,
		Type:          info.Type,
		Constellation: info.Constellation,
		DisplayName:   info.DisplayName,
	}, true
}
