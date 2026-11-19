package com.ecommerce.order_service.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserRequest {

    @GetMapping("/api/auth/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);
}
