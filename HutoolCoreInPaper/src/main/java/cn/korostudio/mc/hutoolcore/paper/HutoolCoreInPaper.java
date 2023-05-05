package cn.korostudio.mc.hutoolcore.paper;

import cn.korostudio.mc.hutoolcore.common.HutoolCore;
import cn.korostudio.mc.hutoolcore.common.Loader;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HutoolCoreInPaper extends JavaPlugin {
private static final Logger log = LoggerFactory.getLogger(HutoolCoreInPaper.class);
    @Override
    public void onEnable() {
        // Plugin startup logic
        HutoolCore.init(Loader.Bukkit);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
