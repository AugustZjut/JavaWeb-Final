-- =====================================================
-- Mock Data for Campus Access Appointment System
-- =====================================================

-- -----------------------------------------------------
-- Truncate all tables and restart sequences
-- -----------------------------------------------------
-- This ensures a clean state every time the script is run.
-- openGauss does not support 'ALTER SEQUENCE ... RESTART' or 'TRUNCATE ... RESTART IDENTITY'.
-- We use TRUNCATE ... CASCADE and then reset sequences manually using setval().
TRUNCATE TABLE "departments", "users", "appointments", "accompanying_persons", "audit_logs" CASCADE;

-- Reset sequences manually for each table to avoid conflicts with new records created by the application.
-- The restart value should be one greater than the maximum ID inserted in the mock data.
-- We will set this at the end of the script


-- -----------------------------------------------------
-- Table "departments"
-- -----------------------------------------------------
INSERT INTO "departments" ("department_id", "department_code", "department_name", "department_type") VALUES
(1, 'CS', '计算机科学与技术学院', 'COLLEGE'),
(2, 'LOGISTICS', '后勤保障部', 'ADMINISTRATIVE'),
(3, 'LIBRARY', '图书馆', 'DIRECTLY_AFFILIATED'),
(4, 'SECURITY', '保卫处', 'ADMINISTRATIVE'),
(5, 'EE', '电子工程学院', 'COLLEGE'),
(6, 'FINANCE', '财务处', 'ADMINISTRATIVE'),
(7, 'HR', '人力资源部', 'ADMINISTRATIVE'),
(8, 'PRESIDENT_OFFICE', '校长办公室', 'ADMINISTRATIVE');

-- -----------------------------------------------------
-- Table "users"
-- -----------------------------------------------------
-- 注意: password_hash 是默认密码 'Changeme123!' 的SM3哈希值。phone_number 应替换为实际的加密数据。
INSERT INTO "users" ("user_id", "username", "password_hash", "full_name", "department_id", "phone_number", "role", "password_change_required", "can_manage_official_appointments") VALUES
(1, 'sysadmin', '667c756cf9334e328a56e44e906245c8e214c655a160f18fdb84d79c209c49cf', '系统管理员', NULL, 'jh/jkAtuA1NYfIX5mxDOwg==', 'SYSTEM_ADMIN', TRUE, TRUE),
(2, 'schooladmin', '667c756cf9334e328a56e44e906245c8e214c655a160f18fdb84d79c209c49cf', '学校管理员', 2, '0wMDQP0bzzyzozo8ufNH3w==', 'SCHOOL_ADMIN', TRUE, TRUE),
(3, 'cs_admin', '667c756cf9334e328a56e44e906245c8e214c655a160f18fdb84d79c209c49cf', '计算机学院管理员', 1, 'BgvXXXt6KoR5/rb6pgtsvg==', 'DEPARTMENT_ADMIN', TRUE, TRUE),
(4, 'security_audit', '667c756cf9334e328a56e44e906245c8e214c655a160f18fdb84d79c209c49cf', '保卫处审计员', 4, '1A3B8YS3xrQUnJx3skMnCQ==', 'AUDIT_ADMIN', TRUE, FALSE),
(5, 'ee_admin', '667c756cf9334e328a56e44e906245c8e214c655a160f18fdb84d79c209c49cf', '电子工程学院管理员', 5, 'KeJ2BRM1pVP45dax6LvcfQ==', 'DEPARTMENT_ADMIN', TRUE, TRUE),
(6, 'finance_admin', '667c756cf9334e328a56e44e906245c8e214c655a160f18fdb84d79c209c49cf', '财务处管理员', 6, 'SY6PSdTpEIsEfg0MSiuGkw==', 'DEPARTMENT_ADMIN', TRUE, TRUE),
(7, 'hr_admin', '667c756cf9334e328a56e44e906245c8e214c655a160f18fdb84d79c209c49cf', '人力资源部管理员', 7, 'tv6CoOOLoRRNh5t3h7wc+g==', 'DEPARTMENT_ADMIN', TRUE, FALSE);

