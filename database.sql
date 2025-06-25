-- Create database if not exists
CREATE DATABASE IF NOT EXISTS vote;

-- Use the database
USE vote;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL DEFAULT 'user',
    fullname VARCHAR(100),
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL
);
ALTER TABLE users ADD UNIQUE (email);
-- Create admin user
INSERT INTO users (username, password, role, fullname, email) 
VALUES ('admin', 'admin123', 'admin', 'Administrator', 'admin@example.com');

-- Create a regular user for testing
INSERT INTO users (username, password, role, fullname, email) 
VALUES ('user', 'user123', 'user', 'Regular User', 'user@example.com');

-- Create OTP table for password reset
CREATE TABLE IF NOT EXISTS otp (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100),
    otp_code VARCHAR(6) NOT NULL,
    status ENUM('active', 'inactive') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_email FOREIGN KEY (email) REFERENCES users(email) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create elections table
CREATE TABLE IF NOT EXISTS elections (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    status ENUM('pending', 'active', 'completed', 'cancelled') DEFAULT 'pending',
    created_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_election_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Create application_settings table
CREATE TABLE IF NOT EXISTS application_settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value TEXT,
    setting_description VARCHAR(255),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert default application settings
INSERT INTO application_settings (setting_key, setting_value, setting_description) VALUES
('db_connection', 'jdbc:mysql://localhost:3306/vote', 'Database connection URL'),
('enable_user_registration', 'true', 'Allow new users to register'),
('enable_email_notifications', 'false', 'Send email notifications for system events'),
('enable_ip_blocking', 'true', 'Block IP addresses after multiple failed login attempts'),
('smtp_server', '', 'SMTP server hostname'),
('smtp_port', '587', 'SMTP server port'),
('email_username', '', 'Email account username'),
('email_password', '', 'Email account password'),
('email_from', '', 'From email address for outgoing emails'),
('application_theme', 'Default', 'UI theme (Default, Light, Dark, High Contrast)'),
('application_title', 'Voting System', 'Application title shown in windows and reports'),
('date_format', 'MM/dd/yyyy', 'Date format used throughout the application'),
('last_backup', 'Never', 'Timestamp of last database backup')
ON DUPLICATE KEY UPDATE setting_description = VALUES(setting_description);

-- Create candidates table
CREATE TABLE IF NOT EXISTS candidates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    election_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    profile TEXT,
    photo_url VARCHAR(255),
    position VARCHAR(100),
    votes INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_candidate_election FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE
);

-- Create votes table to record who voted for what (prevents duplicate voting)
CREATE TABLE IF NOT EXISTS votes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    election_id INT NOT NULL,
    user_id INT NOT NULL,
    candidate_id INT NOT NULL,
    voted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_vote UNIQUE (election_id, user_id),
    CONSTRAINT fk_vote_election FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE,
    CONSTRAINT fk_vote_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_vote_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);

ALTER TABLE users ADD COLUMN last_login TIMESTAMP NULL;
