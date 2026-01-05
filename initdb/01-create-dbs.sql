-- 1. 데이터베이스 생성
CREATE DATABASE diary_db;
CREATE DATABASE user_db;
CREATE DATABASE social_db;
CREATE DATABASE mate_db;

-- 2. diary_db로 접속 전환 (메타 명령어)
\c diary_db

-- 3. PostGIS 확장 기능 활성화 (diary_db에만 적용됨)
CREATE EXTENSION IF NOT EXISTS postgis;


-- 4. 관측소 데이터 추가
INSERT INTO weather_stations (station_id, name, location) VALUES
                                                              (108, '서울', ST_SetSRID(ST_MakePoint(126.9658, 37.5714), 4326)),
                                                              (112, '인천', ST_SetSRID(ST_MakePoint(126.7073, 37.4527), 4326)),
                                                              (119, '수원', ST_SetSRID(ST_MakePoint(127.0219, 37.2574), 4326)),
                                                              (143, '대구', ST_SetSRID(ST_MakePoint(128.6014, 35.8779), 4326)),
                                                              (159, '부산', ST_SetSRID(ST_MakePoint(129.0324, 35.1047), 4326)),
                                                              (184, '제주', ST_SetSRID(ST_MakePoint(126.5297, 33.5141), 4326));
-- 필요한 만큼 더 추가...
