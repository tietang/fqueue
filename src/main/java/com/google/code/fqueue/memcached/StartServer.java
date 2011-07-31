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
package com.google.code.fqueue.memcached;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import com.google.code.fqueue.util.Config;

/**
 * @author sunli
 * @date 2011-5-18
 * @version $Id$
 */
public class StartServer {
	private static final Log log = LogFactory.getLog(StartServer.class);
	static {
		PropertyConfigurator.configure("config/log4j.properties");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configureAndWatch("config/log4j.properties", 5000);
		StartNewQueue.newQueueInstance(Integer.parseInt(Config.getSetting("port")));
		log.info("running at port " + Config.getSetting("port"));
	}

}
