-- Relational schema for scientific products, participants and participation types
-- MySQL-compatible DDL (utf8mb4, InnoDB)

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- 1. Core reference tables
-- =========================================================

CREATE TABLE IF NOT EXISTS researcher (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  resource_id     VARCHAR(50) NULL,          -- e.g. national ID / RUT
  full_name       VARCHAR(255) NOT NULL,
  gender_code     CHAR(1) NULL,              -- 'M' / 'F' / NULL
  email           VARCHAR(255) NULL,
  orcid           VARCHAR(50) NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT pk_researcher PRIMARY KEY (id),
  CONSTRAINT uq_researcher_orcid UNIQUE KEY (orcid),
  CONSTRAINT uq_researcher_resource UNIQUE KEY (resource_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS product_type (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code            VARCHAR(50) NOT NULL,      -- e.g. 'PUBLICATION', 'THESIS'
  description     VARCHAR(255) NOT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT pk_product_type PRIMARY KEY (id),
  CONSTRAINT uq_product_type_code UNIQUE KEY (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS product_status (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code            VARCHAR(50) NOT NULL,      -- internal code
  name            VARCHAR(100) NOT NULL,     -- human-readable label
  description     VARCHAR(255) NULL,
  is_final        TINYINT(1) NOT NULL DEFAULT 0,
  sort_order      INT NOT NULL DEFAULT 0,
  color_hex       VARCHAR(16) NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT pk_product_status PRIMARY KEY (id),
  CONSTRAINT uq_product_status_code UNIQUE KEY (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS participation_type (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code                VARCHAR(50) NOT NULL,      -- e.g. 'AUTHOR', 'CO_AUTHOR', 'STUDENT'
  name                VARCHAR(150) NOT NULL,     -- human-readable label
  description         VARCHAR(255) NULL,
  applicable_products VARCHAR(255) NULL,         -- optional: list of product type codes
  can_be_corresponding TINYINT(1) NOT NULL DEFAULT 0,
  created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT pk_participation_type PRIMARY KEY (id),
  CONSTRAINT uq_participation_type_code UNIQUE KEY (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =========================================================
-- 2. Scientific products
-- =========================================================

CREATE TABLE IF NOT EXISTS scientific_product (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

  -- Basic fields
  title               TEXT NOT NULL,              -- corresponds to descripcion
  comment             TEXT NULL,                  -- corresponds to comentario
  start_date          DATE NULL,                  -- fechaInicio
  end_date            DATE NULL,                  -- fechaTermino

  -- Type and status
  product_type_id     BIGINT UNSIGNED NOT NULL,
  status_id           BIGINT UNSIGNED NULL,

  -- Links and identifiers
  document_url        VARCHAR(512) NULL,
  view_url            VARCHAR(512) NULL,
  pdf_link            VARCHAR(512) NULL,
  anid_code           VARCHAR(100) NULL,

  -- Progress / reporting
  progress_report     VARCHAR(50) NULL,           -- can store single or comma-separated values

  -- Basal & clusters
  basal_flag          CHAR(1) NULL,               -- 'S' / 'N'
  clusters            VARCHAR(50) NULL,           -- comma-separated cluster IDs (e.g. '1,3,5')

  -- Research lines (optional JSON string or comma-separated IDs)
  research_lines      TEXT NULL,

  -- Audit
  created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  created_by          VARCHAR(100) NULL,

  CONSTRAINT pk_scientific_product PRIMARY KEY (id),

  CONSTRAINT fk_product_type
    FOREIGN KEY (product_type_id)
    REFERENCES product_type (id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,

  CONSTRAINT fk_product_status
    FOREIGN KEY (status_id)
    REFERENCES product_status (id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =========================================================
-- 3. Product–participant relationship
-- =========================================================

CREATE TABLE IF NOT EXISTS product_participation (
  id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id              BIGINT UNSIGNED NOT NULL,
  researcher_id           BIGINT UNSIGNED NOT NULL,
  participation_type_id   BIGINT UNSIGNED NOT NULL,
  is_corresponding        TINYINT(1) NOT NULL DEFAULT 0,
  ordering                INT NOT NULL DEFAULT 0,

  created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at              DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT pk_product_participation PRIMARY KEY (id),

  CONSTRAINT fk_pp_product
    FOREIGN KEY (product_id)
    REFERENCES scientific_product (id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,

  CONSTRAINT fk_pp_researcher
    FOREIGN KEY (researcher_id)
    REFERENCES researcher (id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,

  CONSTRAINT fk_pp_participation_type
    FOREIGN KEY (participation_type_id)
    REFERENCES participation_type (id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,

  -- Prevent the same researcher having the same role twice in the same product
  CONSTRAINT uq_pp_unique_participation UNIQUE KEY (product_id, researcher_id, participation_type_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =========================================================
-- 4. Product subtypes (joined inheritance)
--    Each subtype shares the same primary key as scientific_product.id
-- =========================================================

-- Publications
CREATE TABLE IF NOT EXISTS publication (
  product_id          BIGINT UNSIGNED NOT NULL,

  journal_id          BIGINT UNSIGNED NULL,
  volume              VARCHAR(50) NULL,
  issue               VARCHAR(50) NULL,
  year_published      INT NULL,
  first_page          VARCHAR(50) NULL,
  last_page           VARCHAR(50) NULL,
  doi                 VARCHAR(255) NULL,
  impact_factor       DECIMAL(6,3) NULL,

  CONSTRAINT pk_publication PRIMARY KEY (product_id),

  CONSTRAINT fk_publication_product
    FOREIGN KEY (product_id)
    REFERENCES scientific_product (id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- Thesis
CREATE TABLE IF NOT EXISTS thesis (
  product_id              BIGINT UNSIGNED NOT NULL,

  granting_institution_id BIGINT UNSIGNED NULL,  -- degree granting institution
  student_institution_id  BIGINT UNSIGNED NULL,  -- host institution
  academic_degree_id      BIGINT UNSIGNED NULL,  -- e.g. PhD, Master, Undergraduate
  thesis_status_id        BIGINT UNSIGNED NULL,
  program_start_date      DATE NULL,
  qualifying_exam_date    DATE NULL,
  degree_award_date       DATE NULL,
  full_title              TEXT NULL,
  sector_types            VARCHAR(255) NULL,     -- comma-separated IDs

  CONSTRAINT pk_thesis PRIMARY KEY (product_id),

  CONSTRAINT fk_thesis_product
    FOREIGN KEY (product_id)
    REFERENCES scientific_product (id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- Scientific events (organization of scientific events)
CREATE TABLE IF NOT EXISTS scientific_event (
  product_id          BIGINT UNSIGNED NOT NULL,

  event_type_id       BIGINT UNSIGNED NULL,
  country_code        VARCHAR(10) NULL,
  city                VARCHAR(255) NULL,
  participants_count  INT NULL,
  organizer_name      VARCHAR(255) NULL,   -- denormalized main organizer if needed

  CONSTRAINT pk_scientific_event PRIMARY KEY (product_id),

  CONSTRAINT fk_scientific_event_product
    FOREIGN KEY (product_id)
    REFERENCES scientific_product (id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- Outreach activities (diffusion)
CREATE TABLE IF NOT EXISTS outreach_activity (
  product_id          BIGINT UNSIGNED NOT NULL,

  outreach_type_id    BIGINT UNSIGNED NULL,
  country_code        VARCHAR(10) NULL,
  city                VARCHAR(255) NULL,
  place               VARCHAR(255) NULL,
  attendees_count     INT NULL,
  duration_minutes    INT NULL,
  target_audience     VARCHAR(255) NULL,   -- comma-separated IDs
  link                VARCHAR(512) NULL,
  main_responsible    VARCHAR(255) NULL,

  CONSTRAINT pk_outreach_activity PRIMARY KEY (product_id),

  CONSTRAINT fk_outreach_activity_product
    FOREIGN KEY (product_id)
    REFERENCES scientific_product (id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- Scientific collaborations
CREATE TABLE IF NOT EXISTS scientific_collaboration (
  product_id              BIGINT UNSIGNED NOT NULL,

  collaboration_type_id   BIGINT UNSIGNED NULL,
  institution_id          BIGINT UNSIGNED NULL,
  origin_country_code     VARCHAR(10) NULL,
  origin_city             VARCHAR(255) NULL,
  destination_country_code VARCHAR(10) NULL,
  destination_city        VARCHAR(255) NULL,

  CONSTRAINT pk_scientific_collaboration PRIMARY KEY (product_id),

  CONSTRAINT fk_scientific_collaboration_product
    FOREIGN KEY (product_id)
    REFERENCES scientific_product (id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- Technology transfer
CREATE TABLE IF NOT EXISTS technology_transfer (
  product_id          BIGINT UNSIGNED NOT NULL,

  institution_id      BIGINT UNSIGNED NULL,
  transfer_type_id    BIGINT UNSIGNED NULL,
  category            VARCHAR(255) NULL,   -- comma-separated IDs or JSON
  city                VARCHAR(255) NULL,
  region              VARCHAR(255) NULL,
  year                INT NULL,
  country_code        VARCHAR(10) NULL,

  CONSTRAINT pk_technology_transfer PRIMARY KEY (product_id),

  CONSTRAINT fk_technology_transfer_product
    FOREIGN KEY (product_id)
    REFERENCES scientific_product (id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- Postdoctoral fellows
CREATE TABLE IF NOT EXISTS postdoctoral_fellow_product (
  product_id              BIGINT UNSIGNED NOT NULL,

  fellow_name             VARCHAR(255) NULL,
  institution_id          BIGINT UNSIGNED NULL,
  funding_source          VARCHAR(255) NULL,   -- comma-separated or JSON
  sector_type_id          BIGINT UNSIGNED NULL,
  resources               VARCHAR(255) NULL,   -- comma-separated or JSON

  CONSTRAINT pk_postdoctoral_fellow_product PRIMARY KEY (product_id),

  CONSTRAINT fk_postdoctoral_fellow_product
    FOREIGN KEY (product_id)
    REFERENCES scientific_product (id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


SET FOREIGN_KEY_CHECKS = 1;

