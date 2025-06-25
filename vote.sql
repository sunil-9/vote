-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jun 22, 2025 at 06:23 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `vote`
--

-- --------------------------------------------------------

--
-- Table structure for table `application_settings`
--

CREATE TABLE `application_settings` (
  `setting_key` varchar(50) NOT NULL,
  `setting_value` text DEFAULT NULL,
  `setting_description` varchar(255) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `application_settings`
--

INSERT INTO `application_settings` (`setting_key`, `setting_value`, `setting_description`, `updated_at`) VALUES
('application_theme', 'Default', 'UI theme (Default, Light, Dark, High Contrast)', '2025-06-20 08:33:15'),
('application_title', 'Voting System', 'Application title shown in windows and reports', '2025-06-20 08:33:15'),
('date_format', 'MM/dd/yyyy', 'Date format used throughout the application', '2025-06-20 08:33:15'),
('db_connection', 'jdbc:mysql://localhost:3306/vote', 'Database connection URL', '2025-06-20 08:33:15'),
('email_from', '', 'From email address for outgoing emails', '2025-06-20 08:33:15'),
('email_password', '', 'Email account password', '2025-06-20 08:33:15'),
('email_username', '', 'Email account username', '2025-06-20 08:33:15'),
('enable_email_notifications', 'false', 'Send email notifications for system events', '2025-06-20 08:33:15'),
('enable_ip_blocking', 'true', 'Block IP addresses after multiple failed login attempts', '2025-06-20 08:33:15'),
('enable_user_registration', 'true', 'Allow new users to register', '2025-06-20 08:33:15'),
('last_backup', 'Never', 'Timestamp of last database backup', '2025-06-20 08:33:15'),
('smtp_port', '587', 'SMTP server port', '2025-06-20 08:33:15'),
('smtp_server', '', 'SMTP server hostname', '2025-06-20 08:33:15');

-- --------------------------------------------------------

--
-- Table structure for table `candidates`
--

CREATE TABLE `candidates` (
  `id` int(11) NOT NULL,
  `election_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `profile` text DEFAULT NULL,
  `photo_url` varchar(255) DEFAULT NULL,
  `position` varchar(100) DEFAULT NULL,
  `votes` int(11) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `candidates`
--

INSERT INTO `candidates` (`id`, `election_id`, `name`, `profile`, `photo_url`, `position`, `votes`, `created_at`) VALUES
(1, 1, 'sunil', 'please vote me', 'H:\\apply for new zealand\\Payslip-01-Feb-2023.jpg', 'nomi', 1, '2025-06-20 08:01:42'),
(2, 1, 'saroj', 'this is my bio', 'H:\\personal\\red denger\\IMG_20180317_194817_560.jpg', 'can', 1, '2025-06-20 08:02:15');

-- --------------------------------------------------------

--
-- Table structure for table `elections`
--

CREATE TABLE `elections` (
  `id` int(11) NOT NULL,
  `title` varchar(100) NOT NULL,
  `description` text DEFAULT NULL,
  `start_date` datetime NOT NULL,
  `end_date` datetime NOT NULL,
  `status` enum('pending','active','completed','cancelled') DEFAULT 'pending',
  `created_by` int(11) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `elections`
--

INSERT INTO `elections` (`id`, `title`, `description`, `start_date`, `end_date`, `status`, `created_by`, `created_at`, `updated_at`) VALUES
(1, 'new election', 'this is election', '2025-06-18 13:39:00', '2025-06-30 13:39:00', 'active', 1, '2025-06-20 07:57:41', '2025-06-20 08:12:34');

-- --------------------------------------------------------

--
-- Table structure for table `otp`
--

CREATE TABLE `otp` (
  `id` int(11) NOT NULL,
  `email` varchar(100) NOT NULL,
  `otp_code` varchar(6) NOT NULL,
  `status` enum('active','inactive') DEFAULT 'active',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `otp`
--

INSERT INTO `otp` (`id`, `email`, `otp_code`, `status`, `created_at`, `updated_at`) VALUES
(1, 'sunilsapkota9@gmail.com', '399500', 'inactive', '2025-06-19 16:50:29', '2025-06-19 16:51:13');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(10) NOT NULL DEFAULT 'user',
  `fullname` varchar(100) DEFAULT NULL,
  `email` varchar(100) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_login` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `username`, `password`, `role`, `fullname`, `email`, `created_at`, `last_login`) VALUES
(1, 'admin', '$2a$12$pRTJvjHCpLETZvLe9w.uM.TWsvn1ZBX2Jlw0LwLQE9zFUEjbMLn7K', 'admin', 'Administrator', 'admin@example.com', '2025-06-19 15:38:08', NULL),
(2, 'user', '$2a$12$pRTJvjHCpLETZvLe9w.uM.TWsvn1ZBX2Jlw0LwLQE9zFUEjbMLn7K', 'user', 'test user', 'user@example.com', '2025-06-19 15:38:08', NULL),
(3, 'sunil', '$2a$12$pRTJvjHCpLETZvLe9w.uM.TWsvn1ZBX2Jlw0LwLQE9zFUEjbMLn7K', 'user', 'sunil', 'sunil@gmail.com', '2025-06-19 16:15:13', '2025-06-22 16:15:35'),
(4, 'sunil1', '$2a$12$pRTJvjHCpLETZvLe9w.uM.TWsvn1ZBX2Jlw0LwLQE9zFUEjbMLn7K', 'user', 'sunil', 'sunil1@gmail.com', '2025-06-19 16:22:53', NULL),
(5, 'test', '$2a$12$Y2xRg1MoLkuBRtbhc0IsXON8leHm80Hy7zLdFYY4h1Cbx2V3dbGw6', 'user', 'test', 'sunilsapkota9@gmail.com', '2025-06-19 16:50:16', NULL),
(6, 'test1', '$2a$12$Whd759ti4YIRkTzkWXjtYOuy.MZ0yB96iLCd0NuENtaKs3jYNZhZ2', 'admin', 'test', 'test@gmail.com', '2025-06-19 17:04:37', NULL),
(7, 'jhon', '$2a$12$xgf6dNZY1pBqgvvrthaexOuE0OV4A2BE8T8xevnd4zHYfX/Ib2vd2', 'user', 'jhon', 'jhon@gmail.com', '2025-06-22 16:14:44', '2025-06-22 16:21:16');

-- --------------------------------------------------------

--
-- Table structure for table `votes`
--

CREATE TABLE `votes` (
  `id` int(11) NOT NULL,
  `election_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `candidate_id` int(11) NOT NULL,
  `voted_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `votes`
--

INSERT INTO `votes` (`id`, `election_id`, `user_id`, `candidate_id`, `voted_at`) VALUES
(1, 1, 3, 1, '2025-06-20 09:03:33'),
(2, 1, 7, 2, '2025-06-22 16:21:28');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `application_settings`
--
ALTER TABLE `application_settings`
  ADD PRIMARY KEY (`setting_key`);

--
-- Indexes for table `candidates`
--
ALTER TABLE `candidates`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_candidate_election` (`election_id`);

--
-- Indexes for table `elections`
--
ALTER TABLE `elections`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_election_creator` (`created_by`);

--
-- Indexes for table `otp`
--
ALTER TABLE `otp`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_email` (`email`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Indexes for table `votes`
--
ALTER TABLE `votes`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_vote` (`election_id`,`user_id`),
  ADD KEY `fk_vote_user` (`user_id`),
  ADD KEY `fk_vote_candidate` (`candidate_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `candidates`
--
ALTER TABLE `candidates`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `elections`
--
ALTER TABLE `elections`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `otp`
--
ALTER TABLE `otp`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT for table `votes`
--
ALTER TABLE `votes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `candidates`
--
ALTER TABLE `candidates`
  ADD CONSTRAINT `fk_candidate_election` FOREIGN KEY (`election_id`) REFERENCES `elections` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `elections`
--
ALTER TABLE `elections`
  ADD CONSTRAINT `fk_election_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `otp`
--
ALTER TABLE `otp`
  ADD CONSTRAINT `fk_email` FOREIGN KEY (`email`) REFERENCES `users` (`email`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `votes`
--
ALTER TABLE `votes`
  ADD CONSTRAINT `fk_vote_candidate` FOREIGN KEY (`candidate_id`) REFERENCES `candidates` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_vote_election` FOREIGN KEY (`election_id`) REFERENCES `elections` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_vote_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
