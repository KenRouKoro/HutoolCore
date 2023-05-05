package cn.korostudio.mc.hutoolcore.common.config;

import lombok.Data;

import javax.swing.text.StyledEditorKit;

@Data
public class HutoolCoreConfig {
    boolean UseWebConfig = false;
    int WebServerPort = 8620;
    String WebServerUrlHeader = "http://127.0.0.1";
    boolean UseAuth = false;
    String AdminUserName = "admin";
    String AdminUserPasswd = "123456";
    boolean UseExternalAddress = false;
    String ExternalAddress = "https://example.com";
}
