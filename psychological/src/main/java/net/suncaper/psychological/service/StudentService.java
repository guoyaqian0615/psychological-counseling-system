package net.suncaper.psychological.service;

import net.suncaper.psychological.common.Result;
import net.suncaper.psychological.entity.FirstVisit;
import net.suncaper.psychological.entity.User;

import java.util.List;

public interface StudentService {
    Result<User> login(User user);
    Result<Void> submitFirstVisit(FirstVisit firstVisit);
    Result<List<FirstVisit>> myFirstVisits(Long studentId);
    Result<Void> cancelVisit(Long id);
}