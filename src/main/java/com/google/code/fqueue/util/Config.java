/*
 *  Copyright 2011 sunli [sunli1223@gmail.com][weibo.com@sunli1223]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.google.code.fqueue.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.code.fqueue.exception.ConfigException;

/**
 * @author sunli
 * @date 2010-8-12
 * @version $Id$
 */
public class Config {
	private final static Log log = LogFactory.getLog(Config.class);
	// map 存储配置信息
	private static Map<String, String> setting = new ConcurrentHashMap<String, String>();
	// 调用初始化操作
	static {
		iniSetting();
	}

	public Config() {

	}

	/**
	 * 初始化加载配置文件 默认加载配置路径config/config.properties或者config.properties
	 * 
	 * @throws FileNotFoundException
	 */
	public static synchronized void iniSetting() {
		File file;
		file = new File("config.properties");
		if (!file.exists()) {
			iniSetting("config/config.properties");
		} else {
			iniSetting("config.properties");
		}
	}

	/**
	 * 初始化加载配置文件
	 * 
	 * @param path
	 *            加载路径
	 * @throws FileNotFoundException
	 */
	public static synchronized void iniSetting(String path) {
		File file;
		file = new File(path);
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			Properties p = new Properties();
			p.load(in);
			// 遍历配置文件加入到Map中进行缓存
			Enumeration<?> item = p.propertyNames();
			while (item.hasMoreElements()) {
				String key = (String) item.nextElement();
				setting.put(key, p.getProperty(key));
			}
			in.close();
		} catch (FileNotFoundException e) {
			log.error("config file not found at" + file.getAbsolutePath());
			throw new ConfigException("FileNotFoundException", e);
		} catch (IOException e) {
			log.error("config file not found at" + file.getAbsolutePath());
			throw new ConfigException("IOException", e);
		} catch (Exception e) {
			throw new ConfigException("Exception", e);
		}
	}

	public static void reload() {
		try {
			iniSetting();
		} catch (ConfigException e) {
			throw new ConfigException(e.getMessage(), e);
		}
	}

	/**
	 * 获取配置文件的某个键值的配置信息
	 * 
	 * @param key
	 *            键
	 * @return 值
	 */
	public static String getSetting(String key) {
		return setting.get(key);
	}

	/**
	 * 设置配置文件的数据
	 * 
	 * @param key
	 * @param value
	 */
	public static void setSetting(String key, String value) {
		setting.put(key, value);
	}

	public static int getInt(String key) {
		return Integer.parseInt(setting.get(key));
	}
}
