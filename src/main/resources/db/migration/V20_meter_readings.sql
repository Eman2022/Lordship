-- Add field for serial numbers?
CREATE TABLE meters (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    meter_id UUID NOT NULL, -- this should be used to lock a meter to a lot
    title TEXT,
    description VARCHAR(255),
    measurement TEXT, -- set as an enum?
    point_x DOUBLE PRECISION NOT NULL, -- Will real GPS coordinates be used in future?
    point_y DOUBLE PRECISION NOT NULL, -- Point position for meters to be placed on map (front end)
    installed_at DATE,
    deleted_at TIMESTAMP,
    FOREIGN KEY (meter_id) REFERENCES lot(uuid) -- references lot
);