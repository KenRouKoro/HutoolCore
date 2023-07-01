package cn.korostudio.mc.hutoolcore.common.config;

import cn.korostudio.ctoml.OutputAnnotation;
import lombok.Data;

import javax.swing.text.StyledEditorKit;

@Data
@OutputAnnotation("HutoolCore配置文件")
public class HutoolCoreConfig {
    @OutputAnnotation("是否启用网页配置管理器")
    boolean UseWebConfig = false;
    @OutputAnnotation("网页配置管理器端口")
    int WebServerPort = 8620;
    @OutputAnnotation("网页配置管理器前缀")
    String WebServerUrlHeader = "http://127.0.0.1";
    @OutputAnnotation("是否启用基本HTTP验证")
    boolean UseAuth = false;
    @OutputAnnotation("基本Http验证用户名")
    String AdminUserName = "admin";
    @OutputAnnotation("基本Http验证密码")
    String AdminUserPasswd = "123456";
    @OutputAnnotation("是否使用外部地址")
    boolean UseExternalAddress = false;
    @OutputAnnotation("外部地址")
    String ExternalAddress = "https://example.com";
}
