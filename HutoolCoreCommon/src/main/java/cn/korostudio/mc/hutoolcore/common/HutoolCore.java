package cn.korostudio.mc.hutoolcore.common;

import cn.korostudio.mc.hutoolcore.common.config.ConfigUtil;
import cn.korostudio.mc.hutoolcore.common.config.HutoolCoreConfig;
import cn.korostudio.mc.hutoolcore.common.webconfig.WebConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HutoolCore {
    @Getter
    protected static Loader loader = Loader.None;
    @Getter
    protected static HutoolCoreConfig config = ConfigUtil.getInstance("HutoolCoreConfig",HutoolCoreConfig.class);

    public static void init(Loader loader){
        if(getLoader()!=Loader.None){
            log.error("发现多次初始化行为，当前HutoolCore已被 "+getLoader().toString()+"加载器加载，"+loader.toString()+" 加载器正在尝试加载。");
            return;
        }
        log.info("感谢使用HutoolCore，当前加载器为 "+loader.toString());
        if (config.isUseWebConfig()){
            WebConfig.init();
        }else{
            log.info("已关闭在线网页配置器，在HutoolCore配置文件中设置UseWebConfig为true即可开启。");
        }
    }
}
