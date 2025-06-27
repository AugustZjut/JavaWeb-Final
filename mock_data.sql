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
SELECT setval('departments_department_id_seq', 6, false);
SELECT setval('users_user_id_seq', 6, false);
SELECT setval('appointments_appointment_id_seq', 5, false);
SELECT setval('accompanying_persons_accompanying_person_id_seq', 3, false);
SELECT setval('audit_logs_log_id_seq', 5, false);


-- -----------------------------------------------------
-- Table "departments"
-- -----------------------------------------------------
INSERT INTO "departments" ("department_id", "department_code", "department_name", "department_type") VALUES
(1, 'CS', '计算机科学与技术学院', 'COLLEGE'),
(2, 'LOGISTICS', '后勤保障部', 'ADMINISTRATIVE'),
(3, 'LIBRARY', '图书馆', 'DIRECTLY_AFFILIATED'),
(4, 'SECURITY', '保卫处', 'ADMINISTRATIVE'),
(5, 'EE', '电子工程学院', 'COLLEGE');

-- -----------------------------------------------------
-- Table "users"
-- -----------------------------------------------------
-- 注意: password_hash 是默认密码 'Changeme123!' 的SM3哈希值。phone_number 应替换为实际的加密数据。
INSERT INTO "users" ("user_id", "username", "password_hash", "full_name", "department_id", "phone_number", "role", "password_change_required") VALUES
(1, 'sysadmin', '667c756cf9334e328a56e44e906245c8e214c655a160f18fdb84d79c209c49cf', '系统管理员', NULL, 'jh/jkAtuA1NYfIX5mxDOwg==', 'SYSTEM_ADMIN', TRUE),
(2, 'schooladmin', '667c756cf9334e328a56e44e906245c8e214c655a160f18fdb84d79c209c49cf', '学校管理员', 2, '0wMDQP0bzzyzozo8ufNH3w==', 'SCHOOL_ADMIN', TRUE),
(3, 'cs_admin', '667c756cf9334e328a56e44e906245c8e214c655a160f18fdb84d79c209c49cf', '计算机学院管理员', 1, 'BgvXXXt6KoR5/rb6pgtsvg==', 'DEPARTMENT_ADMIN', TRUE),
(4, 'security_audit', '667c756cf9334e328a56e44e906245c8e214c655a160f18fdb84d79c209c49cf', '保卫处审计员', 4, '1A3B8YS3xrQUnJx3skMnCQ==', 'AUDIT_ADMIN', TRUE),
(5, 'ee_admin', '667c756cf9334e328a56e44e906245c8e214c655a160f18fdb84d79c209c49cf', '电子工程学院管理员', 5, 'KeJ2BRM1pVP45dax6LvcfQ==', 'DEPARTMENT_ADMIN', TRUE);

-- -----------------------------------------------------
-- Table "appointments"
-- -----------------------------------------------------
-- 注意: applicant_id_card 和 applicant_phone 应替换为实际的加密数据。

-- 预约 1: 公务来访, 待审批
INSERT INTO "appointments" 
("appointment_id", "appointment_type", "campus", "entry_datetime", "applicant_organization", "applicant_name", "applicant_id_card", "applicant_phone", "transport_mode", "license_plate", "official_visit_department_id", "official_visit_contact_person", "visit_reason", "status")
VALUES
(1, 'OFFICIAL', '主校区', '2025-07-01 09:00:00', 'ABC科技有限公司', '张三', 'DGx+AtnMLAQ7wkXp6xYSsKtod3k5U89fhbDeLXP2jhg=', 'TjVjrLTh9+bAa3uG6AWxjw==', 'CAR', '京A88888', 1, '李老师', '进行校企合作技术交流', 'PENDING_APPROVAL');

