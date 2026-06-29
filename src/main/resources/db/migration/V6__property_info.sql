ALTER TABLE property ADD property_manager UUID;
ALTER TABLE property ADD CONSTRAINT fk_agent_id FOREIGN KEY (property_manager) REFERENCES agent (uuid);

-- (add updated_at column for the patch method — add ALTER TABLE property ADD updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;)
-- Property-contact relationship (tracks multiple persons associated with a property and their role/purpose)
CREATE TABLE property_contact (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    property_id UUID NOT NULL,
    person_uuid UUID NOT NULL,
    contact_phone VARCHAR(120),
    contact_email VARCHAR(120),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (property_id) REFERENCES property(uuid),
    FOREIGN KEY (person_uuid) REFERENCES person(uuid)
);

-- Stores useful links associated with a property
CREATE TABLE property_link (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    property_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (property_id) REFERENCES property(uuid)
);

CREATE INDEX idx_property_contact_property ON property_contact(property_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_property_contact_person ON property_contact(person_uuid) WHERE deleted_at IS NULL;
CREATE INDEX idx_property_link_property ON property_link(property_id) WHERE deleted_at IS NULL;



