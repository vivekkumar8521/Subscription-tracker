ALTER TABLE subscriptions
ADD COLUMN created_at DATETIME NULL;

ALTER TABLE payment_accounts
ADD COLUMN sync_status VARCHAR(30) NULL;

ALTER TABLE payment_accounts
ADD COLUMN last_sync_error TEXT NULL;

ALTER TABLE payment_accounts
ADD COLUMN auto_sync BIT(1) NULL;

ALTER TABLE payment_accounts
ADD COLUMN last_transaction_at DATETIME NULL;

CREATE TABLE IF NOT EXISTS payment_sync_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  payment_account_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(30),
  details TEXT,
  error_message TEXT,
  started_at DATETIME,
  finished_at DATETIME,
  created_at DATETIME,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS activity_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  action VARCHAR(120),
  details TEXT,
  source_ip VARCHAR(120),
  created_at DATETIME,
  PRIMARY KEY (id)
);
