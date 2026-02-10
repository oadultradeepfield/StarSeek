package data

import (
	_ "embed"
	"strings"
)

//go:embed catalog.txt
var catalogData string

type ObjectInfo struct {
	Name          string
	Constellation string
	Type          string
}

var catalog map[string]ObjectInfo

func init() {
	catalog = make(map[string]ObjectInfo)

	for _, line := range strings.Split(catalogData, "\n") {
		line = strings.TrimSpace(line)
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}

		parts := strings.Split(line, "|")
		if len(parts) != 3 {
			continue
		}

		catalog[strings.ToLower(parts[0])] = ObjectInfo{
			Name:          parts[0],
			Constellation: parts[1],
			Type:          parts[2],
		}
	}
}

func GetObjectInfo(name string) (ObjectInfo, bool) {
	info, ok := catalog[strings.ToLower(name)]
	return info, ok
}
