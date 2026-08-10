package com.zcommcx.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AllowedAccountRepository extends JpaRepository<AllowedAccount, String> {
}
