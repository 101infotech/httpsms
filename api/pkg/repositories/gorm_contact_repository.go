package repositories

import (
	"context"
	"errors"
	"fmt"
	"strings"

	"github.com/NdoleStudio/httpsms/pkg/entities"
	"github.com/NdoleStudio/httpsms/pkg/telemetry"
	"github.com/NdoleStudio/stacktrace"
	"github.com/google/uuid"
	"github.com/lib/pq"
	"gorm.io/gorm"
)

// gormContactRepository is responsible for persisting entities.Contact.
type gormContactRepository struct {
	logger telemetry.Logger
	tracer telemetry.Tracer
	db     *gorm.DB
}

// NewGormContactRepository creates the GORM version of the ContactRepository.
func NewGormContactRepository(
	logger telemetry.Logger,
	tracer telemetry.Tracer,
	db *gorm.DB,
) ContactRepository {
	return &gormContactRepository{
		logger: logger.WithService(fmt.Sprintf("%T", &gormContactRepository{})),
		tracer: tracer,
		db:     db,
	}
}

func (repository *gormContactRepository) Store(ctx context.Context, contacts []*entities.Contact) error {
	ctx, span := repository.tracer.Start(ctx)
	defer span.End()

	if len(contacts) == 0 {
		return nil
	}

	if err := repository.db.WithContext(ctx).Create(&contacts).Error; err != nil {
		return repository.tracer.WrapErrorSpan(span, stacktrace.Propagatef(err, "cannot store [%d] contacts", len(contacts)))
	}

	return nil
}

func (repository *gormContactRepository) Update(ctx context.Context, contact *entities.Contact) error {
	ctx, span := repository.tracer.Start(ctx)
	defer span.End()

	if err := repository.db.WithContext(ctx).Save(contact).Error; err != nil {
		return repository.tracer.WrapErrorSpan(span, stacktrace.Propagatef(err, "cannot update contact with ID [%s]", contact.ID))
	}

	return nil
}

func (repository *gormContactRepository) Load(ctx context.Context, userID entities.UserID, contactID uuid.UUID) (*entities.Contact, error) {
	ctx, span := repository.tracer.Start(ctx)
	defer span.End()

	contact := new(entities.Contact)
	err := repository.db.WithContext(ctx).
		Where("user_id = ?", userID).
		Where("id = ?", contactID).
		First(contact).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, repository.tracer.WrapErrorSpan(span, stacktrace.PropagateWithCodef(err, ErrCodeNotFound, "contact with ID [%s] for user [%s] does not exist", contactID, userID))
	}

	if err != nil {
		return nil, repository.tracer.WrapErrorSpan(span, stacktrace.Propagatef(err, "cannot load contact with ID [%s] for user [%s]", contactID, userID))
	}

	return contact, nil
}

// scopedContactQuery builds the shared query that scopes contacts to a user and
// applies the optional name/emails/phone_numbers search and group filters.
// Index and Count both build on it so their filters can never drift apart. It
// sets the model so callers can chain Find or Count without repeating the table.
func (repository *gormContactRepository) scopedContactQuery(ctx context.Context, userID entities.UserID, params IndexParams) *gorm.DB {
	scoped := repository.db.WithContext(ctx).Model(&entities.Contact{}).Where("user_id = ?", userID)
	if len(params.Query) > 0 {
		escaped := strings.NewReplacer(`%`, `\%`, `_`, `\_`).Replace(params.Query)
		queryPattern := "%" + escaped + "%"
		scoped = scoped.Where(
			repository.db.WithContext(ctx).Where("name ILIKE ?", queryPattern).
				Or("array_to_string(emails, ',') ILIKE ?", queryPattern).
				Or("array_to_string(phone_numbers, ',') ILIKE ?", queryPattern),
		)
	}
	if len(params.Group) > 0 {
		scoped = scoped.Where("? = ANY(groups)", params.Group)
	}
	return scoped
}

func (repository *gormContactRepository) Index(ctx context.Context, userID entities.UserID, params IndexParams) (*[]entities.Contact, error) {
	ctx, span := repository.tracer.Start(ctx)
	defer span.End()

	contacts := new([]entities.Contact)
	if err := repository.scopedContactQuery(ctx, userID, params).
		Order(repository.contactOrder(params)).
		Limit(params.Limit).
		Offset(params.Skip).
		Find(contacts).Error; err != nil {
		return nil, repository.tracer.WrapErrorSpan(span, stacktrace.Propagatef(err, "cannot index contacts for user [%s] with params [%+#v]", userID, params))
	}

	return contacts, nil
}

func (repository *gormContactRepository) contactOrder(params IndexParams) string {
	if params.SortBy == "" {
		return "updated_at DESC, id DESC"
	}

	sortBy := "updated_at"
	if params.SortBy == "name" {
		sortBy = "name"
	}

	direction := "ASC"
	if params.SortDescending {
		direction = "DESC"
	}

	return fmt.Sprintf("%s %s, id %s", sortBy, direction, direction)
}

