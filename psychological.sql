/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : psychological

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 04/06/2026 10:16:31
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for closing_report
-- ----------------------------
DROP TABLE IF EXISTS `closing_report`;
CREATE TABLE `closing_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `counseling_id` bigint NULL DEFAULT NULL,
  `student_id` bigint NULL DEFAULT NULL,
  `student_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `department` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `problem_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `total_times` int NULL DEFAULT NULL,
  `counselor_id` bigint NULL DEFAULT NULL,
  `counselor_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `start_date` date NULL DEFAULT NULL,
  `end_date` date NULL DEFAULT NULL,
  `conclusion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of closing_report
-- ----------------------------
INSERT INTO `closing_report` VALUES (1, 1, 2, '王佳', '女', '计算机学院', '13511110001', '学业压力', 8, 38, '梁曼妮', '2024-05-01', '2024-06-20', '学生学业压力明显缓解，能够合理安排学习时间，情绪状态稳定，建议定期回访。', '2024-06-21 10:00:00');
INSERT INTO `closing_report` VALUES (2, 2, 3, '李明', '男', '文学院', '13511110002', '人际关系', 8, 38, '梁曼妮', '2024-05-01', '2024-06-20', '学生人际关系问题得到有效解决，沟通能力显著提升，能够建立健康的社交关系。', '2024-06-21 11:00:00');
INSERT INTO `closing_report` VALUES (3, 5, 4, '张婷', '女', '经管学院', '13511110003', '职业规划', 8, 38, '梁曼妮', '2024-05-03', '2024-06-22', '学生明确了职业发展方向，制定了可行的行动计划，自信心显著提升，咨询目标达成。', '2024-06-23 09:00:00');

-- ----------------------------
-- Table structure for counseling
-- ----------------------------
DROP TABLE IF EXISTS `counseling`;
CREATE TABLE `counseling`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NULL DEFAULT NULL,
  `student_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `counselor_id` bigint NULL DEFAULT NULL,
  `counselor_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `total_weeks` int NULL DEFAULT 8,
  `start_date` date NULL DEFAULT NULL,
  `counseling_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of counseling
-- ----------------------------
INSERT INTO `counseling` VALUES (1, 2, '学生041', 3, '咨询师20', 8, '2026-06-01', '每周一 10:00-10:30', '心理咨询室101', '进行中');

