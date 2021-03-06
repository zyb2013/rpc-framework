package com.xiaoluo.rpc.registry.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.xiaoluo.rpc.exception.CannotDelActiveNodeException;
import com.xiaoluo.rpc.exception.ConfigBindNotReleaseException;
import com.xiaoluo.rpc.registry.RegistryNode;
import com.xiaoluo.rpc.registry.RegistryClusterGroup;
import com.xiaoluo.rpc.registry.event.*;
import com.xiaoluo.rpc.dispatch.ISession;
import com.xiaoluo.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Caedmon on 2015/9/22.
 */
public class RegistryManager {
    public static final String NODE_DATA_FILE_NAME = "node.data";
    public static final String CONFIG_DATA_FILE_NAME = "config.data";
    private static final Logger log = LoggerFactory.getLogger(RegistryManager.class);
    private Map<String, RegistryNode> allNodeMap = new ConcurrentHashMap<>();
    private Map<String, String> configMap = new ConcurrentHashMap<>();
    private static final SerializerFeature[] JSON_SERIALIZERFEATURES=new SerializerFeature[]{SerializerFeature.WriteClassName};
    private static final RegistryManager instance=new RegistryManager();
    private Map<String,RegistryClusterGroup> groupMap=new ConcurrentHashMap<>();
    private volatile boolean started;
    private static final String DATA_DIR="data";
    private final ScheduledExecutorService threadPool= Executors.newScheduledThreadPool(1);
    private static final Object lock=new Object();
    private enum SaveType{
        NODES,CONFIG;
    }
    public static RegistryManager getInstance(){
        return instance;
    }
    public void start(){
        if(started){
            log.warn("RegistryManager has already started");
            return;
        }
        String nodesText=null;
        String configText=null;
        try{
            Util.ifNotExistCreate(DATA_DIR+ File.separator + NODE_DATA_FILE_NAME);
            Util.ifNotExistCreate(DATA_DIR + File.separator + CONFIG_DATA_FILE_NAME);
            nodesText=Util.loadFromDisk(DATA_DIR + File.separator + NODE_DATA_FILE_NAME);
            configText =Util.loadFromDisk(DATA_DIR + File.separator + CONFIG_DATA_FILE_NAME);
        }catch (IOException e){
            throw new IllegalStateException("RegistryManager init error",e);
        }
        if(nodesText!=null&&!nodesText.isEmpty()){
            this.groupMap=JSON.parseObject(nodesText, ConcurrentHashMap.class);
            for(RegistryClusterGroup group:groupMap.values()){
                for(RegistryNode node:group.getNodes().values()){
                    allNodeMap.put(node.getKey(),node);
                }
            }
        }
        if(configText!=null&&!configText.isEmpty()){
            this.configMap = JSON.parseObject(configText, ConcurrentHashMap.class);
        }
        //先把所有节点状态重置为false
        for(RegistryNode node:allNodeMap.values()){
            node.setActive(false);
        }
        threadPool.scheduleAtFixedRate(new KeepAliveTask(),0, KeepAliveTask.TIME_OUT_PERIOD, TimeUnit.SECONDS);
        started=true;
        log.info("Init registry server data");
    }
    private RegistryManager(){

    }

    public Map<String, RegistryNode> getAllNodeMap() {
        return allNodeMap;
    }

    public Map<String, String> getConfigMap() {
        return configMap;
    }
    public RegistryNode register(ISession session, String[] groups, String host, int port) throws Exception{
        String group=groups[0];
        String key=group+"-"+host+":"+port;
        RegistryNode node=allNodeMap.get(key);
        if (node!=null) {
            ISession currentSession=node.getSession();
            if(currentSession!=null&&currentSession.isActive()){
                throw new IllegalStateException("Node exists " + key);
            }else{
                //重连
                node.reconnect(session);
            }
        }else{
            node= RegistryNode.build(session, group, host, port);
            node.setActive(true);
            allNodeMap.put(node.getKey(), node);
            RegistryClusterGroup registryClusterGroup =groupMap.get(group);
            if(registryClusterGroup ==null){
                synchronized (lock){
                    if(registryClusterGroup ==null){
                        registryClusterGroup =new RegistryClusterGroup();
                        registryClusterGroup.setKey(group);
                    }
                }

            }
            registryClusterGroup.add(node);
            groupMap.put(group, registryClusterGroup);
            log.info("Rpc node register success:{}", node.getKey());
        }
        for(RegistryNode n: allNodeMap.values()){
            //不通知自己
            if(n.getSession()==session){
                continue;
            }
            if(!n.isActive()){
                continue;
            }
            NodeActiveEvent event=new NodeActiveEvent();
            event.setNodeKey(node.getKey());
            event.setGroup(node.getGroup());
            event.setHost(node.getHost());
            event.setPort(node.getPort());
            n.notifyEvent(event);
        }
        save(SaveType.NODES);
        return node;
    }

