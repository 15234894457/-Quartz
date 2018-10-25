package com.example.demo.good.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.good.entity.GoodInfoEntity;


public interface GoodInfoRepository
    extends JpaRepository<GoodInfoEntity,Long>
{
}
