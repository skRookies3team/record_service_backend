package com.petlog.record.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "RECAP_HIGHLIGHTS")
public class RecapHighlight {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long highlightId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recap_id")
    private Recap recap;

    private String title;   // 예: "첫 눈 산책"
    private String content; // 예: "생애 처음 보는 눈에 신나서..."

    public void setRecap(Recap recap) {
        this.recap = recap;
    }
}