package com.petlog.record.service;

import com.petlog.record.entity.Diary;

public interface DiaryVectorService {
    void saveToVectorDB(Diary diary);
}