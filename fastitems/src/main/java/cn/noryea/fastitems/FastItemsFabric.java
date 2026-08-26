package cn.noryea.fastitems;

import cn.noryea.fastitems.config.FastItemsConfig;
import dev.henny.hugoutils.client.ui.ConfigPages;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class FastItemsFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FastItemsConfig.init(FabricLoader.getInstance().getConfigDir());
        ConfigPages.INSTANCE.register(new FastItemsConfigPage());
    }
}