-- -----------------------------------------------------
-- Table "appointments"
-- -----------------------------------------------------
-- 注意: applicant_id_card 和 applicant_phone 应替换为实际的加密数据。

-- 预约 1: 公务来访, 待审批 (计算机学院)
INSERT INTO "appointments" ("appointment_id", "appointment_type", "campus", "entry_datetime", "applicant_organization", "applicant_name", "applicant_id_card", "applicant_phone", "transport_mode", "license_plate", "official_visit_department_id", "official_visit_contact_person", "visit_reason", "status")
VALUES
(1, 'OFFICIAL', '屏峰校区', '2025-07-10 09:00:00', 'ABC科技有限公司', '张三', 'DGx+AtnMLAQ7wkXp6xYSsKtod3k5U89fhbDeLXP2jhg=', 'TjVjrLTh9+bAa3uG6AWxjw==', 'CAR', '京A88888', 1, '李老师', '进行校企合作技术交流', 'PENDING_APPROVAL');

-- 预约 2: 个人来访, 已批准
INSERT INTO "appointments" ("appointment_id", "appointment_type", "campus", "entry_datetime", "applicant_name", "applicant_id_card", "applicant_phone", "transport_mode", "visit_reason", "status", "approved_by_user_id", "approval_datetime", "qr_code_data", "qr_code_generated_at")
VALUES
(2, 'PUBLIC', '朝晖校区', '2025-07-11 14:30:00', '李四', 'L6+NVaIK8m60okxNf4qgu31xTHPRT8NmU0zzKv00GtE=', 'FuBtJl2KH8Jru5W79L5Nhg==', 'WALK', '参观校园', 'APPROVED', 3, '2025-07-01 10:00:00', 'qr_code_placeholder_data_1', '2025-07-01 10:00:05');

-- 预约 3: 公务来访, 已驳回 (后勤保障部)
INSERT INTO "appointments" ("appointment_id", "appointment_type", "campus", "entry_datetime", "applicant_organization", "applicant_name", "applicant_id_card", "applicant_phone", "transport_mode", "license_plate", "official_visit_department_id", "official_visit_contact_person", "visit_reason", "status", "approved_by_user_id", "approval_datetime")
VALUES
(3, 'OFFICIAL', '屏峰校区', '2025-07-12 10:00:00', 'XYZ物流公司', '王五', '2BzfFndzFy6xfgqh5C/nKw1tYfPY6tg/w3AA5HsuoMA=', 'yCcTow8s6LXz8ylpPIDL4Q==', 'CAR', '沪B99999', 2, '赵主任', '洽谈物流服务合作', 'REJECTED', 2, '2025-07-02 11:00:00');

-- 预约 4: 个人来访, 有随行人员, 已批准
INSERT INTO "appointments" ("appointment_id", "appointment_type", "campus", "entry_datetime", "applicant_name", "applicant_id_card", "applicant_phone", "transport_mode", "visit_reason", "status", "approved_by_user_id", "approval_datetime", "qr_code_data", "qr_code_generated_at")
VALUES
(4, 'PUBLIC', '屏峰校区', '2025-07-12 10:00:00', '赵六', 'cT67WD2FzjYSd9eGdd+nXoQQTpfJiN88QYSxbOODJ0Q=', 'Br4+Cy3rMc/CB1mdAi+MXQ==', 'WALK', '带家人参观校园', 'APPROVED', 3, '2025-07-02 09:30:00', 'qr_code_placeholder_data_2', '2025-07-02 09:30:05');

