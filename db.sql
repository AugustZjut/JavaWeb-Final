-- -----------------------------------------------------
-- Table "departments" (部门表)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS "departments" (
  "department_id" SERIAL PRIMARY KEY,
  "department_code" VARCHAR(50) NOT NULL UNIQUE,
  "department_name" VARCHAR(100) NOT NULL,
  "department_type" VARCHAR(50) NOT NULL CHECK ("department_type" IN ('ADMINISTRATIVE', 'DIRECTLY_AFFILIATED', 'COLLEGE')), -- 使用VARCHAR和CHECK约束替代ENUM
  "created_at" TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE "departments" IS '部门信息表';
COMMENT ON COLUMN "departments"."department_id" IS '部门ID';
COMMENT ON COLUMN "departments"."department_code" IS '部门编号';
COMMENT ON COLUMN "departments"."department_name" IS '部门名称';
COMMENT ON COLUMN "departments"."department_type" IS '部门类型 (行政部门、直属部门、学院)';
COMMENT ON COLUMN "departments"."created_at" IS '创建时间';
COMMENT ON COLUMN "departments"."updated_at" IS '更新时间';


-- -----------------------------------------------------
-- Table "users" (管理员表)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS "users" (
  "user_id" SERIAL PRIMARY KEY,
  "username" VARCHAR(100) NOT NULL UNIQUE,
  "password_hash" VARCHAR(128) NOT NULL,
  "full_name" VARCHAR(100) NOT NULL,
  "department_id" INT NULL,
  "phone_number" VARCHAR(255) NULL,
  "role" VARCHAR(50) NOT NULL CHECK ("role" IN ('SCHOOL_ADMIN', 'DEPARTMENT_ADMIN', 'AUDIT_ADMIN', 'SYSTEM_ADMIN')), -- 使用VARCHAR和CHECK约束替代ENUM
  "password_last_changed" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "failed_login_attempts" INT NOT NULL DEFAULT 0,
  "lockout_time" TIMESTAMP NULL,
  "created_at" TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fk_users_departments"
    FOREIGN KEY ("department_id")
    REFERENCES "departments" ("department_id")
    ON DELETE SET NULL
    ON UPDATE CASCADE
);
CREATE INDEX "fk_users_departments_idx" ON "users" ("department_id");
COMMENT ON TABLE "users" IS '管理员信息表 (包括各类管理员)';
COMMENT ON COLUMN "users"."password_hash" IS '密码 (SM3加密存储)';
COMMENT ON COLUMN "users"."phone_number" IS '联系电话 (SM2/SM4 加密存储)';


-- -----------------------------------------------------
-- Table "appointments" (预约记录表)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS "appointments" (
  "appointment_id" SERIAL PRIMARY KEY,
  "appointment_type" VARCHAR(50) NOT NULL CHECK ("appointment_type" IN ('PUBLIC', 'OFFICIAL')), -- 使用VARCHAR和CHECK约束替代ENUM
  "campus" VARCHAR(100) NOT NULL,
  "entry_datetime" TIMESTAMP NOT NULL,
  "applicant_organization" VARCHAR(200) NULL,
  "applicant_name" VARCHAR(100) NOT NULL,
  "applicant_id_card" VARCHAR(255) NOT NULL,
  "applicant_phone" VARCHAR(255) NOT NULL,
  "transport_mode" VARCHAR(50) NULL,
  "license_plate" VARCHAR(50) NULL,
  "official_visit_department_id" INT NULL,
  "official_visit_contact_person" VARCHAR(100) NULL,
  "visit_reason" TEXT NULL,
  "status" VARCHAR(50) NOT NULL DEFAULT 'PENDING_APPROVAL' CHECK ("status" IN ('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED', 'EXPIRED')), -- 使用VARCHAR和CHECK约束替代ENUM
  "qr_code_data" TEXT NULL,
  "qr_code_generated_at" TIMESTAMP NULL,
  "application_date" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "approved_by_user_id" INT NULL,
  "approval_datetime" TIMESTAMP NULL,
  "created_at" TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fk_appointments_official_department"
    FOREIGN KEY ("official_visit_department_id")
    REFERENCES "departments" ("department_id")
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT "fk_appointments_approved_by_user"
    FOREIGN KEY ("approved_by_user_id")
    REFERENCES "users" ("user_id")
    ON DELETE SET NULL
    ON UPDATE CASCADE
);
CREATE INDEX "fk_appointments_departments_idx" ON "appointments" ("official_visit_department_id");
CREATE INDEX "fk_appointments_users_idx" ON "appointments" ("approved_by_user_id");
CREATE INDEX "idx_entry_datetime" ON "appointments" ("entry_datetime");
CREATE INDEX "idx_application_date" ON "appointments" ("application_date");
-- 为加密后的身份证号创建索引可能效率不高，但按需添加
-- CREATE INDEX "idx_applicant_id_card" ON "appointments" ("applicant_id_card");
COMMENT ON TABLE "appointments" IS '校园通行预约记录表';
COMMENT ON COLUMN "appointments"."applicant_id_card" IS '申请人身份证号 (SM2/SM4 加密存储)';
COMMENT ON COLUMN "appointments"."applicant_phone" IS '申请人手机号 (SM2/SM4 加密存储)';


-- -----------------------------------------------------
-- Table "accompanying_persons" (随行人员表)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS "accompanying_persons" (
  "accompanying_person_id" SERIAL PRIMARY KEY,
  "appointment_id" INT NOT NULL,
  "name" VARCHAR(100) NOT NULL,
  "id_card" VARCHAR(255) NOT NULL,
  "phone" VARCHAR(255) NOT NULL,
  "created_at" TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fk_accompanying_appointments"
    FOREIGN KEY ("appointment_id")
    REFERENCES "appointments" ("appointment_id")
    ON DELETE CASCADE
    ON UPDATE CASCADE
);
CREATE INDEX "fk_accompanying_appointments_idx" ON "accompanying_persons" ("appointment_id");
COMMENT ON TABLE "accompanying_persons" IS '随行人员信息表';
COMMENT ON COLUMN "accompanying_persons"."id_card" IS '身份证号 (SM2/SM4 加密存储)';
COMMENT ON COLUMN "accompanying_persons"."phone" IS '手机号 (SM2/SM4 加密存储)';


-- -----------------------------------------------------
-- Table "audit_logs" (审计日志表)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS "audit_logs" (
  "log_id" SERIAL PRIMARY KEY,
  "user_id" INT NULL,
  "username" VARCHAR(100) NULL,
  "action_type" VARCHAR(100) NOT NULL,
  "target_entity" VARCHAR(100) NULL,
  "target_entity_id" VARCHAR(100) NULL,
  "details" TEXT NULL,
  "ip_address" VARCHAR(50) NULL,
  "log_timestamp" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "hmac_sm3_hash" VARCHAR(128) NULL,
  CONSTRAINT "fk_audit_logs_users"
    FOREIGN KEY ("user_id")
    REFERENCES "users" ("user_id")
    ON DELETE SET NULL
    ON UPDATE CASCADE
);
CREATE INDEX "fk_audit_logs_users_idx" ON "audit_logs" ("user_id");
CREATE INDEX "idx_log_timestamp" ON "audit_logs" ("log_timestamp" DESC);
COMMENT ON TABLE "audit_logs" IS '系统操作审计日志表';
COMMENT ON COLUMN "audit_logs"."hmac_sm3_hash" IS '日志记录的HMAC-SM3值 (选做)';
