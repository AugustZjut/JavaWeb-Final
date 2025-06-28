-- 测试公众预约数据
-- 插入一些公众预约记录用于测试

INSERT INTO appointments (
    appointment_type, campus, entry_datetime, applicant_organization, 
    applicant_name, applicant_id_card, applicant_phone, transport_mode, 
    license_plate, visit_reason, status, application_date
) VALUES 
    ('PUBLIC', '主校区', '2025-07-01 09:00:00', '北京科技有限公司', 
     '张三', '110101199001011234', '13800138001', '自驾', 
     '京A12345', '商务洽谈', 'PENDING_APPROVAL', NOW()),
    
    ('PUBLIC', '东校区', '2025-07-02 14:00:00', '上海教育机构', 
     '李四', '110101199002021234', '13800138002', '公交', 
     NULL, '参观交流', 'APPROVED', NOW()),
     
    ('PUBLIC', '主校区', '2025-07-03 10:30:00', '深圳技术公司', 
     '王五', '110101199003031234', '13800138003', '自驾', 
     '京B67890', '技术合作', 'REJECTED', NOW()),
     
    ('PUBLIC', '西校区', '2025-07-04 16:00:00', NULL, 
     '赵六', '110101199004041234', '13800138004', '步行', 
     NULL, '个人参观', 'COMPLETED', NOW()),
     
    ('PUBLIC', '主校区', '2025-07-05 08:00:00', '天津制造公司', 
     '陈七', '110101199005051234', '13800138005', '自驾', 
     '津C11111', '产业对接', 'CANCELLED', NOW());

-- 显示插入结果
SELECT COUNT(*) as public_appointment_count FROM appointments WHERE appointment_type = 'PUBLIC';
