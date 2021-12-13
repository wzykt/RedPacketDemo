package com.example.service;

import com.example.entity.RedPacket;
import com.example.util.Result;

public interface RedPacketService {

    /**
     * 获取红包
     *
     * @param redPacketId
     * @return
     */
    RedPacket get(long redPacketId);


    /**
     * 微信抢红包业务实现
     *
     * @param redPacketId
     * @param userId
     * @return
     */
    Result startTwoSeckil(long redPacketId, int userId);


    /**
     * 红包初始化
     *
     * @param redPacketId
     * @param money
     * @param number
     * @return
     */
    Result redPacketInit(long redPacketId, long money, long number);

}
