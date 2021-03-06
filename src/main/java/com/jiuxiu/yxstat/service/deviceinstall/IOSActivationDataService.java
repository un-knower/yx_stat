package com.jiuxiu.yxstat.service.deviceinstall;

import com.jiuxiu.yxstat.dao.stat.StatAppChannelIdDeviceActiveDao;
import com.jiuxiu.yxstat.dao.stat.StatAppIdDeviceActiveDao;
import com.jiuxiu.yxstat.dao.stat.StatChannelIdDeviceActiveDao;
import com.jiuxiu.yxstat.dao.stat.StatChildDeviceActiveDao;
import com.jiuxiu.yxstat.dao.stat.StatPackageIdDeviceActiveDao;
import com.jiuxiu.yxstat.es.DeviceActivationStatisticsESStorage;
import com.jiuxiu.yxstat.redis.JedisAppChannelIDActivationKeyConstant;
import com.jiuxiu.yxstat.redis.JedisAppIDActivationKeyConstant;
import com.jiuxiu.yxstat.redis.JedisChannelIDActivationKeyConstant;
import com.jiuxiu.yxstat.redis.JedisChildIDActivationKeyConstant;
import com.jiuxiu.yxstat.redis.JedisPackageIDActivationKeyConstant;
import com.jiuxiu.yxstat.redis.JedisPoolConfigInfo;
import com.jiuxiu.yxstat.redis.JedisUtils;
import com.jiuxiu.yxstat.service.ServiceConstant;
import com.jiuxiu.yxstat.utils.DateUtil;
import com.jiuxiu.yxstat.utils.StringUtils;
import net.sf.json.JSONObject;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.elasticsearch.action.search.SearchResponse;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ZhouFy on 2018/6/12.
 *
 * @author ZhouFy
 */
public class IOSActivationDataService implements Serializable{

    private static StatAppIdDeviceActiveDao statAppIdDeviceActiveDao = StatAppIdDeviceActiveDao.getInstance();

    private static StatChildDeviceActiveDao statChildDeviceActiveDao = StatChildDeviceActiveDao.getInstance();

    private static StatChannelIdDeviceActiveDao statChannelIdDeviceActiveDao = StatChannelIdDeviceActiveDao.getInstance();

    private static StatAppChannelIdDeviceActiveDao statAppChannelIdDeviceActiveDao = StatAppChannelIdDeviceActiveDao.getInstance();

    private static StatPackageIdDeviceActiveDao statPackageIdDeviceActiveDao = StatPackageIdDeviceActiveDao.getInstance();

    private static DeviceActivationStatisticsESStorage deviceActivationStatisticsESStorage = DeviceActivationStatisticsESStorage.getInstance();

