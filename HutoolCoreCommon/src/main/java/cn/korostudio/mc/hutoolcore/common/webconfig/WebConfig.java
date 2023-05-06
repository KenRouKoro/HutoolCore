package cn.korostudio.mc.hutoolcore.common.webconfig;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.server.SimpleServer;
import cn.hutool.http.server.handler.ActionHandler;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.korostudio.mc.hutoolcore.common.HutoolCore;
import cn.korostudio.mc.hutoolcore.common.config.ConfigUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.naming.NameNotFoundException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.HashMap;

@Slf4j
public class WebConfig {
    @Getter
    protected static SimpleServer server ;
    public static void init(){
        ClassPathResource resource = new ClassPathResource("web/index.html",WebConfig.class);
        ClassPathResource resourceJS = new ClassPathResource("web/assets/index.js",WebConfig.class);
        ClassPathResource resourceCSS = new ClassPathResource("web/assets/index.css",WebConfig.class);

        String indexStr = resource.readUtf8Str();
        String jsStr = resourceJS.readUtf8Str();
        String cssStr = resourceCSS.readUtf8Str();


        server = HttpUtil.createServer(HutoolCore.getConfig().getWebServerPort());
        HttpContext context = server.createContext("/",new ActionHandler((req, res)->{
            res.write(indexStr, ContentType.TEXT_HTML.toString());
        }));

        if (HutoolCore.getConfig().isUseAuth()){
            BasicAuthenticator authenticator = new BasicAuthenticator("HutoolCoreWebConfigAuth") {
                @Override
                public boolean checkCredentials(String username, String password) {
                    // 检查用户名和密码是否匹配
                    return username.equals(HutoolCore.getConfig().getAdminUserName()) && password.equals(HutoolCore.getConfig().getAdminUserPasswd());
                }
            };
            context.setAuthenticator(authenticator);
            log.info("已启用在线网页配置器基本HTTP验证");
        }

        server.addAction("/assets/index.js",(req,res)->{
            res.write(jsStr,"application/javascript");
        });
        server.addAction("/assets/index.css",(req,res)->{
            res.write(cssStr,"text/css");
        });
        server.addAction("/datalist",(request,response)->{
            JSONArray array = new JSONArray();
            ConfigUtil.getConfigObject().forEach((key,obj)->{
                JSONObject object = new JSONObject();
                object.putOnce("id",key);
                object.putOnce("name",key);
                array.add(object);
            });
            response.write(array.toString(),ContentType.JSON.toString());
        });
        server.addAction("/getdata",((request, response) -> {
            String id = request.getParam("id");
            response.write(JSONUtil.toJsonStr(ConfigUtil.getConfigObject().get(id)),ContentType.JSON.toString());
        }));
        server.addAction("/updata",((request, response) -> {
            String data = request.getBody(CharsetUtil.CHARSET_UTF_8);
            JSONObject jsonObject = JSONUtil.parseObj(data);
            String id = jsonObject.getStr("id");
            JSONObject value = jsonObject.getJSONObject("data");
            try {
                ConfigUtil.updateConfig(id,value);
            } catch (NameNotFoundException e) {
                response.send404("未知请求ID");
                return;
            } catch (Exception e){
                log.error("更新在线配置文件"+id+"失败！",e);
                response.sendError(502,e.getMessage());
                return;
            }
            ConfigUtil.save(id);
            response.write("{\"status\":\"ok\"}",ContentType.JSON.toString());
        }));



        ThreadUtil.execAsync(()->{
            server.start();
        });
        if (HutoolCore.getConfig().isUseExternalAddress()) {
            log.info("已启用在线网页配置器，地址：\n" + HutoolCore.getConfig().getWebServerUrlHeader() + ":" + HutoolCore.getConfig().getWebServerPort()+"\n"+HutoolCore.getConfig().getExternalAddress());
            log.info("\n"+ StrQRcode(HutoolCore.getConfig().getExternalAddress()));
        }else{
            log.info("已启用在线网页配置器，地址：" + HutoolCore.getConfig().getWebServerUrlHeader() + ":" + HutoolCore.getConfig().getWebServerPort());
            log.info("\n"+ StrQRcode(HutoolCore.getConfig().getWebServerUrlHeader() + ":" + HutoolCore.getConfig().getWebServerPort()));
        }
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            log.info("正在关闭HTTPServer");
            server.getRawServer().stop(5);
        }));

    }


    public static String StrQRcode(String content) {
        int width = 1; // 二维码宽度
        int height = 1; // 二维码高度
        StringBuilder str = new StringBuilder();

        // 定义二维码的参数
        HashMap<EncodeHintType, Serializable> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");//编码方式
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);//纠错等级

        // 打印二维码
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            for (int j = 0; j < bitMatrix.getHeight(); j++) {
                for (int i = 0; i < bitMatrix.getWidth(); i++) {
                    if (bitMatrix.get(i, j)) {
                        str.append("■");
                        //System.out.print("■");
                    } else {
                        str.append("  ");
                        //System.out.print("  ");
                    }

                }
                //System.out.println();
                str.append('\n');
            }
        } catch (WriterException e) {
            log.error("",e);
        }
        return str.toString();
    }
}
