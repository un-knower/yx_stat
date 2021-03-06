package com.jiuxiu.yxstat.redis;


public class JedisChannelIDActivationKeyConstant {

    /**
     *   启动次数
     */
    public static String CHANNEL_ID_STARTUP_COUNT = "_CHANNEL_ID_STARTUP_COUNT_";

    /**
     *   没有唯一值 同ip， 设备名 系统名 新增设备数
     */
    public static String CHANNEL_ID_NEW_DEVICE_IP_DEVICENAME_DEVICEOSVER_COUNT = "CHANNEL_ID_NEW_DEVICE_IP_DEVICENAME_DEVICEOSVER_COUNT:";

    /**
     *   新增设备数
     */
    public static String CHANNEL_ID_NEW_DEVICE_COUNT = "_CHANNEL_ID_NEW_DEVICE_COUNT:";

    /**
     *   启动设备数
     */
    public static String CHANNEL_ID_STARTUP_DEVICE_COUNT = "_CHANNEL_ID_STARTUP_DEVICE_COUNT:";

    /**
     *  IOS 启动设备数
     */
    public static String IOS_CHANNEL_ID_STARTUP_DEVICE_COUNT = "_IOS_CHANNEL_ID_STARTUP_DEVICE_COUNT:";

    /**
     *   启动设备信息
     */
    public static String CHANNEL_ID_STARTUP_DEVICE_INFO = "_CHANNEL_ID_STARTUP_DEVICE_INFO_IMEI:";

    /**
     *  ios 启动设备信息
     */
    public static String IOS_CHANNEL_ID_STARTUP_DEVICE_INFO = "_IOS_CHANNEL_ID_STARTUP_DEVICE_INFO_IDFA:";

    /**
     *   ios 没有唯一值 ， 同 ip 设备名，设备系统版本 启动的设备数 key
     */
    public static String CHANNEL_ID_STARTUP_DEVCEI_INFO_IP_DEVICENAME_DEVICEOSVER = "_APP_CHANNEL_ID_STARTUP_DEVCEI_INFO_IP_DEVICENAME_DEVICEOSVER=";

}
