package cn.starhelix.material.erp;

import cn.starhelix.material.erp.current.CurrentErpGateway;

public final class ErpGatewayProvider {
    private static volatile ErpGateway gateway;

    private ErpGatewayProvider() {
    }

    public static ErpGateway getGateway() {
        if (gateway != null) {
            return gateway;
        }

        synchronized (ErpGatewayProvider.class) {
            if (gateway == null) {
                // 未来接入另一家 ERP 时，只需要把这里切换成新的实现类。
                gateway = new CurrentErpGateway();
            }
        }

        return gateway;
    }

    public static void reset() {
        synchronized (ErpGatewayProvider.class) {
            // 当服务器地址或端口切换后，网关实例也要一起失效重建。
            gateway = null;
        }
    }
}
