CREATE DATABASE IF NOT EXISTS vault;

-- Create vault database user
CREATE USER IF NOT EXISTS 'vault-backend'@'%' IDENTIFIED BY 'supersecretpassword';
GRANT ALL PRIVILEGES ON vault.* TO 'vault-backend'@'%';
FLUSH PRIVILEGES;
