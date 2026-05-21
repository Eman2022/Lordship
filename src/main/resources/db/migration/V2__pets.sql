-- Pets are linked to a tenancy, not directly to a person.
-- The owner column is intentionally omitted; see tenancy module when implemented.

CREATE TABLE pet (
     uuid UUID PRIMARY KEY DEFAULT uuidv7(),
     pet_name VARCHAR(120),
     pet_type VARCHAR(120),         -- e.g. dog, cat, bird
     pet_breed VARCHAR(120),
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     deleted_at TIMESTAMP
);