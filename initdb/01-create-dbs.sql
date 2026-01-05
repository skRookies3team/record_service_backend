-- 1. 데이터베이스 생성
CREATE DATABASE diary_db;
CREATE DATABASE user_db;
CREATE DATABASE social_db;
CREATE DATABASE mate_db;

-- 2. diary_db로 접속 전환 (메타 명령어)
\c diary_db

-- 3. PostGIS 확장 기능 활성화 (diary_db에만 적용됨)
CREATE EXTENSION IF NOT EXISTS postgis;

-- 4. 테이블 생성 (JPA ddl-auto 설정과 상관없이 초기 데이터 삽입을 위해 미리 생성)
CREATE TABLE IF NOT EXISTS weather_stations (
                                                station_id INT PRIMARY KEY,
                                                name VARCHAR(100),
    location GEOMETRY(Point, 4326)
    );

-- 5. 사용자 위치 경로 저장 테이블 추가 (전날 위치 조회를 위해 필수)
CREATE TABLE IF NOT EXISTS walk_routes (
                                           id SERIAL PRIMARY KEY,
                                           user_id BIGINT NOT NULL,
                                           start_point GEOMETRY(Point, 4326) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- 날짜별 조회를 최적화하기 위한 인덱스
    date_only DATE GENERATED ALWAYS AS (created_at::DATE) STORED
    );
CREATE INDEX IF NOT EXISTS idx_walk_routes_user_date ON walk_routes(user_id, date_only);

-- 6. 관측소 초기 데이터 삽입
-- ON CONFLICT를 사용하여 중복 삽입 에러를 방지합니다.
-- 전국 주요 거점 및 경기권 상세 관측소 데이터 (25개소)
INSERT INTO weather_stations (station_id, name, location) VALUES
                                                              -- 수도권
                                                              (108, '서울', ST_SetSRID(ST_MakePoint(126.9658, 37.5714), 4326)),
                                                              (112, '인천', ST_SetSRID(ST_MakePoint(126.7073, 37.4527), 4326)),
                                                              (119, '수원', ST_SetSRID(ST_MakePoint(127.0219, 37.2574), 4326)),
                                                              (116, '관악산', ST_SetSRID(ST_MakePoint(126.9570, 37.4433), 4326)),
                                                              (203, '이천', ST_SetSRID(ST_MakePoint(127.4849, 37.2640), 4326)),
                                                              (99,  '파주', ST_SetSRID(ST_MakePoint(126.7665, 37.8867), 4326)),
                                                              (202, '양평', ST_SetSRID(ST_MakePoint(127.4915, 37.4880), 4326)),
                                                              (102, '백령도', ST_SetSRID(ST_MakePoint(124.6300, 37.9500), 4326)),
                                                              -- 강원권
                                                              (101, '춘천', ST_SetSRID(ST_MakePoint(127.7306, 37.8858), 4326)),
                                                              (105, '강릉', ST_SetSRID(ST_MakePoint(128.8910, 37.7515), 4326)),
                                                              (114, '원주', ST_SetSRID(ST_MakePoint(127.9466, 37.3375), 4326)),
                                                              (121, '속초', ST_SetSRID(ST_MakePoint(128.5910, 38.2509), 4326)),
                                                              -- 충청권
                                                              (131, '청주', ST_SetSRID(ST_MakePoint(127.4407, 36.6392), 4326)),
                                                              (133, '대전', ST_SetSRID(ST_MakePoint(127.3721, 36.3720), 4326)),
                                                              (129, '서산', ST_SetSRID(ST_MakePoint(126.4477, 36.7766), 4326)),
                                                              (177, '홍성', ST_SetSRID(ST_MakePoint(126.6870, 36.6570), 4326)),
                                                              -- 경상권
                                                              (143, '대구', ST_SetSRID(ST_MakePoint(128.6014, 35.8779), 4326)),
                                                              (159, '부산', ST_SetSRID(ST_MakePoint(129.0324, 35.1047), 4326)),
                                                              (152, '울산', ST_SetSRID(ST_MakePoint(129.3347, 35.5825), 4326)),
                                                              (138, '포항', ST_SetSRID(ST_MakePoint(129.3796, 36.0320), 4326)),
                                                              (192, '진주', ST_SetSRID(ST_MakePoint(128.1201, 35.1637), 4326)),
                                                              -- 전라권
                                                              (146, '전주', ST_SetSRID(ST_MakePoint(127.1550, 35.8215), 4326)),
                                                              (156, '광주', ST_SetSRID(ST_MakePoint(126.8916, 35.1729), 4326)),
                                                              (165, '목포', ST_SetSRID(ST_MakePoint(126.3812, 34.8172), 4326)),
                                                              (168, '여수', ST_SetSRID(ST_MakePoint(127.7300, 34.7300), 4326)),
                                                              -- 제주권
                                                              (184, '제주', ST_SetSRID(ST_MakePoint(126.5297, 33.5141), 4326))
    ON CONFLICT (station_id) DO NOTHING;