-- ----------------------------
-- Table structure for counseling_record
-- ----------------------------
DROP TABLE IF EXISTS `counseling_record`;
CREATE TABLE `counseling_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `counseling_id` bigint NULL DEFAULT NULL,
  `times` int NULL DEFAULT NULL,
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `record` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of counseling_record
-- ----------------------------
INSERT INTO `counseling_record` VALUES (1, 1, 1, '完成', '首次咨询，建立关系，了解基本情况');
INSERT INTO `counseling_record` VALUES (2, 1, 2, '完成', '深入探讨学业压力来源，制定初步应对方案');
INSERT INTO `counseling_record` VALUES (3, 1, 3, '完成', '调整应对策略，练习时间管理技巧');
INSERT INTO `counseling_record` VALUES (4, 1, 4, '完成', '请假一次，电话沟通');
INSERT INTO `counseling_record` VALUES (5, 1, 5, '完成', '回归咨询，评估策略效果');
INSERT INTO `counseling_record` VALUES (6, 1, 6, '完成', '巩固效果，预防复发');
INSERT INTO `counseling_record` VALUES (7, 1, 7, '完成', '总结咨询成果，制定长期规划');
INSERT INTO `counseling_record` VALUES (8, 1, 8, '结案', '咨询结束，学生学业压力明显缓解');
INSERT INTO `counseling_record` VALUES (9, 2, 1, '完成', '首次咨询，了解人际关系问题');
INSERT INTO `counseling_record` VALUES (10, 2, 2, '完成', '分析社交模式，识别负面认知');
INSERT INTO `counseling_record` VALUES (11, 2, 3, '旷约', '未按时到场，事后电话提醒');
INSERT INTO `counseling_record` VALUES (12, 2, 4, '完成', '学习沟通技巧，角色扮演练习');
INSERT INTO `counseling_record` VALUES (13, 2, 5, '完成', '应用沟通技巧，反馈效果');
INSERT INTO `counseling_record` VALUES (14, 2, 6, '完成', '处理社交挫折，强化积极体验');
INSERT INTO `counseling_record` VALUES (15, 2, 7, '完成', '总结进步，制定社交目标');
INSERT INTO `counseling_record` VALUES (16, 2, 8, '结案', '咨询结束，人际关系明显改善');
INSERT INTO `counseling_record` VALUES (17, 3, 1, '完成', '首次咨询，评估情绪状态');
INSERT INTO `counseling_record` VALUES (18, 3, 2, '完成', '学习情绪调节技巧');
INSERT INTO `counseling_record` VALUES (19, 3, 3, '完成', '应用技巧，记录情绪变化');
INSERT INTO `counseling_record` VALUES (20, 3, 4, '完成', '深入探讨情绪根源');
INSERT INTO `counseling_record` VALUES (21, 3, 5, '完成', '调整调节策略，效果良好');
INSERT INTO `counseling_record` VALUES (22, 3, 6, '未开始', '待进行');
INSERT INTO `counseling_record` VALUES (23, 3, 7, '未开始', '待进行');
INSERT INTO `counseling_record` VALUES (24, 3, 8, '未开始', '待进行');

-- ----------------------------
-- Table structure for duty
-- ----------------------------
DROP TABLE IF EXISTS `duty`;
CREATE TABLE `duty`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL,
  `duty_date` date NULL DEFAULT NULL,
  `start_time` time NULL DEFAULT NULL,
  `end_time` time NULL DEFAULT NULL,
  `status` int NULL DEFAULT 1,
  `max_person` int NOT NULL DEFAULT 1 COMMENT '该时段最大预约人数',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_duty_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_duty_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of duty
-- ----------------------------
INSERT INTO `duty` VALUES (1, 14, '2026-06-15', '10:34:00', '11:04:00', 0, 3, '');
INSERT INTO `duty` VALUES (2, 12, '2026-06-09', '10:00:00', '10:30:00', 0, 3, '');
INSERT INTO `duty` VALUES (3, 15, '2026-06-12', '10:00:00', '10:30:00', 0, 3, '');
INSERT INTO `duty` VALUES (4, 38, '2026-06-09', '10:30:00', '11:00:00', 0, 3, '');
INSERT INTO `duty` VALUES (5, 38, '2026-06-16', '10:30:00', '11:00:00', 0, 3, '');
INSERT INTO `duty` VALUES (6, 38, '2026-06-23', '10:30:00', '11:00:00', 0, 3, '');
INSERT INTO `duty` VALUES (7, 38, '2026-06-30', '10:30:00', '11:00:00', 0, 3, '');
INSERT INTO `duty` VALUES (8, 38, '2026-07-07', '10:30:00', '11:00:00', 0, 3, '');
INSERT INTO `duty` VALUES (9, 38, '2026-07-14', '10:30:00', '11:00:00', 0, 3, '');
INSERT INTO `duty` VALUES (10, 38, '2026-07-21', '10:30:00', '11:00:00', 0, 3, '');
INSERT INTO `duty` VALUES (11, 38, '2026-07-28', '10:30:00', '11:00:00', 0, 3, '');
INSERT INTO `duty` VALUES (12, 12, '2026-06-12', '10:13:00', '10:43:00', 0, 3, '');

-- ----------------------------
-- Table structure for extra_apply
-- ----------------------------
DROP TABLE IF EXISTS `extra_apply`;
CREATE TABLE `extra_apply`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `counseling_id` bigint NULL DEFAULT NULL,
  `student_id` bigint NULL DEFAULT NULL,
  `counselor_id` bigint NULL DEFAULT NULL,
  `apply_time` datetime NULL DEFAULT NULL,
  `extra_weeks` int NULL DEFAULT NULL,
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `admin_remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of extra_apply
-- ----------------------------
INSERT INTO `extra_apply` VALUES (1, 1, 5, 33, '2026-06-03 22:39:45', 8, '还需继续观察', '已通过', '可以添加', '2026-06-03 22:48:46');
INSERT INTO `extra_apply` VALUES (4, 2, 6, 32, '2026-06-03 22:47:38', 8, '还需继续观察', '已拒绝', '不建议追加', '2026-06-03 23:20:22');
INSERT INTO `extra_apply` VALUES (5, 3, 7, 34, '2026-06-03 22:48:12', 8, '还需继续观察', '待审批', NULL, NULL);

-- ----------------------------
-- Table structure for first_visit
-- ----------------------------
DROP TABLE IF EXISTS `first_visit`;
CREATE TABLE `first_visit`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NULL DEFAULT NULL,
  `student_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `apply_time` datetime NULL DEFAULT NULL,
  `questionnaire_score` int NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `visitor_id` bigint NULL DEFAULT NULL,
  `visit_date` date NULL DEFAULT NULL,
  `visit_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `is_emergency` tinyint(1) NULL DEFAULT 0,
  `is_alert` tinyint(1) NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `reschedule_time` datetime NULL DEFAULT NULL COMMENT '改约时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of first_visit
