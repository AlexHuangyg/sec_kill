package com.debug.kill.server.service.impl;

import com.debug.kill.model.entity.ItemKill;
import com.debug.kill.model.mapper.ItemKillMapper;
import com.debug.kill.server.service.IItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService implements IItemService {
    private static final Logger log= LoggerFactory.getLogger(ItemService.class);
    @Autowired
    private ItemKillMapper itemKillMapper;
    @Override
    public List<ItemKill> getItemKill() {
        return itemKillMapper.selectAll();
    }

    @Override
    public ItemKill getKillDetail(Integer id) throws Exception {
        ItemKill itemKill = itemKillMapper.selectById(id);
        if(itemKill==null){
            log.error("未查询到商品");
            throw new Exception("待秒杀商品不存在");

        }
        return itemKill;
    }
}
