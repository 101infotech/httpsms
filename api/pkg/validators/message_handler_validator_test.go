package validators

import (
	"context"
	"testing"

	"github.com/NdoleStudio/httpsms/pkg/requests"
	"github.com/NdoleStudio/httpsms/pkg/telemetry"
	"github.com/stretchr/testify/assert"
)

func TestMessageHandlerValidatorValidateMessageCallMissedIndex(t *testing.T) {
	tests := []struct {
		name    string
		request requests.MessageCallMissedIndex
		valid   bool
	}{
		{
			name:    "defaults",
			request: (&requests.MessageCallMissedIndex{}).Sanitize(),
			valid:   true,
		},
		{
			name: "owners and pagination",
			request: (&requests.MessageCallMissedIndex{
				Owners: []string{"+18005550199", "+18005550100"},
				Skip:   "10",
				Limit:  "100",
			}).Sanitize(),
			valid: true,
		},
		{
			name: "invalid owner",
			request: (&requests.MessageCallMissedIndex{
				Owners: []string{"not-a-phone-number"},
			}).Sanitize(),
			valid: false,
		},
		{
			name: "limit below minimum",
			request: (&requests.MessageCallMissedIndex{
				Limit: "0",
			}).Sanitize(),
			valid: false,
		},
		{
			name: "limit above maximum",
			request: (&requests.MessageCallMissedIndex{
				Limit: "101",
			}).Sanitize(),
			valid: false,
		},
		{
			name: "negative skip",
			request: (&requests.MessageCallMissedIndex{
				Skip: "-1",
			}).Sanitize(),
			valid: false,
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			validator := newMessageHandlerValidator()
			errors := validator.ValidateMessageCallMissedIndex(context.Background(), test.request)

			if test.valid {
				assert.Empty(t, errors)
			} else {
				assert.NotEmpty(t, errors)
			}
		})
	}
}

func newMessageHandlerValidator() *MessageHandlerValidator {
	logger := &contactValidatorNoopLogger{}
	return NewMessageHandlerValidator(
		logger,
		telemetry.NewOtelLogger("test", logger),
		nil,
		nil,
	)
}