-- ----------------------------
INSERT INTO `first_visit` VALUES (1, 2, '王佳', '2026-06-03 21:34:03', 40, '已通过', 14, '2026-06-15', '10:34-11:04', '心理咨询室', 0, 0, NULL, '2026-06-03 21:35:12', NULL);
INSERT INTO `first_visit` VALUES (12, 4, '张婷', '2026-06-03 21:56:55', 10, '已完成', 14, '2026-06-15', '10:34-11:04', '教室', 0, 1, '2026-06-03 21:56:55', NULL, NULL);
INSERT INTO `first_visit` VALUES (13, 8, '周悦', '2026-06-04 09:10:07', 10, '待审核', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL);
INSERT INTO `first_visit` VALUES (14, 9, '吴帆', '2026-06-04 10:11:35', 40, '待审核', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL);
INSERT INTO `first_visit` VALUES (15, 10, '郑冉', '2026-06-04 10:12:05', 70, '待审核', NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL);
INSERT INTO `first_visit` VALUES (16, 11, '孙凯', '2026-06-04 10:12:32', 40, '已通过', 12, '2026-06-12', '10:13-10:43', '教室', 0, 0, NULL, '2026-06-04 10:14:56', NULL);

-- ----------------------------
-- Table structure for first_visit_result
-- ----------------------------
DROP TABLE IF EXISTS `first_visit_result`;
CREATE TABLE `first_visit_result`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `first_visit_id` bigint NULL DEFAULT NULL,
  `crisis_level` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `problem_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `conclusion` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of first_visit_result
-- ----------------------------
INSERT INTO `first_visit_result` VALUES (1, 1, '低风险', '学业压力', '安排咨询');
INSERT INTO `first_visit_result` VALUES (2, 2, '低风险', '人际关系', '安排咨询');
INSERT INTO `first_visit_result` VALUES (3, 3, '中风险', '情绪障碍', '安排咨询');
INSERT INTO `first_visit_result` VALUES (4, 4, '低风险', '家庭问题', '安排咨询');
INSERT INTO `first_visit_result` VALUES (5, 5, '中风险', '职业规划', '安排咨询');
INSERT INTO `first_visit_result` VALUES (6, 12, '中', '人际关系', '安排咨询');

-- ----------------------------
-- Table structure for notice
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `is_read` tinyint(1) NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notice
-- ----------------------------
INSERT INTO `notice` VALUES (1, 2, '新的结案报告', '学生张三的咨询已结案，请查看结案报告。', 0, '2024-06-21 10:10:00');
INSERT INTO `notice` VALUES (2, 3, '追加咨询申请', '你提交的学生王五追加咨询申请待审批。', 0, '2024-06-01 10:10:00');
INSERT INTO `notice` VALUES (3, 4, '咨询结案通知', '你的学业压力咨询已结案，如有需要可再次预约。', 1, '2024-06-21 10:20:00');
INSERT INTO `notice` VALUES (4, 5, '初访预约审核通过', '您的初访预约已审核通过，请按时前往。初访时间：2026-06-01 09:00-09:30，地点：心理咨询室1号', 0, '2026-06-01 08:58:42');

-- ----------------------------
-- Table structure for time_config
-- ----------------------------
DROP TABLE IF EXISTS `time_config`;
CREATE TABLE `time_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `single_duration` int NULL DEFAULT 30,
  `interval_minute` int NULL DEFAULT 10,
  `daily_start_hour` int NULL DEFAULT 9 COMMENT '排班开始小时',
  `daily_end_hour` int NULL DEFAULT 18 COMMENT '排班结束小时',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of time_config
