-- Add rejection_reason column to appointments table
-- This field stores the reason when an appointment is rejected

ALTER TABLE "appointments" ADD COLUMN "rejection_reason" TEXT NULL;

-- Add comment to the new column
COMMENT ON COLUMN "appointments"."rejection_reason" IS '驳回原因 (当状态为 REJECTED 时记录)';
