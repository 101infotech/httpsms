package entities

import (
	"database/sql/driver"
	"encoding/json"
	"strings"
	"time"
	"unicode/utf8"

	"github.com/NdoleStudio/stacktrace"
	"github.com/google/uuid"
	"github.com/lib/pq"
)

// EntityNameContact is the entitlement entity name for contacts.
const EntityNameContact = "Contact"

const (
	// MaxContactGroups is the maximum number of groups a contact can belong to.
	MaxContactGroups = 20
	// MaxContactGroupNameLength is the maximum length of a contact group name in runes.
	MaxContactGroupNameLength = 50
)

// ContactGroup is a group label with the number of contacts that carry it.
type ContactGroup struct {
	Name  string `json:"name" example:"Students"`
	Count int64  `json:"count" example:"42"`
}

// SanitizeContactGroups trims, drops empty values, removes case-insensitive
// duplicates and enforces the group count/length limits.
func SanitizeContactGroups(values []string) pq.StringArray {
	result := make(pq.StringArray, 0, len(values))
	seen := make(map[string]struct{}, len(values))
	for _, value := range values {
		value = strings.TrimSpace(value)
		if value == "" {
			continue
		}
		if utf8.RuneCountInString(value) > MaxContactGroupNameLength {
			value = strings.TrimSpace(string([]rune(value)[:MaxContactGroupNameLength]))
			if value == "" {
				continue
			}
		}
		key := strings.ToLower(value)
		if _, found := seen[key]; found {
			continue
		}
		seen[key] = struct{}{}
		result = append(result, value)
		if len(result) == MaxContactGroups {
			break
		}
	}
	return result
}

// ContactProperties is a free-form key/value map persisted as a jsonb column.
type ContactProperties map[string]string

var _ driver.Valuer = ContactProperties{}

// Value implements driver.Valuer, serializing the map to JSON bytes.
func (p ContactProperties) Value() (driver.Value, error) {
	if p == nil {
		return []byte("{}"), nil
	}

	data, err := json.Marshal(p)
	if err != nil {
		return nil, stacktrace.Propagatef(err, "cannot marshal ContactProperties")
	}

	return data, nil
}

// Scan implements sql.Scanner, deserializing jsonb bytes/string into the map.
func (p *ContactProperties) Scan(src any) error {
	if src == nil {
		*p = ContactProperties{}
		return nil
	}

	var data []byte
	switch value := src.(type) {
	case []byte:
		data = value
	case string:
		data = []byte(value)
	default:
		return stacktrace.NewErrorf("unsupported type [%T] for ContactProperties", src)
	}

	if len(data) == 0 {
		*p = ContactProperties{}
		return nil
	}

	result := ContactProperties{}
	if err := json.Unmarshal(data, &result); err != nil {
		return stacktrace.Propagatef(err, "cannot unmarshal ContactProperties")
	}

	*p = result
	return nil
}

// Contact represents a saved contact belonging to a user.
type Contact struct {
	ID           uuid.UUID         `json:"id" gorm:"primaryKey;type:uuid" bson:"_id" example:"32343a19-da5e-4b1b-a767-3298a73703cb"`
	UserID       UserID            `json:"user_id" gorm:"index" bson:"user_id" example:"WB7DRDWrJZRGbYrv2CKGkqbzvqdC"`
	Name         string            `json:"name" bson:"name" example:"Alice Smith"`
	Emails       pq.StringArray    `json:"emails" gorm:"type:text[]" bson:"emails" swaggertype:"array,string" example:"alice@example.com"`
	PhoneNumbers pq.StringArray    `json:"phone_numbers" gorm:"type:text[]" bson:"phone_numbers" swaggertype:"array,string" example:"+18005550199,+18005550100"`
	Groups       pq.StringArray    `json:"groups" gorm:"type:text[]" bson:"groups" swaggertype:"array,string" example:"Students,Staff"`
	Properties   ContactProperties `json:"properties" gorm:"type:jsonb" bson:"properties" swaggertype:"object,string"`
	CreatedAt    time.Time         `json:"created_at" bson:"created_at" example:"2022-06-05T14:26:02.302718+03:00"`
	UpdatedAt    time.Time         `json:"updated_at" bson:"updated_at" example:"2022-06-05T14:26:02.302718+03:00"`
}

// TableName overrides the table name used by Contact.
func (Contact) TableName() string {
	return "contacts"
}
