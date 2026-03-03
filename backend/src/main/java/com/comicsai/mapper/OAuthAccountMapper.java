package com.comicsai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comicsai.model.entity.OAuthAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OAuthAccountMapper extends BaseMapper<OAuthAccount> {
}
