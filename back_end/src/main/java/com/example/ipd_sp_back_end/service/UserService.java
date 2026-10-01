package com.example.ipd_sp_back_end.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ipd_sp_back_end.entity.AccountUserBinding;
import com.example.ipd_sp_back_end.entity.User;
import com.example.ipd_sp_back_end.mapper.AccountUserBindingMapper;
import com.example.ipd_sp_back_end.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    private final AccountUserBindingMapper accountUserBindingMapper;

    public UserService(AccountUserBindingMapper accountUserBindingMapper) {
        this.accountUserBindingMapper = accountUserBindingMapper;
    }

    public Optional<User> getCurrentUser(Integer accountId) {
        Integer userId = resolveUserId(accountId);
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(getById(userId));
    }

    public User getCurrentUserOrThrow(Integer accountId) {
        return getCurrentUser(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found. Please create your profile first."));
    }

    public List<User> getCurrentUserAsList(Integer accountId) {
        return getCurrentUser(accountId).map(List::of).orElseGet(List::of);
    }

    @Transactional
    public User upsertCurrentUser(Integer accountId, User userDetails) {
        Optional<User> existing = getCurrentUser(accountId);
        if (existing.isPresent()) {
            User user = existing.get();
            applyProfileFields(user, userDetails);
            updateById(user);
            return user;
        }

        User user = new User();
        applyProfileFields(user, userDetails);
        save(user);

        AccountUserBinding binding = new AccountUserBinding();
        binding.setAccountId(accountId);
        binding.setUserId(user.getId());
        accountUserBindingMapper.insert(binding);
        return user;
    }

    @Transactional
    public void deleteCurrentUser(Integer accountId) {
        Integer userId = resolveUserId(accountId);
        if (userId == null) {
            return;
        }
        accountUserBindingMapper.deleteById(accountId);
        removeById(userId);
    }

    public Integer requireCurrentUserId(Integer accountId) {
        Integer userId = resolveUserId(accountId);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please create your profile first.");
        }
        return userId;
    }

    private void applyProfileFields(User user, User userDetails) {
        user.setAge(userDetails.getAge());
        user.setGender(userDetails.getGender());
        user.setHeight(userDetails.getHeight());
        user.setWeight(userDetails.getWeight());
    }

    private Integer resolveUserId(Integer accountId) {
        AccountUserBinding binding = accountUserBindingMapper.selectById(accountId);
        if (binding != null && binding.getUserId() != null) {
            return binding.getUserId();
        }
        return null;
    }
}
