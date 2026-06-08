## First Time Setup

1. Run the following in psql:
CREATE DATABASE lordship;
CREATE USER your_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE lordship TO your_user;

2. Set the following environment variables on your machine:
DB_URL=jdbc:postgresql://localhost:5442/lordship
DB_USERNAME=your_user
DB_PASSWORD=your_password
ROOT_PASSWORD=your_chosen_root_password

3. Run the application — Flyway will handle the rest.