package requests

import "strings"

// ContactGroupRename renames a contact group label.
type ContactGroupRename struct {
	request
	Name string `json:"name" example:"Students"`
}

// Sanitize trims the new group name.
func (input ContactGroupRename) Sanitize() ContactGroupRename {
	input.Name = strings.TrimSpace(input.Name)
	return input
}