    public RegistryNode delete(String nodeKey) {
        log.info("Delete registry node:"+nodeKey);
        RegistryNode node=allNodeMap.get(nodeKey);
        if(node==null){
            return null;
        }
        //节点为活动状态不能删除
        if(node.isActive()){
            throw new CannotDelActiveNodeException("The registry node "+nodeKey+" is active");
        }
        allNodeMap.remove(nodeKey);
        if(node!=null){
            String group=node.getGroup();
            RegistryClusterGroup registryClusterGroup =groupMap.get(group);
            if(registryClusterGroup !=null){
                registryClusterGroup.delete(nodeKey);
            }
        }
        NodeInActiveEvent event=new NodeInActiveEvent();
        event.setNodeKey(nodeKey);
        notifyEventToAll(event);
        save(SaveType.NODES);
        return node;
    }
    public void notifyEventToAll(RegistryEvent event){
        for(RegistryNode n:allNodeMap.values()){
            n.notifyEvent(event);
        }
    }
    public void save(SaveType type){
        String text =null;
        try{
            switch (type){
                case NODES:
                    text=JSON.toJSONString(groupMap, JSON_SERIALIZERFEATURES);
                    Util.saveToDisk(text, DATA_DIR + File.separator + NODE_DATA_FILE_NAME);
                    break;
                case CONFIG:
                    text = JSON.toJSONString(configMap,JSON_SERIALIZERFEATURES);
                    Util.saveToDisk(text, DATA_DIR + File.separator + CONFIG_DATA_FILE_NAME);

                    break;
                default:
                    break;
            }
            log.debug("Save {} data to disk success:{}",type, text);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public Collection<RegistryNode> getAllNodeList() {
        return allNodeMap.values();
    }

    public RegistryNode getRpcNode(String nodeKey) {
        return allNodeMap.get(nodeKey);
    }

    public void updateConfig(String configKey, String configValue) {
        configMap.put(configKey, configValue);
        log.info("Monitor server update config:key={},config={}", configKey, configValue);
        for(RegistryNode node:getAllNodeMap().values()){
            if(node.getBindConfigKeySet().contains(configKey)) {
                ConfigEvent event = new ConfigEvent();
                event.addConfigEntity(configKey, configValue);
                node.notifyEvent(event);
            }
        }
        save(SaveType.CONFIG);
    }

    public void updateConfigBind(String nodeKey, Set<String> configKeySet) {
        log.info("Update config bind:nodeKey={},configKeySet={}", nodeKey, configKeySet);
        RegistryNode registryNode =allNodeMap.get(nodeKey);
        registryNode.setBindConfigKeySet(configKeySet);
        ConfigEvent configEvent=new ConfigEvent();
        for(String configKey:configKeySet){
            String configValue= getConfig(configKey);
            configEvent.addConfigEntity(configKey,configValue);
        }
        registryNode.notifyEvent(configEvent);
        save(SaveType.NODES);
    }

    public String getConfig(String configKey) {
        return configMap.get(configKey);
    }

    public Map<String, String> getConfigMapByKeys(Set<String> configKeySet) {
        Map<String, String> result = new HashMap<>();
        for (String key : configKeySet) {
            String configValue = getConfig(key);
            result.put(key, configValue);
        }
        return result;
    }

    public Map<String, String> getNodeConfig(String nodeKey) {
        RegistryNode registryNode = getRpcNode(nodeKey);
        if (registryNode != null) {
            Set<String> keySet = registryNode.getBindConfigKeySet();
            return getConfigMapByKeys(keySet);
        }
        return null;
    }
    public void disconnectNode(String nodeKey){
        log.info("Disconnect registry node:"+nodeKey);
        RegistryNode node=allNodeMap.get(nodeKey);
        if(null!=node){
            node.setSession(null);
            node.setActive(false);
            node.disconnect();
            NodeInActiveEvent event=new NodeInActiveEvent();
            event.setNodeKey(nodeKey);
            notifyEventToAll(event);
        }
        save(SaveType.NODES);
    }
    public void deleteConfig(String configKey){
        if(configKey!=null){
            log.info("Delete config:{}", configKey);
            Set<String> nodeSet=new HashSet<>();
            for(RegistryNode node:allNodeMap.values()){
                if(node.getBindConfigKeySet().contains(configKey)){
                    nodeSet.add(node.getKey());
                }
            }
            if(!nodeSet.isEmpty()){
                throw new ConfigBindNotReleaseException("You must release the config bind first configKey="+configKey+",bindNodes="+nodeSet.toString());
            }
            configMap.remove(configKey);
            save(SaveType.CONFIG);
        }


    }

    public void updateRouteTable(String nodeKey,String routeTableString) throws Exception{
        log.info("Update route table:node={},content={}", nodeKey, routeTableString);
        InputStream inputStream=new ByteArrayInputStream(routeTableString.getBytes());
        Properties properties=new Properties();
        properties.load(inputStream);
        inputStream.close();
        RegistryNode node=allNodeMap.get(nodeKey);
        node.setRouteTable(properties);
        RouteEvent event=new RouteEvent();
        event.setRouteTable(properties);
        node.notifyEvent(event);
        save(SaveType.NODES);

    }
    public String showRouteTable(String nodeKey){
        log.info("Show route table:node={}",nodeKey);
        StringBuilder properties=new StringBuilder();
        Properties routeTable=allNodeMap.get(nodeKey).getRouteTable();
        for(Map.Entry<Object,Object> entry:routeTable.entrySet()){
            properties.append(entry.getKey()).append("=").append(entry.getValue()).append("\r\n");
        }
        return properties.toString();
    }
    public Map<String,RegistryClusterGroup> getGroupMap(){
        return groupMap;
    }
}
