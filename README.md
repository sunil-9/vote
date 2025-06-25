# Voting System Application

A comprehensive JavaFX application for managing elections and voting processes with role-based access control.

## Overview

This Voting System is a desktop application built with JavaFX that allows for election management, voting, and result tracking. The system supports two types of users:
- **Administrators** who can manage users, elections, candidates, and view reports
- **Regular Users** who can view elections, cast votes, and manage their profile

## Requirements

- Java Development Kit (JDK) 11 or newer
- MySQL Server 8.0 or newer
- Maven for dependency management

## Setup Instructions

### Database Setup

1. Install MySQL Server if not already installed
2. Log in to MySQL as a user with admin privileges
3. Run the database creation script:
   ```
   mysql -u root -p < database.sql
   ```
   or copy and paste the contents of `database.sql` into your MySQL client

### Email Configuration

For the "Forgot Password" functionality to work, you need to configure email settings:

1. Open `src/main/java/com/example/vote/util/EmailService.java`
2. Replace the placeholder values with your email provider's details:
   ```java
   private static final String SMTP_HOST = "smtp.example.com"; // e.g., smtp.gmail.com
   private static final int SMTP_PORT = 587; // or the port your provider uses
   private static final String SMTP_USERNAME = "your-email@example.com";
   private static final String SMTP_PASSWORD = "your-app-password"; // Use app-specific password
   ```

> **Note for Gmail users:** If you're using Gmail, you'll need to generate an "App Password" rather than using your regular password. Visit your Google Account > Security > 2-Step Verification > App passwords.

### Database Connection

By default, the application connects to a MySQL database named `vote` on localhost with username `root` and no password. To change these settings:

1. Open `src/main/java/com/example/vote/util/DatabaseConnection.java`
2. Update the connection URL, username, and password variables

## Building and Running the Application

### Using Maven

1. Navigate to the project directory
2. Run the following command to build the project:
   ```
   mvn clean package
   ```
3. Run the application:
   ```
   java -jar target/vote-1.0-SNAPSHOT.jar
   ```

### Using an IDE

1. Import the project as a Maven project
2. Make sure all dependencies are resolved
3. Run `src/main/java/com/example/vote/VoteApplication.java`

## Default Users

The application comes with two pre-configured users:

1. **Administrator**
   - Username: admin
   - Password: admin123

2. **Regular User**
   - Username: user
   - Password: user123

> **Important:** For security reasons, change these default passwords after your first login.

## Features

### Admin Features

- **Dashboard**: View system statistics
- **Manage Users**: Create, update, and delete user accounts
- **Manage Elections**: Create and manage elections, set start and end dates
- **Manage Candidates**: Add candidates to elections
- **Reports**: View voting results and system reports
- **Settings**: Configure system settings

### User Features

- **Dashboard**: View active elections and personal voting statistics
- **Elections**: View all elections, filter by status, and see details
- **Vote**: Cast votes in active elections (one vote per election)
- **Results**: View results of completed elections
- **Profile**: Update personal information, change password, and view voting history

## Architecture

The application follows the Model-View-Controller (MVC) pattern:
- **Model**: Java classes in the `model` package
- **View**: FXML files in the `resources` directory
- **Controller**: Java classes in the `controller` package

## Security Features

- Password hashing using BCrypt
- Role-based access control
- Email verification for password resets
- Prevention of duplicate voting

## Troubleshooting

### Database Connection Issues
- Verify MySQL is running
- Check connection credentials in `DatabaseConnection.java`
- Ensure the `vote` database exists

### Email Sending Issues
- Verify SMTP settings in `EmailService.java`
- Check your email provider's security settings
- For Gmail, ensure you're using an App Password if 2FA is enabled

### JavaFX Display Issues
- Ensure JavaFX modules are properly included in your JDK
- If using Java 11+, make sure JavaFX dependencies are correctly configured in Maven

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- JavaFX for UI components
- MySQL for database management
- JBCrypt for password hashing
- JavaMail for email functionality
