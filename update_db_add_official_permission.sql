-- Add can_manage_official_appointments column to users table
ALTER TABLE "users" ADD COLUMN "can_manage_official_appointments" BOOLEAN NOT NULL DEFAULT FALSE;

-- Add comment to the new column
COMMENT ON COLUMN "users"."can_manage_official_appointments" IS '是否有权限管理公务预约';
