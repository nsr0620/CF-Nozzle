package com.cfnozzle.utils;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Logger;

import com.cfnozzle.mock.MockUtils;

public class UDPConnection {
	private static final Logger logger = Logger.getLogger(UDPConnection.class.getCanonicalName());
	public static DatagramSocket udp = null;
	public static InetAddress targetAddress = null;

	public static DatagramSocket getUDPConnection() {
		if (udp == null) {
			try {
				udp = new DatagramSocket();
			} catch (Exception e) {
				logger.severe("Exception occured while getting UDP connection, reason: " + e.getMessage());
				udp = null;
			}
		}
		return udp;
	}

	public static InetAddress getInetAddress(String targetIp) {

		if (targetAddress == null) {
			try {
				targetAddress = InetAddress.getByName(targetIp);
			} catch (Exception e) {
				logger.severe("Exception occured while getting Inet address, reason: " + e.getMessage());
				targetAddress = null;
			}
		}
		return targetAddress;

	}

}