-- 预约 5: 公务来访, 已批准 (电子工程学院)
INSERT INTO "appointments" ("appointment_id", "appointment_type", "campus", "entry_datetime", "applicant_organization", "applicant_name", "applicant_id_card", "applicant_phone", "transport_mode", "license_plate", "official_visit_department_id", "official_visit_contact_person", "visit_reason", "status", "approved_by_user_id", "approval_datetime", "qr_code_data", "qr_code_generated_at")
VALUES
(5, 'OFFICIAL', '莫干山校区', '2025-07-13 15:00:00', '华为技术有限公司', '刘备', 'L+puPAo9//hw4nk2vxhMUKtod3k5U89fhbDeLXP2jhg=', 'rSpU+HylNTqCOil/p6XB1g==', 'CAR', '粤B12345', 5, '关羽', '商讨鸿蒙系统课程合作', 'APPROVED', 5, '2025-07-03 16:00:00', 'qr_code_placeholder_data_5', '2025-07-03 16:00:05');

-- 预约 6: 公务来访, 已完成 (财务处)
INSERT INTO "appointments" ("appointment_id", "appointment_type", "campus", "entry_datetime", "applicant_organization", "applicant_name", "applicant_id_card", "applicant_phone", "transport_mode", "official_visit_department_id", "official_visit_contact_person", "visit_reason", "status", "approved_by_user_id", "approval_datetime", "qr_code_data", "qr_code_generated_at")
VALUES
(6, 'OFFICIAL', '屏峰校区', '2025-06-20 10:00:00', '普华永道会计师事务所', '曹操', '6gbFpDM8Xu5MT0GDmTboan1xTHPRT8NmU0zzKv00GtE=', 'DIfKognK2X8KhZmEfrzbgw==', 'WALK', 6, '张辽', '年度财务审计会议', 'COMPLETED', 6, '2025-06-15 11:00:00', 'qr_code_placeholder_data_6', '2025-06-15 11:00:05');

-- 预约 7: 公务来访, 已取消 (计算机学院)
INSERT INTO "appointments" ("appointment_id", "appointment_type", "campus", "entry_datetime", "applicant_organization", "applicant_name", "applicant_id_card", "applicant_phone", "transport_mode", "license_plate", "official_visit_department_id", "official_visit_contact_person", "visit_reason", "status")
VALUES
(7, 'OFFICIAL', '屏峰校区', '2025-07-15 09:30:00', '腾讯科技有限公司', '马化腾', 'oZDZmlsN+i5lZXg1lihHmQ1tYfPY6tg/w3AA5HsuoMA=', 'NrIFi1nUOIVwtFrXJcDx7A==', 'CAR', '粤B67890', 1, '李老师', '游戏开发合作洽谈', 'CANCELLED');

-- 预约 8: 公务来访, 已过期 (电子工程学院)
INSERT INTO "appointments" ("appointment_id", "appointment_type", "campus", "entry_datetime", "applicant_organization", "applicant_name", "applicant_id_card", "applicant_phone", "transport_mode", "official_visit_department_id", "official_visit_contact_person", "visit_reason", "status", "approved_by_user_id", "approval_datetime", "qr_code_data", "qr_code_generated_at")
VALUES
(8, 'OFFICIAL', '莫干山校区', '2025-06-25 14:00:00', '大疆创新科技有限公司', '汪滔', 'ySsPlD23DTe2071cXCZk/IQQTpfJiN88QYSxbOODJ0Q=', '7TCeNE9GxaravFvBrfphIg==', 'WALK', 5, '张飞', '无人机技术研讨', 'EXPIRED', 5, '2025-06-20 10:00:00', 'qr_code_placeholder_data_8', '2025-06-20 10:00:05');

-- 预约 9: 公务来访, 待审批 (校长办公室), 带随行人员
INSERT INTO "appointments" ("appointment_id", "appointment_type", "campus", "entry_datetime", "applicant_organization", "applicant_name", "applicant_id_card", "applicant_phone", "transport_mode", "license_plate", "official_visit_department_id", "official_visit_contact_person", "visit_reason", "status")
VALUES
(9, 'OFFICIAL', '屏峰校区', '2025-07-18 11:00:00', '教育部', '孙权', 'qCTnSulW5YcRfAyJb7c2vlaEBNtMxLWq/FQjDXzlX7k=', 'v9LCwd3yFXUgPut9QfvZ7g==', 'CAR', '国A00001', 8, '周瑜', '上级部门视察工作', 'PENDING_APPROVAL');

