package com.debug.kill.server.service;

import com.debug.kill.model.entity.ItemKill;

import java.util.List;

public interface IItemService {
    List<ItemKill> getItemKill() throws Exception;
    ItemKill getKillDetail(Integer id) throws Exception;
}
