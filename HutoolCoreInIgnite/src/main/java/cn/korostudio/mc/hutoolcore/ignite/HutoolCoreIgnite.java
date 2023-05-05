package cn.korostudio.mc.hutoolcore.ignite;

import cn.korostudio.mc.hutoolcore.common.HutoolCore;
import cn.korostudio.mc.hutoolcore.common.Loader;
import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import space.vectrix.ignite.api.Platform;
import space.vectrix.ignite.api.event.Subscribe;
import space.vectrix.ignite.api.event.platform.PlatformInitializeEvent;

public class HutoolCoreIgnite {

  public static Logger logger;
  private static Platform platform;
  @Inject
  public HutoolCoreIgnite(final Logger logger,
                         final Platform platform) {
    HutoolCoreIgnite.logger = logger;
    HutoolCoreIgnite.platform = platform;
  }
  @Subscribe
  public void onInitialize(final @NonNull PlatformInitializeEvent event) {
    HutoolCore.init(Loader.Ignite);
  }

  //public void onInitialize() {
  //      WAC.init();
  //  }
}
