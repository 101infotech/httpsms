package requests

import (
	"strings"

	"github.com/NdoleStudio/httpsms/pkg/entities"

	"github.com/NdoleStudio/httpsms/pkg/repositories"

	"github.com/NdoleStudio/httpsms/pkg/services"
)

// MessageCallMissedIndex is the payload fetching missed calls of a user
type MessageCallMissedIndex struct {
	request
	Owners []string `json:"owners" query:"owners"`
	Skip   string   `json:"skip" query:"skip"`
	Limit  string   `json:"limit" query:"limit"`
}

// Sanitize sets defaults to MessageCallMissedIndex
func (input *MessageCallMissedIndex) Sanitize() MessageCallMissedIndex {
	input.Owners = input.removeEmptyStrings(input.sanitizeAddresses(input.Owners))

	input.Skip = strings.TrimSpace(input.Skip)
	if input.Skip == "" {
		input.Skip = "0"
	}

	input.Limit = strings.TrimSpace(input.Limit)
	if input.Limit == "" {
		input.Limit = "50"
	}

	return *input
}

// ToSearchParams converts request to services.MessageSearchParams. An empty
// owners list searches all the phones of the user.
func (input *MessageCallMissedIndex) ToSearchParams(userID entities.UserID) *services.MessageSearchParams {
	return &services.MessageSearchParams{
		IndexParams: repositories.IndexParams{
			Skip:           input.getInt(input.Skip),
			SortBy:         "created_at",
			SortDescending: true,
			Limit:          input.getInt(input.Limit),
		},
		UserID: userID,
		Owners: input.Owners,
		Types:  []entities.MessageType{entities.MessageTypeCallMissed},
	}
}