func (repository *gormContactRepository) Count(ctx context.Context, userID entities.UserID, params IndexParams) (int64, error) {
	ctx, span := repository.tracer.Start(ctx)
	defer span.End()

	var count int64
	if err := repository.scopedContactQuery(ctx, userID, params).Count(&count).Error; err != nil {
		return 0, repository.tracer.WrapErrorSpan(span, stacktrace.Propagatef(err, "cannot count contacts for user [%s] with query [%s]", userID, params.Query))
	}

	return count, nil
}

func (repository *gormContactRepository) Groups(ctx context.Context, userID entities.UserID) ([]entities.ContactGroup, error) {
	ctx, span := repository.tracer.Start(ctx)
	defer span.End()

	groups := make([]entities.ContactGroup, 0)
	if err := repository.db.WithContext(ctx).
		Table("contacts").
		Select("group_name AS name, COUNT(*) AS count").
		Joins("CROSS JOIN unnest(groups) AS group_name").
		Where("user_id = ?", userID).
		Group("group_name").
		Order("group_name ASC").
		Scan(&groups).Error; err != nil {
		return nil, repository.tracer.WrapErrorSpan(span, stacktrace.Propagatef(err, "cannot list contact groups for user [%s]", userID))
	}

	return groups, nil
}

func (repository *gormContactRepository) RenameGroup(ctx context.Context, userID entities.UserID, oldName string, newName string) error {
	ctx, span := repository.tracer.Start(ctx)
	defer span.End()

	// array_replace renames the label, while the nested DISTINCT ON removes any
	// case-insensitive duplicate that would appear when the target label already
	// exists on the same contact. UpdateColumn skips updated_at so renaming a
	// group does not reshuffle contacts ordered by recent changes.
	expression := gorm.Expr(`(
		SELECT array_agg(deduped.group_name ORDER BY deduped.position)
		FROM (
			SELECT DISTINCT ON (lower(unnested.group_name)) unnested.group_name, unnested.position
			FROM unnest(array_replace(groups, ?, ?)) WITH ORDINALITY AS unnested(group_name, position)
			ORDER BY lower(unnested.group_name), unnested.position
		) AS deduped
	)`, oldName, newName)

	err := repository.db.WithContext(ctx).
		Model(&entities.Contact{}).
		Where("user_id = ?", userID).
		Where("? = ANY(groups)", oldName).
		UpdateColumn("groups", expression).Error
	if err != nil {
		return repository.tracer.WrapErrorSpan(span, stacktrace.Propagatef(err, "cannot rename contact group [%s] to [%s] for user [%s]", oldName, newName, userID))
	}

	return nil
}

func (repository *gormContactRepository) DeleteGroup(ctx context.Context, userID entities.UserID, name string) error {
	ctx, span := repository.tracer.Start(ctx)
	defer span.End()

	err := repository.db.WithContext(ctx).
		Model(&entities.Contact{}).
		Where("user_id = ?", userID).
		Where("? = ANY(groups)", name).
		UpdateColumn("groups", gorm.Expr("array_remove(groups, ?)", name)).Error
	if err != nil {
		return repository.tracer.WrapErrorSpan(span, stacktrace.Propagatef(err, "cannot delete contact group [%s] for user [%s]", name, userID))
	}

	return nil
}

func (repository *gormContactRepository) FetchByPhoneNumbers(ctx context.Context, userID entities.UserID, phoneNumbers []string) (*[]entities.Contact, error) {
	ctx, span := repository.tracer.Start(ctx)
	defer span.End()

	contacts := new([]entities.Contact)
	if err := repository.db.WithContext(ctx).
		Where("user_id = ?", userID).
		Where("phone_numbers && ?", pq.Array(phoneNumbers)).
		Order("updated_at ASC, id ASC").
		Find(contacts).Error; err != nil {
		return nil, repository.tracer.WrapErrorSpan(span, stacktrace.Propagatef(err, "cannot fetch contacts for user [%s] by phone numbers [%v]", userID, phoneNumbers))
	}

	return contacts, nil
}

func (repository *gormContactRepository) Delete(ctx context.Context, userID entities.UserID, contactID uuid.UUID) error {
	ctx, span := repository.tracer.Start(ctx)
	defer span.End()

	err := repository.db.WithContext(ctx).
		Where("user_id = ?", userID).
		Where("id = ?", contactID).
		Delete(&entities.Contact{}).Error
	if err != nil {
		return repository.tracer.WrapErrorSpan(span, stacktrace.Propagatef(err, "cannot delete contact with ID [%s] for user [%s]", contactID, userID))
	}

	return nil
}

func (repository *gormContactRepository) DeleteAllForUser(ctx context.Context, userID entities.UserID) error {
	ctx, span := repository.tracer.Start(ctx)
	defer span.End()

	if err := repository.db.WithContext(ctx).Where("user_id = ?", userID).Delete(&entities.Contact{}).Error; err != nil {
		return repository.tracer.WrapErrorSpan(span, stacktrace.Propagatef(err, "cannot delete all contacts for user [%s]", userID))
	}

	return nil
}
