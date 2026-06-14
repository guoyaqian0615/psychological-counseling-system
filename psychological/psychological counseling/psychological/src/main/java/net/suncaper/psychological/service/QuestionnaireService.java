package net.suncaper.psychological.service;

import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.dto.QuestionnaireSubmitDTO;

public interface QuestionnaireService {
    Result<?> submit(QuestionnaireSubmitDTO dto);
}