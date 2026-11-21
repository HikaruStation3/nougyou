package com.nougyou.system.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.security.SecurityUtils;
import com.nougyou.system.dto.UserForm;
import com.nougyou.system.dto.UserFormVO;
import com.nougyou.system.dto.UserInfoVO;
import com.nougyou.system.dto.UserItemVO;
import com.nougyou.system.dto.UserProfileVO;
import com.nougyou.system.dto.UserQuery;
import com.nougyou.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService userService;

    @GetMapping
    public ApiResponse<PageResult<UserItemVO>> page(UserQuery query) {
        return ApiResponse.ok(userService.page(query));
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoVO> me() {
        return ApiResponse.ok(userService.currentUserInfo(SecurityUtils.currentUser().userId()));
    }

    @GetMapping("/{id}/form")
    public ApiResponse<UserFormVO> form(@PathVariable Long id) {
        return ApiResponse.ok(userService.getForm(id));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody UserForm form) {
        userService.create(form);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody UserForm form) {
        userService.update(id, form);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/password/reset")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        userService.resetPassword(id, password);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{ids}")
    public ApiResponse<Void> delete(@PathVariable String ids) {
        userService.deleteByIds(ids);
        return ApiResponse.ok();
    }

    @GetMapping("/options")
    public ApiResponse<List<Map<String, Object>>> options() {
        return ApiResponse.ok(userService.options());
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileVO> profile() {
        return ApiResponse.ok(userService.getProfile(SecurityUtils.currentUser().userId()));
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@RequestBody UserForm form) {
        userService.updateProfile(SecurityUtils.currentUser().userId(), form);
        return ApiResponse.ok();
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@RequestBody Map<String, String> body) {
        userService.changePassword(SecurityUtils.currentUser().userId(),
                body.get("oldPassword"), body.get("newPassword"), body.get("confirmPassword"));
        return ApiResponse.ok();
    }
}
