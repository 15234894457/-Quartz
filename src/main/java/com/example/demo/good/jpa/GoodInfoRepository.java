package com.example.demo.good.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.good.entity.GoodInfoEntity;

/**
 * ========================
 * Created with IntelliJ IDEA.
 * Date：2017/11/5
 * Time：14:55
 * 码云：http://git.oschina.net/jnyqy
 * ========================
 * @author 恒宇少年
 */
public interface GoodInfoRepository
    extends JpaRepository<GoodInfoEntity,Long>
{
}
