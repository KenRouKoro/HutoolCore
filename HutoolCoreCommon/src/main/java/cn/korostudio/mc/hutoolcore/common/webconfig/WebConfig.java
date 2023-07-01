package cn.korostudio.mc.hutoolcore.common.webconfig;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.server.SimpleServer;
import cn.hutool.http.server.handler.ActionHandler;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.korostudio.ctoml.OutputAnnotation;
import cn.korostudio.ctoml.OutputAnnotationData;
import cn.korostudio.mc.hutoolcore.common.HutoolCore;
import cn.korostudio.mc.hutoolcore.common.config.JSONConfigUtil;
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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

@Slf4j
public class WebConfig {
    @Getter
    protected static SimpleServer server ;
    @SuppressWarnings("unchecked")
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
            JSONConfigUtil.getConfigObject().forEach((key, obj)->{
                JSONObject object = new JSONObject();
                object.putOnce("id",key);
                object.putOnce("name",key);
                array.add(object);
            });
            response.write(array.toString(),ContentType.JSON.toString());
        });
        server.addAction("/getdata",((request, response) -> {
            String id = request.getParam("id");
            response.write(JSONUtil.toJsonStr(JSONConfigUtil.getConfigObject().get(id)),ContentType.JSON.toString());
        }));
        server.addAction("/getannotation",((request, response) -> {
            String id = request.getParam("id");
            Object obj = JSONConfigUtil.getConfigObject().get(id);
            Set<Field> fields = getFields(obj.getClass());
            Map<String, Object> to = new LinkedHashMap<>();
            OutputAnnotation classAnn = obj.getClass().getAnnotation(OutputAnnotation.class);
            if (isValidAnnotation(classAnn)){
                to.put("CLASS_ANN",classAnn.value());
            }

            for (Field field : fields) {
                OutputAnnotation annotation = field.getAnnotation(OutputAnnotation.class);
                field.setAccessible(true);
                if(isValidAnnotation(annotation)){
                    handleAnnotation(obj, to, field, annotation);
                }else {
                    handleNonAnnotatedField(obj, to, field);
                }
            }
            String rep = JSONUtil.toJsonStr(to);
            response.write(rep,ContentType.JSON.toString());
        }));
        server.addAction("/updata",((request, response) -> {
            String data = request.getBody(CharsetUtil.CHARSET_UTF_8);
            JSONObject jsonObject = JSONUtil.parseObj(data);
            String id = jsonObject.getStr("id");
            JSONObject value = jsonObject.getJSONObject("data");
            try {
                JSONConfigUtil.updateConfig(id,value);
            } catch (NameNotFoundException e) {
                response.send404("未知请求ID");
                return;
            } catch (Exception e){
                log.error("更新在线配置文件"+id+"失败！",e);
                response.sendError(502,e.getMessage());
                return;
            }
            JSONConfigUtil.save(id);
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
            server.getRawServer().stop(2);
        }));

    }

    public static Map<String,Object> getBeanAnn(Class<?> target){
        return getBeanAnn(target, new HashSet<>());
    }

    private static Map<String,Object> getBeanAnn(Class<?> target, Set<Class<?>> visited){
        if (visited.contains(target)) {
            return Collections.emptyMap();
        }

        visited.add(target);

        Set<Field> fields = getFields(target);
        Map<String, Object> to = new LinkedHashMap<>();
        OutputAnnotation classAnn = target.getAnnotation(OutputAnnotation.class);
        if (isValidAnnotation(classAnn)){
            to.put("CLASS_ANN",classAnn.value());
        }
        for (Field field : fields) {
            OutputAnnotation annotation = field.getAnnotation(OutputAnnotation.class);
            field.setAccessible(true);
            if(isValidAnnotation(annotation)){
                if (BeanUtil.isBean(field.getType())){
                    to.put(field.getName(),getBeanAnn(field.getType(), visited));
                }else if(Map.class.isAssignableFrom(field.getType())){
                    // Handle Map type field, you need to implement this method
                    handleMapFieldInBeanAnn(field, to, visited);
                }
                else {
                    to.put(field.getName(), annotation.value());
                }
            }else {
                to.put(field.getName(), null);
            }
        }
        return to;
    }

    private static void handleMapFieldInBeanAnn(Field field, Map<String, Object> to, Set<Class<?>> visited) {
        try {
            Map<String, Object> mapValue = (Map<String, Object>) field.get(null);
            Map<String, Object> annotationMap = new LinkedHashMap<>();
            for(Map.Entry<String, Object> entry : mapValue.entrySet()) {
                if (BeanUtil.isBean(entry.getValue().getClass())) {
                    annotationMap.put(entry.getKey(), getBeanAnn(entry.getValue().getClass(), visited));
                }
            }
            to.put(field.getName(), annotationMap);
        } catch (IllegalAccessException e) {
            // Log the exception here
            log.error("Accessing field failed", e);
        }
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
    private static Set<Field> getFields(Class<?> cls) {
        Set<Field> fields = new LinkedHashSet<Field>(Arrays.asList(cls.getDeclaredFields()));
        while (cls != Object.class) {
            fields.addAll(Arrays.asList(cls.getDeclaredFields()));
            cls = cls.getSuperclass();
        }
        removeConstantsAndSyntheticFields(fields);

        return fields;
    }
    private static void removeConstantsAndSyntheticFields(Set<Field> fields) {
        Iterator<Field> iterator = fields.iterator();
        while (iterator.hasNext()) {
            Field field = iterator.next();
            if ((Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) || field.isSynthetic() || Modifier.isTransient(field.getModifiers())) {
                iterator.remove();
            }
        }
    }

    private static boolean isValidAnnotation(OutputAnnotation annotation){
        return annotation != null && annotation.at() != null && annotation.value() != null;
    }

    private static void handleAnnotation(Object obj, Map<String, Object> to, Field field, OutputAnnotation annotation){
        if (BeanUtil.isBean(field.getType())){
            to.put(field.getName(),getBeanAnn(field.getType()));
        }else if(Map.class.isAssignableFrom(field.getType())){
            handleMapField(obj, to, field);
        }
        else {
            to.put(field.getName(), annotation.value());
        }
    }

    private static void handleNonAnnotatedField(Object obj, Map<String, Object> to, Field field){
        if(Map.class.isAssignableFrom(field.getType())){
            handleMapField(obj, to, field);
        }else {
            to.put(field.getName(), null);
        }
    }

    private static void handleMapField(Object obj, Map<String, Object> to, Field field){
        try {
            Map<String,Object> mapObj = (Map<String, Object>) field.get(obj);
            Map<String, Object> to1 = new LinkedHashMap<>();
            for(String key: mapObj.keySet()){
                Object value = mapObj.get(key);
                if (BeanUtil.isBean(value.getClass())){
                    to1.put(key,getBeanAnn(value.getClass()));
                }
            }
            to.put(field.getName(),to1);
        } catch (Exception e) {
            log.error("转换Map对象失败",e);
        }
    }
}
