-- Properties must exist before persons, since person references property.

CREATE TABLE property (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    property_code VARCHAR(255),
    property_name TEXT NOT NULL,
    property_address TEXT NOT NULL,
    property_city VARCHAR(255),
    property_state VARCHAR(2),
    purchase_date DATE,
    year_built INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Represents a real-world person. May exist in the system before they have any
-- agent account OR are attached to a tenancy -- e.g. a resident or contact added manually.
CREATE TABLE person (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    name_raw VARCHAR(120),
    name_full VARCHAR(120),
    birthday DATE,
    personal_phone VARCHAR(120),
    personal_email VARCHAR(120),
    primary_property UUID,   -- Either the place of their tenancy, residence, or primary agent location
    mailing_address TEXT,
    emergency_contact UUID,
    social VARCHAR(72),            -- AES-256 encrypted SSN
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (emergency_contact) REFERENCES person(uuid),
    FOREIGN KEY (primary_property) REFERENCES property(uuid)
);

CREATE INDEX idx_person_name_last  ON person(LOWER(name_last))      WHERE deleted_at IS NULL;
CREATE INDEX idx_person_name_first ON person(LOWER(name_first))     WHERE deleted_at IS NULL;
CREATE INDEX idx_person_email      ON person(LOWER(personal_email)) WHERE deleted_at IS NULL;

-- Tracks known or inferred relationships between persons (e.g. spouse, parent, roommate).
-- Confidence score allows for muddied or inferred data.
-- NOTE: application layer must prevent both (A,B) and (B,A) from existing simultaneously.
-- document_id is nullable until the documents module is implemented.
CREATE TABLE person_relationship (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    person_1 UUID NOT NULL,        -- person_1 is the (relationship) of person_2
    person_2 UUID NOT NULL,
    relationship VARCHAR(40) NOT NULL,
    strength NUMERIC(6,4),         -- confidence in this relationship (0.0 - 1.0)
    document_id UUID,              -- nullable until documents module is implemented
    FOREIGN KEY (person_1) REFERENCES person(uuid),
    FOREIGN KEY (person_2) REFERENCES person(uuid)
);