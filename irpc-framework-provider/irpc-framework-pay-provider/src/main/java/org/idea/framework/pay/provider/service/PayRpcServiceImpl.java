package org.idea.framework.pay.provider.service;

import org.idea.irpc.framework.interfaces.pay.PayRpcService;
import org.idea.irpc.framework.spring.starter.common.IRpcService;

import java.util.Arrays;
import java.util.List;

/**
 * @Author linhao
 * @Date created in 10:58 上午 2022/3/19
 */
@IRpcService
public class PayRpcServiceImpl implements PayRpcService {

    @Override
    public boolean doPay() {
        return true;
    }

    @Override
    public List<String> getPayHistoryByGoodNo(String goodNo) {
        return Arrays.asList(goodNo + "-pay-001", goodNo + "-pay-002");
    }

}
