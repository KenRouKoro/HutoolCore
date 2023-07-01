package cn.korostudio.mc.hutoolcore.common.config;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.exceptions.NotInitedException;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.core.io.watch.watchers.DelayWatcher;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ReflectUtil;
import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.naming.NameNotFoundException;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static cn.korostudio.mc.hutoolcore.common.config.JSONConfigUtil.configObject;

@Slf4j
public class TOMLConfigUtil {
    @Getter
    private static final ConcurrentHashMap<String, List<ConfigChangeCallback>>configCallbackMap = new ConcurrentHashMap<>();
    @Getter
    private static final ConcurrentHashMap<String, WatchMonitor>configWatchMap = new ConcurrentHashMap<>();
    @Getter
    private static final ConcurrentHashMap<String, Long>lastSaveMap = new ConcurrentHashMap<>();
    @SuppressWarnings("unchecked")
    public static<T> T getInstance(String name,Class<T> config){
        if (configObject.contains(name)){
            try {
                return (T) configObject.get(name);
            }catch (Exception e){
                log.error("转换配置文件出错！",e);
            }
        }
        if(FileUtil.isFile(new File(System.getProperty("user.dir")+"/config/"+name+".toml"))){
            String configToml = FileReader.create(new File(System.getProperty("user.dir")+"/config/" + name + ".toml"), CharsetUtil.CHARSET_UTF_8).readString();
            T obj = new Toml().read(configToml).to(config);
            configObject.put(name,obj);
            log.info("已读取配置文件："+System.getProperty("user.dir")+"/config/"+name+".toml");
            watchConfigFile(name);
            return obj;
        }
        T obj = ReflectUtil.newInstance(config);
        FileWriter fileWriter = FileWriter.create(FileUtil.touch(System.getProperty("user.dir")+"/config/"+name+".toml"), CharsetUtil.CHARSET_UTF_8);
        TomlWriter tomlWriter = new TomlWriter();
        fileWriter.write(tomlWriter.write(obj));
        configObject.put(name,obj);
        log.info("已创建配置文件："+System.getProperty("user.dir")+"/config/"+name+".toml");
        watchConfigFile(name);
        return obj;
    }

    protected static void watchConfigFile(String name){
        if(configWatchMap.get(name)!=null){
            log.warn("重复注册"+name+"配置文件修改监听器！");
            return;
        }
        WatchMonitor watchMonitor = WatchMonitor.create(System.getProperty("user.dir")+"/config/"+name+".toml", WatchMonitor.ENTRY_MODIFY);
        watchMonitor.setWatcher(new DelayWatcher(new Watcher() {
            @Override
            public void onCreate(WatchEvent<?> event, Path currentPath) {

            }

            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                Long lastTime = lastSaveMap.get(name);
                if (lastTime != null){
                    if(System.currentTimeMillis()-lastTime<=2000){
                        return;
                    }
                }
                String configTOML = FileReader.create(new File(System.getProperty("user.dir") + "/config/" + name + ".toml"),CharsetUtil.CHARSET_UTF_8).readString();
                try {
                    updateConfig(name,configTOML);
                } catch (Exception e) {
                    log.error("更新本地配置文件失败！地址："+System.getProperty("user.dir") + "/config/" + name + ".toml",e);
                    return;
                }
                List<ConfigChangeCallback>list = configCallbackMap.get(name);
                if (list == null){
                    return;
                }
                list.forEach(obj-> obj.run(configObject.get(name)));
            }

            @Override
            public void onDelete(WatchEvent<?> event, Path currentPath) {

            }

            @Override
            public void onOverflow(WatchEvent<?> event, Path currentPath) {

            }
        },500));
        watchMonitor.start();
        log.info("已启用"+name+"配置文件修改监听器");
    }
    public static void updateConfig(String name, String configTOML) throws NameNotFoundException {
        Object obj = configObject.get(name);


        if (obj==null){
            throw new NameNotFoundException("ID "+name+" 没有对应的配置文件注册。");
        }
        Object beanObj = new Toml().read(configTOML).to(obj.getClass());
        BeanUtil.copyProperties(beanObj,obj);
        log.info("更新配置文件:"+name);
    }

    public static void addConfigChangeCallBack(String name,ConfigChangeCallback callback){
        List<ConfigChangeCallback> list = configCallbackMap.get(name);
        if (list == null){
            list = new CopyOnWriteArrayList<>();
            configCallbackMap.put(name,list);
        }
        list.add(callback);
    }

    public static void saveALL(){
        TomlWriter writer = new TomlWriter();
        configObject.forEach((name,obj)->{
            FileWriter fileWriter = FileWriter.create(FileUtil.touch(System.getProperty("user.dir")+"/config/"+name+".toml"), CharsetUtil.CHARSET_UTF_8);
            fileWriter.write(writer.write(obj));
        });
    }
    public static void save(String name){
        log.info("正在保存："+System.getProperty("user.dir")+"/config/"+name+".toml");
        Object value = configObject.get(name);
        if (value == null){
            throw new NotInitedException(name+" 的配置文件没有初始化,保存失败");
        }
        TomlWriter writer = new TomlWriter();
        FileWriter fileWriter = FileWriter.create(FileUtil.touch(System.getProperty("user.dir")+"/config/"+name+".toml"), CharsetUtil.CHARSET_UTF_8);
        lastSaveMap.put(name,System.currentTimeMillis());
        fileWriter.write(writer.write(value));
    }

    public static<T> void save(Class<T>name){
        save(name.getSimpleName());
    }

    public static<T> T getInstance(Class<T> config){
        return getInstance(config.getSimpleName(),config);
    }
    public static Object get(String name){
        Object value = configObject.get(name);
        if (value == null){
            throw new NotInitedException(name+" 的配置文件没有初始化,保存失败");
        }
        return value;
    }
}