-- 预约 10: 公务来访, 待审批 (财务处)
INSERT INTO "appointments" ("appointment_id", "appointment_type", "campus", "entry_datetime", "applicant_organization", "applicant_name", "applicant_id_card", "applicant_phone", "transport_mode", "official_visit_department_id", "official_visit_contact_person", "visit_reason", "status")
VALUES
(10, 'OFFICIAL', '屏峰校区', '2025-07-20 10:00:00', '中国工商银行', '姜维', '1CmQ1AYQspfT7FTsQSfJQej+wSuwKsFv3pPL9OV+/Cg=', 'rbrHtBkxtBGz8Acv6khW8w==', 'WALK', 6, '夏侯惇', '银校合作洽谈', 'PENDING_APPROVAL');


-- -----------------------------------------------------
-- Table "accompanying_persons"
-- -----------------------------------------------------
-- 注意: id_card 和 phone 应替换为实际的加密数据。
-- 预约 4 的随行人员 (个人来访)
INSERT INTO "accompanying_persons" ("accompanying_person_id", "appointment_id", "name", "id_card", "phone") VALUES
(1, 4, '赵六的妻子', 'JJH6XJKKrICGSa6kHgGdJZ8eBbCHB9JUO4ZubNzfyZY=', 'HUipLLqDOy+GCv224koJng=='),
(2, 4, '赵六的儿子', 'oPjLgbxKBCern8JIzHhVTrpVxhp4XJYHCQFPFEDKg4Y=', 'qKAg3wNP7orCxWXuR131vA==');

-- 预约 9 的随行人员 (公务来访)
INSERT INTO "accompanying_persons" ("accompanying_person_id", "appointment_id", "name", "id_card", "phone") VALUES
(3, 9, '孙权的秘书', 'Hp8exfB+QrF04ZDEMEuGWwJPhS4eh8pK9L1gPMwWHWI=', 'nZlh0knW+CIElIugN3GrUA=='),
(4, 9, '孙权的助理', 'hI+yrej34yYCgbAVr60QebzQxtDDtfb6Uad9iU8R1mY=', 'MQlW8eVMFL84q4yrb5Yj1Q==');


-- -----------------------------------------------------
-- Table "audit_logs"
-- -----------------------------------------------------
-- 注意: hmac_sm3_hash 是选做项, 此处留空 (NULL).
INSERT INTO "audit_logs" ("log_id", "user_id", "username", "action_type", "target_entity", "target_entity_id", "details", "ip_address") VALUES
(1, 2, 'schooladmin', 'USER_LOGIN', 'users', 2, '用户 schooladmin 登录成功', '192.168.1.10'),
(2, 3, 'cs_admin', 'APPROVE_APPOINTMENT', 'appointments', 2, '批准了李四的个人预约', '192.168.1.20'),
(3, 2, 'schooladmin', 'REJECT_APPOINTMENT', 'appointments', 3, '拒绝了王五的公务预约，原因：申请材料不全', '192.168.1.10'),
(4, 1, 'sysadmin', 'CREATE_USER', 'users', 5, '创建了新用户 ee_admin', '127.0.0.1'),
(5, 5, 'ee_admin', 'APPROVE_APPOINTMENT', 'appointments', 5, '批准了刘备的公务预约', '192.168.1.30'),
(6, 6, 'finance_admin', 'APPROVE_APPOINTMENT', 'appointments', 6, '批准了曹操的公务预约', '192.168.1.40'),
(7, 1, 'sysadmin', 'UPDATE_USER', 'users', 7, '更新用户 hr_admin 的权限', '127.0.0.1');


-- -----------------------------------------------------
-- Finalize sequence values
-- -----------------------------------------------------
SELECT setval('departments_department_id_seq', 8, true);
SELECT setval('users_user_id_seq', 7, true);
SELECT setval('appointments_appointment_id_seq', 10, true);
SELECT setval('accompanying_persons_accompanying_person_id_seq', 4, true);
SELECT setval('audit_logs_log_id_seq', 7, true);
