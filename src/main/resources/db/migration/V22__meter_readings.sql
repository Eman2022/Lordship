CREATE TABLE meters (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    meter_id UUID NOT NULL, -- this should be used to lock a meter to a lot
    title TEXT,
    description VARCHAR(255),
    serial_number VARCHAR(255) UNIQUE, -- can help identify a meter alongside the meter_id
    point_x DOUBLE PRECISION NOT NULL,
    point_y DOUBLE PRECISION NOT NULL, -- Point position for meters to be placed on map (front end)
    installed_at DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (meter_id) REFERENCES lot(uuid) -- references lot
);


-- Enum for the different utilities (energy/water)
CREATE TABLE meter_type (
    code TEXT PRIMARY KEY,
    description TEXT
);
-- Enum for different types of utility measurements (gallons/killowatt-hours)
CREATE TABLE meter_measurement (
    code TEXT PRIMARY KEY,
    description TEXT
);

ALTER TABLE meters
    ADD COLUMN utility_type TEXT NOT NULL REFERENCES meter_type(code),
    ADD COLUMN measurement TEXT NOT NULL REFERENCES meter_measurement(code);