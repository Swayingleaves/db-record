-- 数据库结构导出
-- 版本: test-debug-v1
-- 版本ID: 18
-- 导出时间: Thu Jul 17 23:18:57 CST 2025

-- 数据库信息
-- 数据库名: db_record_test
-- 字符集: UTF8
-- 排序规则: en_US.utf8

-- 创建Schema

-- Schema: public (1个表)
-- ==================================================

-- 表: public.book (图书基础信息表)
DROP TABLE IF EXISTS "public"."book" CASCADE;
CREATE TABLE "public"."book" (
  "id" INTEGER NOT NULL DEFAULT 'nextval('book_id_seq'::regclass)',
  "title" VARCHAR(255) NOT NULL,
  "author" VARCHAR(255) NOT NULL,
  "isbn" VARCHAR(20),
  "publication_year" INTEGER,
  "publisher" VARCHAR(255),
  "genre" VARCHAR(100),
  "pages" INTEGER,
  "language" VARCHAR(50),
  "price" NUMERIC(10,2),
  "stock" INTEGER DEFAULT '0',
  "status" VARCHAR(20) DEFAULT ''available'::character varying',
  "created_at" TIMESTAMP DEFAULT 'CURRENT_TIMESTAMP',
  "updated_at" TIMESTAMP DEFAULT 'CURRENT_TIMESTAMP'

);
COMMENT ON TABLE "book" IS '图书基础信息表';;
