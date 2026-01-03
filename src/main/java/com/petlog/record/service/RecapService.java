package com.petlog.record.service;

import com.petlog.record.dto.request.RecapRequest;
import com.petlog.record.dto.response.RecapResponse;
import java.util.List;

public interface RecapService {
    /**
     * AI를 활용하여 월간 리캡을 생성합니다.
     */
    Long createAiRecap(RecapRequest.Generate request);

    Long createWaitingRecap(RecapRequest.Create request); // 추가

    RecapResponse.Detail getRecap(Long recapId);

    List<RecapResponse.Simple> getAllRecaps(Long userId);

    List<RecapResponse.Simple> getRecapsByPet(Long petId);
}