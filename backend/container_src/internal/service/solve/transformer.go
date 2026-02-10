package solve

import (
	"strconv"

	"server/internal/client/astrometry"
	"server/internal/model"
	"server/internal/model/data"
)

func TransformAnnotations(annotations []astrometry.Annotation, jobID int) *model.SolveResult {
	var objects []model.CelestialObject
	seen := make(map[string]bool)

	for _, ann := range annotations {
		if len(ann.Names) == 0 {
			continue
		}

		name := ann.Names[0]
		if seen[name] {
			continue
		}

		seen[name] = true
		info, known := data.GetObjectInfo(name)
		if !known {
			continue
		}

		obj := model.CelestialObject{
			Name:          name,
			Type:          info.Type,
			Constellation: info.Constellation,
		}

		if info.Type == "star" && ann.PixelX != 0 && ann.PixelY != 0 {
			x, y := ann.PixelX, ann.PixelY
			obj.PixelX = &x
			obj.PixelY = &y
		}

		objects = append(objects, obj)
	}

	return &model.SolveResult{
		AnnotatedImageURL: "https://nova.astrometry.net/annotated_full/" + strconv.Itoa(jobID),
		Objects:           objects,
	}
}
