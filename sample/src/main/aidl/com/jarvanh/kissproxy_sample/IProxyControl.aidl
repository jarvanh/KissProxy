package com.jarvanh.kissproxy_sample;

interface IProxyControl {
	boolean start();

	boolean stop();

	boolean isRunning();

	int getPort();
}
