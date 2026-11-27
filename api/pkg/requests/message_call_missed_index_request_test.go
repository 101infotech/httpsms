package requests

import (
	"testing"

	"github.com/NdoleStudio/httpsms/pkg/entities"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestMessageCallMissedIndexSanitizeDefaults(t *testing.T) {
	input := (&MessageCallMissedIndex{}).Sanitize()

	assert.Equal(t, "50", input.Limit)
	assert.Equal(t, "0", input.Skip)
	assert.Empty(t, input.Owners)

	params := input.ToSearchParams(entities.UserID("user-id"))
	require.NotNil(t, params)
	assert.Equal(t, 50, params.Limit)
	assert.Equal(t, 0, params.Skip)
	assert.Equal(t, "created_at", params.SortBy)
	assert.True(t, params.SortDescending)
	assert.Equal(t, []entities.MessageType{entities.MessageTypeCallMissed}, params.Types)
	assert.Empty(t, params.Owners)
}

func TestMessageCallMissedIndexSanitizeOwners(t *testing.T) {
	input := (&MessageCallMissedIndex{
		Owners: []string{" +18005550199 ", "", "+18005550100"},
		Skip:   " 10 ",
		Limit:  " 25 ",
	}).Sanitize()

	assert.Equal(t, []string{"+18005550199", "+18005550100"}, input.Owners)

	params := input.ToSearchParams(entities.UserID("user-id"))
	assert.Equal(t, []string{"+18005550199", "+18005550100"}, params.Owners)
	assert.Equal(t, 10, params.Skip)
	assert.Equal(t, 25, params.Limit)
	assert.Equal(t, []entities.MessageType{entities.MessageTypeCallMissed}, params.Types)
}
