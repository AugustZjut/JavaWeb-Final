-- 更新appointments表，确保有正确的批准相关字段和驳回原因字段

-- 检查是否已存在rejection_reason列，不存在则添加
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'appointments' AND column_name = 'rejection_reason') THEN
        ALTER TABLE "appointments" ADD COLUMN "rejection_reason" TEXT NULL;
        COMMENT ON COLUMN "appointments"."rejection_reason" IS '驳回原因 (当状态为 REJECTED 时记录)';
    END IF;
END $$;

-- 检查approved_by_user_id列是否存在，如果不存在则添加
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'appointments' AND column_name = 'approved_by_user_id') THEN
        ALTER TABLE "appointments" ADD COLUMN "approved_by_user_id" INT NULL;
        ALTER TABLE "appointments" ADD CONSTRAINT "fk_appointments_approved_by_user"
            FOREIGN KEY ("approved_by_user_id") REFERENCES "users" ("user_id") ON DELETE SET NULL ON UPDATE CASCADE;
        CREATE INDEX "fk_appointments_users_idx" ON "appointments" ("approved_by_user_id");
    END IF;
END $$;

-- 检查approval_datetime列是否存在，如果不存在则添加
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'appointments' AND column_name = 'approval_datetime') THEN
        ALTER TABLE "appointments" ADD COLUMN "approval_datetime" TIMESTAMP NULL;
    END IF;
END $$;

-- 添加注释
COMMENT ON COLUMN "appointments"."approved_by_user_id" IS '审核人ID，对应users表中的user_id';
COMMENT ON COLUMN "appointments"."approval_datetime" IS '审核时间';
