-- Fix book table columns that were incorrectly created as TINYINT.
-- TINYINT (signed) only allows -128..127, so page 128+ and year 2026 fail with error 1264.
-- Run this against the application database (e.g. sigic).

ALTER TABLE `book`
  MODIFY COLUMN `firstPage` INT NOT NULL,
  MODIFY COLUMN `lastPage` INT NOT NULL,
  MODIFY COLUMN `year` SMALLINT NOT NULL;
