package requests

import (
	"encoding/json"
	"testing"

	"github.com/NdoleStudio/httpsms/pkg/entities"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestPhoneUpsertArchiveNewOutgoingMessageThreadsPresence(t *testing.T) {
	tests := []struct {
		name    string
		body    string
		present bool
		want    bool
	}{
		{name: "enabled", body: `{"phone_number":"+18005550199","archive_new_outgoing_message_threads":true}`, present: true, want: true},
		{name: "disabled", body: `{"phone_number":"+18005550199","archive_new_outgoing_message_threads":false}`, present: true, want: false},
		{name: "omitted by older client", body: `{"phone_number":"+18005550199"}`, present: false},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			var input PhoneUpsert
			require.NoError(t, json.Unmarshal([]byte(test.body), &input))

			params := input.ToUpsertParams(entities.AuthContext{}, "test", []byte(test.body))

			if !test.present {
				assert.Nil(t, params.ArchiveNewOutgoingMessageThreads)
				return
			}
			require.NotNil(t, params.ArchiveNewOutgoingMessageThreads)
			assert.Equal(t, test.want, *params.ArchiveNewOutgoingMessageThreads)
		})
	}
}

func TestPhoneUpsertName(t *testing.T) {
	empty := ""
	named := "SSIT front desk"
	tests := []struct {
		name string
		body string
		want *string
	}{
		{name: "omitted by older client", body: `{"phone_number":"+18005550199"}`},
		{name: "provided", body: `{"phone_number":"+18005550199","name":"SSIT front desk"}`, want: &named},
		{name: "trimmed", body: `{"phone_number":"+18005550199","name":"  SSIT front desk  "}`, want: &named},
		{name: "cleared with empty string", body: `{"phone_number":"+18005550199","name":""}`, want: &empty},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			var input PhoneUpsert
			require.NoError(t, json.Unmarshal([]byte(test.body), &input))
			input.Sanitize()

			params := input.ToUpsertParams(entities.AuthContext{}, "test", []byte(test.body))

			if test.want == nil {
				assert.Nil(t, params.Name)
				return
			}
			require.NotNil(t, params.Name)
			assert.Equal(t, *test.want, *params.Name)
		})
	}
}
