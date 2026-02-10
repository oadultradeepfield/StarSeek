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
	DisplayName   string
}

func (o ObjectInfo) GetDisplayName() string {
	if o.DisplayName != "" {
		return o.DisplayName
	}
	return o.Name
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
		if len(parts) < 3 {
			continue
		}

		info := ObjectInfo{
			Name:          parts[0],
			Constellation: parts[1],
			Type:          parts[2],
		}
		if len(parts) >= 4 {
			info.DisplayName = parts[3]
		}
		catalog[strings.ToLower(parts[0])] = info
	}
}

func GetObjectInfo(name string) (ObjectInfo, bool) {
	info, ok := catalog[strings.ToLower(name)]
	return info, ok
}
