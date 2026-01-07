-- Add created_at column to patients table
ALTER TABLE patients 
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();