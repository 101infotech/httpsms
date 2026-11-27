package entities

import (
	"reflect"
	"strings"
	"testing"
	"unicode/utf8"

	"github.com/stretchr/testify/assert"
)

func TestContact_BSONFieldNames(t *testing.T) {
	contactType := reflect.TypeOf(Contact{})
	expected := map[string]string{
		"ID":           "_id",
		"UserID":       "user_id",
		"Name":         "name",
		"Emails":       "emails",
		"PhoneNumbers": "phone_numbers",
		"Groups":       "groups",
		"Properties":   "properties",
		"CreatedAt":    "created_at",
		"UpdatedAt":    "updated_at",
	}

	for fieldName, bsonName := range expected {
		field, found := contactType.FieldByName(fieldName)
		assert.True(t, found)
		assert.Equal(t, bsonName, field.Tag.Get("bson"))
	}
}

func TestSanitizeContactGroups_TrimsDropsEmptyAndDeduplicatesCaseInsensitively(t *testing.T) {
	groups := SanitizeContactGroups([]string{" Students ", "", "students", "Staff", "STAFF"})

	assert.Equal(t, []string{"Students", "Staff"}, []string(groups))
}

func TestSanitizeContactGroups_EnforcesCountAndLengthLimits(t *testing.T) {
	groups := make([]string, 0, MaxContactGroups+5)
	groups = append(groups, strings.Repeat("x", MaxContactGroupNameLength+10))
	for index := 0; index < MaxContactGroups+5; index++ {
		groups = append(groups, string(rune('A'+index)))
	}

	sanitized := SanitizeContactGroups(groups)

	assert.Len(t, sanitized, MaxContactGroups)
	for _, group := range sanitized {
		assert.LessOrEqual(t, utf8.RuneCountInString(group), MaxContactGroupNameLength)
	}
}

func TestContactProperties_ValueScanRoundTrip(t *testing.T) {
	cases := []ContactProperties{
		nil,
		{},
		{"company": "Acme", "role": "CTO"},
	}

	for _, original := range cases {
		value, err := original.Value()
		assert.Nil(t, err)

		var scanned ContactProperties
		assert.Nil(t, scanned.Scan(value))

		if len(original) == 0 {
			assert.Equal(t, 0, len(scanned))
			continue
		}
		assert.Equal(t, original, scanned)
	}
}

func TestContactProperties_ScanFromString(t *testing.T) {
	var scanned ContactProperties
	assert.Nil(t, scanned.Scan(`{"k":"v"}`))
	assert.Equal(t, ContactProperties{"k": "v"}, scanned)
}

func TestContactProperties_ScanNil(t *testing.T) {
	var scanned ContactProperties
	assert.Nil(t, scanned.Scan(nil))
	assert.Equal(t, 0, len(scanned))
}

func TestContactProperties_ScanUnsupportedType(t *testing.T) {
	var scanned ContactProperties

	err := scanned.Scan(123)

	assert.Error(t, err)
	assert.Contains(t, err.Error(), "unsupported type [int] for ContactProperties")
}
