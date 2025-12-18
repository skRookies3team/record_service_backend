-- 1. 데이터베이스 생성
CREATE DATABASE diary_db;
CREATE DATABASE user_db;
CREATE DATABASE social_db;

-- 2. diary_db로 접속 전환 (메타 명령어)
\c diary_db

-- 3. PostGIS 확장 기능 활성화 (diary_db에만 적용됨)
CREATE EXTENSION IF NOT EXISTS postgis;