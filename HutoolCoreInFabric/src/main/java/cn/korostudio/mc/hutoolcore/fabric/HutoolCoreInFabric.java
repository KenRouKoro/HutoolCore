package cn.korostudio.mc.hutoolcore.fabric;

import cn.korostudio.mc.hutoolcore.common.HutoolCore;
import cn.korostudio.mc.hutoolcore.common.Loader;
import net.fabricmc.api.ModInitializer;

public class HutoolCoreInFabric implements ModInitializer {
    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        HutoolCore.init(Loader.Fabric);
    }
}