-- ----------------------------
INSERT INTO `time_config` VALUES (1, 30, 10, 10, 18);

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学号/工号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `department` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'student/visitor/assistant/counselor/admin',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 43 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, '2023103', '123456', '郭雅倩', '男', '13800000001', '心理咨询中心', 'admin');
INSERT INTO `user` VALUES (2, '2026001', '123456', '王佳', '女', '13511110001', '计算机学院', 'student');
INSERT INTO `user` VALUES (3, '2026002', '123456', '李明', '男', '13511110002', '文学院', 'student');
INSERT INTO `user` VALUES (4, '2026003', '123456', '张婷', '女', '13511110003', '经管学院', 'student');
INSERT INTO `user` VALUES (5, '2026004', '123456', '赵浩', '男', '13511110004', '机电学院', 'student');
INSERT INTO `user` VALUES (6, '2026005', '123456', '刘雪', '女', '13511110005', '外国语学院', 'student');
INSERT INTO `user` VALUES (7, '2026006', '123456', '陈宇', '男', '13511110006', '法学院', 'student');
INSERT INTO `user` VALUES (8, '2026007', '123456', '周悦', '女', '13511110007', '医学院', 'student');
INSERT INTO `user` VALUES (9, '2026008', '123456', '吴帆', '男', '13511110008', '土木学院', 'student');
INSERT INTO `user` VALUES (10, '2026009', '123456', '郑冉', '女', '13511110009', '艺术学院', 'student');
INSERT INTO `user` VALUES (11, '2026010', '123456', '孙凯', '男', '13511110010', '化工学院', 'student');
INSERT INTO `user` VALUES (12, '4000', '123456', '郝萍', '女', '13622220001', '心理咨询中心', 'visitor');
INSERT INTO `user` VALUES (13, '4001', '123456', '朱峰', '男', '13622220002', '心理咨询中心', 'visitor');
INSERT INTO `user` VALUES (14, '4002', '123456', '曹莉', '女', '13622220003', '心理咨询中心', 'visitor');
INSERT INTO `user` VALUES (15, '4003', '123456', '彭涛', '男', '13622220004', '心理咨询中心', 'visitor');
INSERT INTO `user` VALUES (16, '4004', '123456', '薛雯', '女', '13622220005', '心理咨询中心', 'visitor');
INSERT INTO `user` VALUES (17, '4005', '123456', '高磊', '男', '13622220006', '心理咨询中心', 'visitor');
INSERT INTO `user` VALUES (18, '4006', '123456', '叶芳', '女', '13622220007', '心理咨询中心', 'visitor');
INSERT INTO `user` VALUES (19, '4007', '123456', '江波', '男', '13622220008', '心理咨询中心', 'visitor');
INSERT INTO `user` VALUES (20, '4008', '123456', '苏晴', '女', '13622220009', '心理咨询中心', 'visitor');
INSERT INTO `user` VALUES (21, '4009', '123456', '马俊', '男', '13622220010', '心理咨询中心', 'visitor');
INSERT INTO `user` VALUES (22, '5000', '123456', '陈雨桐', '女', '13510010000', '心理服务部', 'assistant');
INSERT INTO `user` VALUES (23, '5001', '123456', '周子轩', '男', '13510010001', '心理服务部', 'assistant');
INSERT INTO `user` VALUES (24, '5002', '123456', '林思瑶', '女', '13510010002', '心理服务部', 'assistant');
INSERT INTO `user` VALUES (25, '5003', '123456', '王景行', '男', '13510010003', '心理服务部', 'assistant');
INSERT INTO `user` VALUES (26, '5004', '123456', '赵若曦', '女', '13510010004', '心理服务部', 'assistant');
INSERT INTO `user` VALUES (27, '5005', '123456', '刘承宇', '男', '13510010005', '心理服务部', 'assistant');
INSERT INTO `user` VALUES (28, '5006', '123456', '高语茉', '女', '13510010006', '心理服务部', 'assistant');
INSERT INTO `user` VALUES (29, '5007', '123456', '孙泽安', '男', '13510010007', '心理服务部', 'assistant');
INSERT INTO `user` VALUES (30, '5008', '123456', '吴梦琪', '女', '13510010008', '心理服务部', 'assistant');
INSERT INTO `user` VALUES (31, '5009', '123456', '黄俊彦', '男', '13510010009', '心理服务部', 'assistant');
INSERT INTO `user` VALUES (32, '6000', '123456', '徐静娴', '女', '13620020000', '心理教研室', 'counselor');
INSERT INTO `user` VALUES (33, '6001', '123456', '马博文', '男', '13620020001', '心理教研室', 'counselor');
INSERT INTO `user` VALUES (34, '6002', '123456', '韩婉清', '女', '13620020002', '心理教研室', 'counselor');
INSERT INTO `user` VALUES (35, '6003', '123456', '程致远', '男', '13620020003', '心理教研室', 'counselor');
INSERT INTO `user` VALUES (36, '6004', '123456', '邓诗涵', '女', '13620020004', '心理教研室', 'counselor');
INSERT INTO `user` VALUES (37, '6005', '123456', '冯修远', '男', '13620020005', '心理教研室', 'counselor');
INSERT INTO `user` VALUES (38, '6006', '123456', '梁曼妮', '女', '13620020006', '心理教研室', 'counselor');
INSERT INTO `user` VALUES (39, '6007', '123456', '陆明轩', '男', '13620020007', '心理教研室', 'counselor');
INSERT INTO `user` VALUES (40, '6008', '123456', '尹若琳', '女', '13620020008', '心理教研室', 'counselor');
INSERT INTO `user` VALUES (41, '6009', '123456', '江亦辰', '男', '13620020009', '心理教研室', 'counselor');
INSERT INTO `user` VALUES (42, '5010', '999028872cfff7ae8ee330a33cbd3874', '徐江海', '女', '12345678950', '心理服务部', 'counselor');

SET FOREIGN_KEY_CHECKS = 1;
