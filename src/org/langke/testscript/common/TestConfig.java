package org.langke.testscript.common;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 配置文件读取程序
 * 读取指定目录下配置文件，有守护线程每60秒检测如果文件被修改则重新加载配置
 * 如果有配置autoUpdate，勾子程序在进程结束时，判断如有修改过配置，则把配置全部刷到文件
 * @author langke
 * @since JDK1.6
 * @version 1.0
 *
 */
public class TestConfig {
	public Properties properties = new Properties();
	private static Logger log = LoggerFactory.getLogger(TestConfig.class);
	private static Map<String, TestConfig> configMap = new HashMap<String, TestConfig>();
	private boolean modified = false;
	private final File cf;
	private long time;

	public static String getConfigDir() {
		String configDir = null;
		String userDir = System.getProperty("user.dir");
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {

			configDir = userDir + "";
		} else {

			configDir = userDir + "";
		}
		return configDir;
	}

	public String getConfigPath() {
		return cf.getAbsolutePath();
	}
	
	private TestConfig(String confFilePath) {
		cf = new File(confFilePath);
		try {
			/*
			if (!cf.exists()) {
				cf.getParentFile().mkdirs();
				cf.createNewFile();
			}*/
			// 当进程关闭，如果properties有修改，则写入文件
			Runtime.getRuntime().addShutdownHook(new Thread("store-config") {
				public void run() {
					try {
						if (modified) {
							boolean autoUpdate = properties.containsKey("autoUpdate");
							if (autoUpdate) {
								FileOutputStream fos = new FileOutputStream(cf);
								properties.store(fos,
												"add an <autoUpdate> key to auto update config form default values");
								fos.close();
							}
						}
					} catch (Exception ex) {
						log.warn("store config", ex);
					}
				}
			});
			properties.load(new java.io.FileInputStream(cf));
			log.info("loading config from:" + cf.getAbsolutePath());
			time = cf.lastModified();
			// 检测配置文件是否被修改，自动reload
			Thread t = new Thread(new Runnable() {
				public void run() {
					try {
						Thread.sleep(60000);
					} catch (InterruptedException e) {
					}
					long newlmd = cf.lastModified();
					if (newlmd > time) {
						time = newlmd;
						log.info("Config file {} is modified,reloading ...",
								cf.getAbsolutePath());
						try {
							properties.load(new java.io.FileInputStream(cf));
						} catch (IOException e) {
							log.error("Error while loading config file:{}",
									cf.getAbsolutePath());
						}
					}
				}
			}, "Config file refresher");
			t.setDaemon(true);
			t.start();
		} catch (IOException ex) {
			log.warn("cannot create log file", ex);
		}
	}

	public static final TestConfig getInstance(String confFilePath) {
		if (!configMap.containsKey(confFilePath)) {
			TestConfig config = new TestConfig(confFilePath);
			configMap.put(confFilePath, config);
		}
		return configMap.get(confFilePath);
	}

	public String get(String key) {
		return properties.getProperty(key);
	}

	public String get(String k, String defaultValue) {
		String s = properties.getProperty(k);
		if (s == null ) {
			properties.setProperty(k, defaultValue);
			modified = true;
			return defaultValue;
		}else if( s.equals("")){
			properties.setProperty(k, defaultValue);
			return defaultValue;
		}
		return s;
	}

	public int getInt(String k, int defaultValue) {
		String s = this.get(k, defaultValue + "");
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public boolean getBoolean(String k, boolean defaultValue) {
		String s = this.get(k, defaultValue + "");
		try {
			return Boolean.parseBoolean(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public boolean setProperty(String key, String value) {
		properties.setProperty(key, value);
		try {
			FileOutputStream fos = new java.io.FileOutputStream(cf);
			properties.store(fos, "");
			fos.close();
			return true;
		} catch (Exception ex) {
			log.warn("store config", ex);
			return false;
		}
	}

}
