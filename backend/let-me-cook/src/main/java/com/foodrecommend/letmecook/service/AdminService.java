package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.admin.*;

public interface AdminService {
    
    AdminLoginResponse login(AdminLoginRequest request, String ip);
    
    AdminProfileDTO getProfile(Integer adminId);
    
    void updatePassword(Integer adminId, ChangePasswordRequest request);
    
    PageResult<UserDTO> getUsers(int page, int pageSize, String keyword, Integer status);
    
    UserDTO getUserById(Integer id);
    
    UserDTO createUser(CreateUserRequest request);
    
    void updateUser(Integer id, UpdateUserRequestAdmin request);
    
    void deleteUser(Integer id);
    
    void batchDeleteUsers(Integer[] ids);
    
    void updateUserStatus(Integer id, Integer status);
    
    void resetUserPassword(Integer id, String password);
    
    StatisticsOverviewDTO getOverview();
    
    StatisticsDTO getUserStatistics(String type, String startTime, String endTime);
    
    StatisticsDTO getRecipeStatistics(String type, String startTime, String endTime);
    
    StatisticsDTO getCommentStatistics(String type, String startTime, String endTime);
}
