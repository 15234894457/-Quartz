package com.example.demo.good.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.good.entity.GoodInfoEntity;
import com.example.demo.good.service.GoodInfoService;


@RestController
@RequestMapping(value = "/good")
public class GoodController
{
    /**
     * 业务逻辑实现
     */
    @Autowired
    private GoodInfoService goodInfoService;
    /**
     * 添加
     * @return
     */
    @RequestMapping(value = "/save")
    public Long save(GoodInfoEntity good) throws Exception
    {
        return goodInfoService.saveGood(good);
    }
}