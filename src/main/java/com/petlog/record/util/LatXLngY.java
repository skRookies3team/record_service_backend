package com.petlog.record.util;

/**
 * [기상청 격자 좌표 변환 유틸리티]
 * WGS84 지리 좌표(위경도)를 기상청 단기예보 구역인 격자 좌표(nx, ny)로 변환합니다.
 * 이 알고리즘은 램버트 정각 원추 투영법(Lambert Conformal Conic Projection)을 기반으로 합니다.
 */
public class LatXLngY {

    /**
     * 위경도 좌표를 기상청 격자 좌표로 변환
     * * @param lat 변환할 위도(Latitude)
     * @param lng 변환할 경도(Longitude)
     * @return int[]{nx, ny} (기상청 격자 X, Y 좌표)
     */
    public static int[] convert(double lat, double lng) {
        // 기상청 투영 상수
        double RE = 6371.00877; // 지구 반경(km)
        double GRID = 5.0; // 격자 간격(km)
        double SLAT1 = 30.0; // 투영 위도1(degree)
        double SLAT2 = 60.0; // 투영 위도2(degree)
        double OLON = 126.0; // 기준점 경도(degree)
        double OLAT = 38.0; // 기준점 위도(degree)
        double XO = 43; // 기준점 X좌표(GRID)
        double YO = 136; // 기준점 Y좌표(GRID)

        // 변환 로직 (LCC DFT)
        double DEGRAD = Math.PI / 180.0; // 도(Degree)를 라디안(Radian)으로 변환하는 상수
        
        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + (lat) * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = lng * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        return new int[]{nx, ny};
    }
}