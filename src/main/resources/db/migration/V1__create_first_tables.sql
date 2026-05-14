

CREATE TABLE person {
    uuid VARCHAR(60) PRIMARY KEY,
    name_raw VARCHAR(120), -- from being read in by ai PDF readers
    name_first VARCHAR(60),
    name_last VARCHAR(60), --
    data_confidence INT, -- LOW (read in from AI), MED (entered by tenant), HIGH (confirmed by staff)
    birthday DATE,
    roles VARCHAR(50), -- idk what data type this should be, I imagine it as an array of roles
    }


CREATE TABLE user {
    uuid VARCHAR(60) PRIMARY KEY,
    person_id VARCHAR(60),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (property_code) REFERENCES properties(property_code)
    }