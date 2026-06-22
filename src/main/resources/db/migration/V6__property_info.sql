ALTER TABLE property ADD property_manager UUID;
ALTER TABLE property ADD CONSTRAINT fk_agent_id FOREIGN KEY (property_manager) REFERENCES agent (uuid);

-- Property-person relationship (tracks multiple persons associated with a property)
CREATE TABLE property_person (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    property_code VARCHAR(5) NOT NULL,
    person_uuid UUID NOT NULL,
    contact_phone VARCHAR(120),
    contact_email VARCHAR(120),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (property_code) REFERENCES property(property_code),
    FOREIGN KEY (person_uuid) REFERENCES person(uuid)
);

-- Role(s) a person has on a property (TENANT, OWNER, EMERGENCY_CONTACT, GUARANTOR)
CREATE TABLE property_person_role (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    property_person_uuid UUID NOT NULL,
    role VARCHAR(40) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (property_person_uuid) REFERENCES property_person(uuid)
);

CREATE INDEX idx_property_person_property ON property_person(property_code) WHERE deleted_at IS NULL;
CREATE INDEX idx_property_person_person ON property_person(person_uuid) WHERE deleted_at IS NULL;


