-- 数据库更新脚本：添加password_change_required字段和公众预约管理权限字段
-- 如果您的数据库中已经存在users表但缺少相关字段，请执行此脚本

-- 检查password_change_required字段是否存在，如果不存在则添加
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' 
        AND column_name = 'password_change_required'
    ) THEN
        ALTER TABLE users ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;
        COMMENT ON COLUMN users.password_change_required IS '是否需要强制修改密码 (TRUE: 是, FALSE: 否)';
    END IF;
END $$;

-- 检查can_manage_public_appointments字段是否存在，如果不存在则添加
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' 
        AND column_name = 'can_manage_public_appointments'
    ) THEN
        ALTER TABLE users ADD COLUMN can_manage_public_appointments BOOLEAN NOT NULL DEFAULT FALSE;
        COMMENT ON COLUMN users.can_manage_public_appointments IS '是否有权限管理公众预约';
    END IF;
END $$;

-- 默认给系统管理员和学校管理员授予公众预约管理权限
UPDATE users 
SET can_manage_public_appointments = TRUE 
WHERE role IN ('SYSTEM_ADMIN', 'SCHOOL_ADMIN');

-- 为系统管理员和学校管理员自动设置公众预约管理权限
-- 虽然代码逻辑中这些角色默认就有权限，但为了数据一致性，我们在数据库中也设置为true
UPDATE users 
SET can_manage_public_appointments = TRUE 
WHERE role IN ('SYSTEM_ADMIN', 'SCHOOL_ADMIN');

-- 为审计管理员确保没有公众预约管理权限
UPDATE users 
SET can_manage_public_appointments = FALSE 
WHERE role = 'AUDIT_ADMIN';

-- 输出更新结果
DO $$
DECLARE
    system_admin_count INT;
    school_admin_count INT;
    audit_admin_count INT;
BEGIN
    SELECT COUNT(*) INTO system_admin_count FROM users WHERE role = 'SYSTEM_ADMIN' AND can_manage_public_appointments = TRUE;
    SELECT COUNT(*) INTO school_admin_count FROM users WHERE role = 'SCHOOL_ADMIN' AND can_manage_public_appointments = TRUE;
    SELECT COUNT(*) INTO audit_admin_count FROM users WHERE role = 'AUDIT_ADMIN' AND can_manage_public_appointments = FALSE;
    
    RAISE NOTICE '已更新 % 个系统管理员的公众预约管理权限', system_admin_count;
    RAISE NOTICE '已更新 % 个学校管理员的公众预约管理权限', school_admin_count;
    RAISE NOTICE '已确认 % 个审计管理员没有公众预约管理权限', audit_admin_count;
END $$;

-- 插入一些示例数据（可选）
-- 注意：这些语句仅在开发环境中使用，生产环境请根据实际需要调整

-- 插入示例部门数据
INSERT INTO departments (department_code, department_name, department_type) VALUES
('ADM001', '行政办公室', 'ADMINISTRATIVE'),
('STU001', '学生工作处', 'DIRECTLY_AFFILIATED'),
('CS001', '计算机科学学院', 'COLLEGE'),
('LIB001', '图书馆', 'DIRECTLY_AFFILIATED')
ON CONFLICT (department_code) DO NOTHING;

-- 插入示例公众预约数据
INSERT INTO appointments (
    appointment_type, campus, entry_datetime, applicant_organization, 
    applicant_name, applicant_id_card, applicant_phone, transport_mode, 
    visit_reason, status, application_date
) VALUES
('PUBLIC', '主校区', '2024-12-01 09:00:00', '北京科技有限公司', '张三', '110101199001011234', '13800138001', '开车', '商务洽谈', 'APPROVED', '2024-11-28 10:30:00'),
('PUBLIC', '南校区', '2024-12-01 14:00:00', '上海教育机构', '李四', '310101199002022345', '13800138002', '步行', '学术交流', 'PENDING_APPROVAL', '2024-11-29 09:15:00'),
('PUBLIC', '主校区', '2024-12-02 10:00:00', '广州文化传媒', '王五', '440101199003033456', '13800138003', '公交', '采访报道', 'APPROVED', '2024-11-29 16:20:00'),
('PUBLIC', '东校区', '2024-12-02 15:30:00', '深圳技术公司', '赵六', '440301199004044567', '13800138004', '地铁', '技术调研', 'REJECTED', '2024-11-30 11:45:00'),
('PUBLIC', '主校区', '2024-12-03 08:30:00', '天津咨询集团', '孙七', '120101199005055678', '13800138005', '开车', '咨询服务', 'COMPLETED', '2024-11-30 14:10:00')
ON CONFLICT DO NOTHING;

-- 更新统计信息
ANALYZE users;
ANALYZE departments;
ANALYZE appointments;

-- 修改audit_logs表中target_entity_id字段类型为INT
-- 这个修改确保ID字段都是整数类型，保持数据一致性
DO $$
BEGIN
    -- 检查target_entity_id字段是否为VARCHAR类型
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'audit_logs' 
        AND column_name = 'target_entity_id'
        AND data_type = 'character varying'
    ) THEN
        -- 首先清空表中的数据（如果有的话），因为VARCHAR到INT的转换可能失败
        -- 在生产环境中，您可能需要先备份数据并写专门的迁移脚本
        DELETE FROM audit_logs WHERE target_entity_id IS NOT NULL AND target_entity_id !~ '^[0-9]+$';
        
        -- 修改字段类型
        ALTER TABLE audit_logs ALTER COLUMN target_entity_id TYPE INT USING CASE 
            WHEN target_entity_id ~ '^[0-9]+$' THEN target_entity_id::INT 
            ELSE NULL 
        END;
        
        RAISE NOTICE 'target_entity_id字段类型已更新为INT';
    END IF;
END $$;

COMMIT;