    public static void iosActivationData(JavaRDD<JSONObject> ios) {

        String toDay = DateUtil.getNowDate(DateUtil.yyyy_MM_dd);

        ios.foreach(new VoidFunction<JSONObject>() {
            @Override
            public void call(JSONObject json) {
                int appID = json.getInt("appid");
                int childID = json.getInt("child_id");
                int appChannelID = json.getInt("app_channel_id");
                int channelID = json.getInt("channel_id");
                int packageID = json.getInt("package_id");
                String imei = json.getString("imei");
                String idfa = json.getString("idfa");
                boolean flag = false;

                if (StringUtils.isEmpty(imei)) {
                     if(StringUtils.isEmpty(idfa)){
                         SearchResponse searchResponse = deviceActivationStatisticsESStorage.getAppDeviceActivationForIdfa(idfa, appID, childID);
                         if (searchResponse == null || searchResponse.getHits() == null || searchResponse.getHits().getHits() == null ||
                                 searchResponse.getHits().getHits().length == 0) {
                             flag = true;
                         }
                     }else{
                         String deviceName = json.getString("device_name");
                         String deviceOSVer = json.getString("device_os_ver");
                         String ip = json.getString("client_ip");

                         SearchResponse searchResponse = deviceActivationStatisticsESStorage.iosSpecialSearchForAppID(ip, deviceName, deviceOSVer, appID, childID);
                         if (searchResponse == null || searchResponse.getHits() == null || searchResponse.getHits().getHits() == null ||
                                 searchResponse.getHits().getHits().length < ServiceConstant.ACTIVE_COUNT) {
                             flag = true;
                         }
                     }
                } else {
                    SearchResponse searchResponse = deviceActivationStatisticsESStorage.getAppDeviceActivationForImei(imei, appID, childID);
                    if (searchResponse == null || searchResponse.getHits() == null || searchResponse.getHits().getHits() == null ||
                            searchResponse.getHits().getHits().length == 0) {
                        flag = true;
                    }
                }
                if (flag) {
                    // 没有激活
                    deviceActivationStatisticsESStorage.saveDeviceInstallForAppID(json, appID);
                    //  保存 child id 激活数
                    JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisChildIDActivationKeyConstant.CHILD_ID_NEW_DEVICE_COUNT + childID);
                    //  保存 app id 激活数
                    JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisAppIDActivationKeyConstant.APP_ID_NEW_DEVICE_COUNT + appID);
                    //  保存 app channel id 激活数
                    JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisAppChannelIDActivationKeyConstant.APP_CHANNEL_ID_NEW_DEVICE_COUNT + appChannelID);
                    //  保存 channel id 激活数
                    JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisChannelIDActivationKeyConstant.CHANNEL_ID_NEW_DEVICE_COUNT + channelID);
                    //  保存 package id 激活数
                    JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisPackageIDActivationKeyConstant.PACKAGE_ID_NEW_DEVICE_COUNT + packageID);
                }

                //  启动设备数
                if (StringUtils.isEmpty(idfa)) {

                    String deviceName = json.getString("device_name");
                    String deviceOSVer = json.getString("device_os_ver");
                    String ip = json.getString("client_ip");

                    String key = toDay + JedisChildIDActivationKeyConstant.CHILD_ID_STARTUP_DEVCEI_INFO_IP_DEVICENAME_DEVICEOSVER
                            + childID + ":" + ip + ":" + deviceName + ":" + deviceOSVer;

                    String value = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, key);
                    value = value == null ? "0" : value;
                    if (Integer.parseInt(value) < ServiceConstant.ACTIVE_COUNT) {
                        JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, key);
                        // child id 启动设备数加一
                        JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisChildIDActivationKeyConstant.IOS_CHILD_ID_STARTUP_DEVICE_COUNT + childID);
                        // app channel id 启动设备数加一
                        JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisAppChannelIDActivationKeyConstant.IOS_APP_CHANNEL_ID_STARTUP_DEVICE_COUNT + appChannelID);
                        // app id 启动设备数加一
                        JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisAppIDActivationKeyConstant.IOS_APP_ID_STARTUP_DEVICE_COUNT + appID);
                        // channel id 启动设备数加一
                        JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisChannelIDActivationKeyConstant.IOS_CHANNEL_ID_STARTUP_DEVICE_COUNT + channelID);
                        // package id 启动设备数加一
                        JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisPackageIDActivationKeyConstant.IOS_PACKAGE_ID_STARTUP_DEVICE_COUNT + packageID);
                    }
                } else {
                    // child id 启动设备数
                    String value = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisChildIDActivationKeyConstant.IOS_CHILD_ID_STARTUP_DEVICE_INFO + childID + ":" + idfa);
                    if (value == null) {
                        JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisChildIDActivationKeyConstant.IOS_CHILD_ID_STARTUP_DEVICE_COUNT + childID);
                        JedisUtils.set(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisChildIDActivationKeyConstant.IOS_CHILD_ID_STARTUP_DEVICE_INFO + childID + ":" + idfa, json.toString(), 0);
                    }
                    //  app channel id 启动设备数
                    value = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisAppChannelIDActivationKeyConstant.IOS_APP_CHANNEL_ID_STARTUP_DEVICE_INFO + appChannelID + ":" + idfa);
                    if (value == null) {
                        JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisAppChannelIDActivationKeyConstant.IOS_APP_CHANNEL_ID_STARTUP_DEVICE_COUNT + appChannelID);
                        JedisUtils.set(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisAppChannelIDActivationKeyConstant.IOS_APP_CHANNEL_ID_STARTUP_DEVICE_INFO + appChannelID + ":" + idfa, json.toString(), 0);
                    }
                    //  app id 启动设备数
                    value = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisAppIDActivationKeyConstant.IOS_APP_ID_STARTUP_DEVICE_INFO + appID + ":" + idfa);
                    if (value == null) {
                        JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisAppIDActivationKeyConstant.IOS_APP_ID_STARTUP_DEVICE_COUNT + appID);
                        JedisUtils.set(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisAppIDActivationKeyConstant.IOS_APP_ID_STARTUP_DEVICE_INFO + appID + ":" + idfa, json.toString(), 0);
                    }
                    //  channel id 启动设备数
                    value = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisChannelIDActivationKeyConstant.IOS_CHANNEL_ID_STARTUP_DEVICE_INFO + channelID + ":" + idfa);
                    if (value == null) {
                        JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisChannelIDActivationKeyConstant.IOS_CHANNEL_ID_STARTUP_DEVICE_COUNT + channelID);
                        JedisUtils.set(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisChannelIDActivationKeyConstant.IOS_CHANNEL_ID_STARTUP_DEVICE_INFO + channelID + ":" + idfa, json.toString(), 0);
                    }
                    //  package id 启动设备数
                    value = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisPackageIDActivationKeyConstant.IOS_PACKAGE_ID_STARTUP_DEVICE_INFO + packageID + ":" + idfa);
                    if (value == null) {
                        JedisUtils.incr(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisPackageIDActivationKeyConstant.IOS_PACKAGE_ID_STARTUP_DEVICE_COUNT + packageID);
                        JedisUtils.set(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisPackageIDActivationKeyConstant.IOS_PACKAGE_ID_STARTUP_DEVICE_INFO + packageID + ":" + idfa, json.toString(), 0);
                    }
                }
            }
        });

        ios.filter(new Function<JSONObject, Boolean>() {
            Map<String , JSONObject> map = new HashMap<>(16);
            @Override
            public Boolean call(JSONObject json) throws Exception {
                StringBuffer key = new StringBuffer();
                key.append(json.getInt("package_id"));
                key.append("#");
                key.append(json.getInt("child_id"));
                key.append("#");
                key.append(json.getInt("app_channel_id"));
                key.append("#");
                key.append(json.getInt("channel_id"));
                key.append("#");
                key.append(json.getInt("appid"));

                if(map.get(key.toString()) == null){
                    map.put(key.toString() , json);
                    return true;
                }
                return false;
            }
        }).foreach(new VoidFunction<JSONObject>() {
            @Override
            public void call(JSONObject json) {
                int appID = json.getInt("appid");
                int childID = json.getInt("child_id");
                int appChannelID = json.getInt("app_channel_id");
                int channelID = json.getInt("channel_id");
                int packageID = json.getInt("package_id");
                /*
                 *   保存 新增设备数
                 */
                // 保存 child id 新增设备数 (激活数量)
                String childIDNewDeviceCount = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisChildIDActivationKeyConstant.CHILD_ID_NEW_DEVICE_COUNT + childID);
                if (childIDNewDeviceCount != null) {
                    statChildDeviceActiveDao.saveChildIdNewDeviceCount(new Object[]{childID, childIDNewDeviceCount, childIDNewDeviceCount});
                }
                // 保存 app Channel id 新增设备数
                String appChannelIDNewDeviceCount = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisAppChannelIDActivationKeyConstant.APP_CHANNEL_ID_NEW_DEVICE_COUNT + appChannelID);
                if (appChannelIDNewDeviceCount != null) {
                    statAppChannelIdDeviceActiveDao.saveAppChannelIdNewDeviceCount(new Object[]{appChannelID, appChannelIDNewDeviceCount, appChannelIDNewDeviceCount});
                }
                // 保存 app id 新增设备数
                String appIDNewDeviceCount = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisAppIDActivationKeyConstant.APP_ID_NEW_DEVICE_COUNT + appID);
                if (appIDNewDeviceCount != null) {
                    statAppIdDeviceActiveDao.saveAppIdNewDeviceCount(new Object[]{appID, appIDNewDeviceCount, appIDNewDeviceCount});
                }
                // 保存 channel id 新增设备数
                String channelIDNewDevice = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisChannelIDActivationKeyConstant.CHANNEL_ID_NEW_DEVICE_COUNT + channelID);
                if (channelIDNewDevice != null) {
                    statChannelIdDeviceActiveDao.saveChannelIdNewDeviceCount(new Object[]{channelID, channelIDNewDevice, channelIDNewDevice});
                }
                // 保存 package id 新增设备数
                String packageIDNewDevice = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisPackageIDActivationKeyConstant.PACKAGE_ID_NEW_DEVICE_COUNT + packageID);
                if (packageIDNewDevice != null) {
                    statPackageIdDeviceActiveDao.savePackageIdNewDeviceCount(new Object[]{packageID, packageIDNewDevice, packageIDNewDevice});
                }

                /*
                 *  保存启动设备数
                 */
                // package id 启动设备数
                String packageIDStartupCount = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisPackageIDActivationKeyConstant.IOS_PACKAGE_ID_STARTUP_DEVICE_COUNT + packageID);
                if (packageIDStartupCount != null) {
                    statPackageIdDeviceActiveDao.savePackageIdDeviceStartUpCount(new Object[]{packageID, packageIDStartupCount, packageIDStartupCount});
                }
                // app channel id 启动设备数
                String appChannelIDStartupCount = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisAppChannelIDActivationKeyConstant.IOS_APP_CHANNEL_ID_STARTUP_DEVICE_COUNT + appChannelID);
                if (appChannelIDStartupCount != null) {
                    statAppChannelIdDeviceActiveDao.saveAppChannelIdDeviceStartUpCount(new Object[]{appChannelID, appChannelIDStartupCount, appChannelIDStartupCount});
                }
                // appid 启动设备数
                String appidStartupCount = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisAppIDActivationKeyConstant.IOS_APP_ID_STARTUP_DEVICE_COUNT + appID);
                if (appidStartupCount != null) {
                    statAppIdDeviceActiveDao.saveAppIdDeviceStartUpCount(new Object[]{appID, appidStartupCount, appidStartupCount});
                }
                //  channel  id  启动设备数
                String channelIDStartupCount = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisChannelIDActivationKeyConstant.IOS_CHANNEL_ID_STARTUP_DEVICE_COUNT + channelID);
                if (channelIDStartupCount != null) {
                    statChannelIdDeviceActiveDao.saveChannelIdDeviceStartUpCount(new Object[]{channelID, channelIDStartupCount, channelIDStartupCount});
                }
                // child id 启动设备数
                String childIDStartupCount = JedisUtils.get(JedisPoolConfigInfo.statRedisPoolKey, toDay + JedisChildIDActivationKeyConstant.IOS_CHILD_ID_STARTUP_DEVICE_COUNT + childID);
                if (childIDStartupCount != null) {
                    statChildDeviceActiveDao.saveChildIdDeviceStartUpCount(new Object[]{childID, childIDStartupCount, childIDStartupCount});
                }
            }
        });
    }
}
