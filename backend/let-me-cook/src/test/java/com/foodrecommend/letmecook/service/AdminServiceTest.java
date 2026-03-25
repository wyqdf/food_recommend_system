package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.admin.AdminLoginRequest;
import com.foodrecommend.letmecook.dto.admin.AdminLoginResponse;
import com.foodrecommend.letmecook.dto.admin.AdminProfileDTO;
import com.foodrecommend.letmecook.dto.admin.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AdminServiceTest {
    
    @Autowired
    private AdminService adminService;
    
    @Test
    void testLoginSuccess() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");
        
        AdminLoginResponse response = adminService.login(request, "127.0.0.1");
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getAdmin());
        assertEquals("admin", response.getAdmin().getUsername());
    }
    
    @Test
    void testGetProfile() {
        AdminProfileDTO profile = adminService.getProfile(1);
        assertNotNull(profile);
        assertEquals("admin", profile.getUsername());
    }
    
    @Test
    void testGetUsers() {
        PageResult<UserDTO> result = adminService.getUsers(1, 10, null, null);
        assertNotNull(result);
        assertNotNull(result.getList());
        assertTrue(result.getTotal() >= 0);
    }
}