-- 预约 2: 个人来访, 已批准
INSERT INTO "appointments" 
("appointment_id", "appointment_type", "campus", "entry_datetime", "applicant_name", "applicant_id_card", "applicant_phone", "transport_mode", "visit_reason", "status", "approved_by_user_id", "approval_datetime", "qr_code_data", "qr_code_generated_at")
VALUES
(2, 'PUBLIC', '南校区', '2025-07-02 14:30:00', '李四', 'L6+NVaIK8m60okxNf4qgu31xTHPRT8NmU0zzKv00GtE=', 'FuBtJl2KH8Jru5W79L5Nhg==', 'WALK', '参观校园', 'APPROVED', 3, '2025-06-28 10:00:00', 'qr_code_placeholder_data_1', '2025-06-28 10:00:05');

-- 预约 3: 公务来访, 已拒绝
INSERT INTO "appointments" 
("appointment_id", "appointment_type", "campus", "entry_datetime", "applicant_organization", "applicant_name", "applicant_id_card", "applicant_phone", "transport_mode", "license_plate", "official_visit_department_id", "official_visit_contact_person", "visit_reason", "status", "approved_by_user_id", "approval_datetime")
VALUES
(3, 'OFFICIAL', '主校区', '2025-07-03 10:00:00', 'XYZ物流公司', '王五', '2BzfFndzFy6xfgqh5C/nKw1tYfPY6tg/w3AA5HsuoMA=', 'yCcTow8s6LXz8ylpPIDL4Q==', 'CAR', '沪B99999', 2, '赵主任', '洽谈物流服务合作，但材料不全', 'REJECTED', 2, '2025-06-28 11:00:00');

-- 预约 4: 个人来访, 有随行人员, 已批准
INSERT INTO "appointments" 
("appointment_id", "appointment_type", "campus", "entry_datetime", "applicant_name", "applicant_id_card", "applicant_phone", "transport_mode", "visit_reason", "status", "approved_by_user_id", "approval_datetime", "qr_code_data", "qr_code_generated_at")
VALUES
(4, 'PUBLIC', '主校区', '2025-07-05 10:00:00', '赵六', 'cT67WD2FzjYSd9eGdd+nXoQQTpfJiN88QYSxbOODJ0Q=', 'Br4+Cy3rMc/CB1mdAi+MXQ==', 'WALK', '带家人参观校园', 'APPROVED', 3, '2025-06-29 09:30:00', 'qr_code_placeholder_data_2', '2025-06-29 09:30:05');


-- -----------------------------------------------------
-- Table "accompanying_persons"
-- -----------------------------------------------------
-- 注意: id_card 和 phone 应替换为实际的加密数据。
-- 预约 4 的随行人员
INSERT INTO "accompanying_persons" ("appointment_id", "name", "id_card", "phone") VALUES
(4, '赵六的妻子', 'JJH6XJKKrICGSa6kHgGdJZ8eBbCHB9JUO4ZubNzfyZY=', 'HUipLLqDOy+GCv224koJng=='),
(4, '赵六的儿子', 'oPjLgbxKBCern8JIzHhVTrpVxhp4XJYHCQFPFEDKg4Y=', 'qKAg3wNP7orCxWXuR131vA==');


-- -----------------------------------------------------
-- Table "audit_logs"
-- -----------------------------------------------------
-- 注意: hmac_sm3_hash 是选做项, 此处留空 (NULL).
INSERT INTO "audit_logs" ("user_id", "username", "action_type", "target_entity", "target_entity_id", "details", "ip_address") VALUES
(2, 'schooladmin', 'USER_LOGIN', 'users', 2, '用户 schooladmin 登录成功', '192.168.1.10'),
(3, 'cs_admin', 'APPROVE_APPOINTMENT', 'appointments', 2, '批准了李四的个人预约', '192.168.1.20'),
(2, 'schooladmin', 'REJECT_APPOINTMENT', 'appointments', 3, '拒绝了王五的公务预约，原因：材料不全', '192.168.1.10'),
(1, 'sysadmin', 'CREATE_USER', 'users', 5, '创建了新用户 ee_admin', '127.0.0.1');
