package solve

import (
	"strconv"
	"strings"

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

		var info data.ObjectInfo
		var known bool

		for _, rawName := range ann.Names {
			for _, part := range strings.Split(rawName, "/") {
				name := strings.TrimSpace(part)
				if info, known = data.GetObjectInfo(name); known {
					break
				}
			}
			if known {
				break
			}
		}

		if !known || seen[info.Name] {
			continue
		}

		seen[info.Name] = true
		obj := model.CelestialObject{
			Name:          info.Name,
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
