package com.yb.trust;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * UEBA异常检测API调用客户端
 * 
 * 使用Apache HttpClient调用远程服务器上的UEBA检测服务
 * 
 * Maven依赖：
 * <dependency>
 * <groupId>org.apache.httpcomponents</groupId>
 * <artifactId>httpclient</artifactId>
 * <version>4.5.13</version>
 * </dependency>
 * 
 * <dependency>
 * <groupId>org.json</groupId>
 * <artifactId>json</artifactId>
 * <version>20230227</version>
 * </dependency>
 */
public class UEBADetectorCaller {

    // ===== 配置服务器地址 =====
    // TODO: 修改为您的服务器地址
    private static final String SERVER_HOST = "127.0.0.1"; // 修改为实际服务器IP
    private static final int SERVER_PORT = 5000;
    private static final String BASE_URL = "http://" + SERVER_HOST + ":" + SERVER_PORT;

    // API端点
    private static final String URL_HEALTH = BASE_URL + "/health";
    private static final String URL_MODEL_INFO = BASE_URL + "/model/info";
    private static final String URL_COMPREHENSIVE = BASE_URL + "/detect/comprehensive";
    private static final String URL_USER_LEVEL = BASE_URL + "/detect/user_level";
    private static final String URL_EVENT_LEVEL = BASE_URL + "/detect/event_level";
    private static final String URL_SINGLE_EVENT = BASE_URL + "/detect/single_event";

    /**
     * 健康检查
     * 
     * @return 服务是否健康
     */
    public static boolean healthCheck() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(URL_HEALTH);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                System.out.println("健康检查状态码: " + statusCode);
                System.out.println("响应: " + responseBody);

                if (statusCode == 200) {
                    JSONObject json = new JSONObject(responseBody);
                    return "healthy".equals(json.optString("status"));
                }
                return false;
            }
        } catch (Exception e) {
            System.err.println("健康检查失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取模型信息
     * 
     * @return 模型信息JSON对象，失败返回null
     */
    public static JSONObject getModelInfo() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(URL_MODEL_INFO);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                System.out.println("模型信息状态码: " + statusCode);
                System.out.println("响应: " + responseBody);

                if (statusCode == 200) {
                    return new JSONObject(responseBody);
                }
                return null;
            }
        } catch (Exception e) {
            System.err.println("获取模型信息失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 用户级别异常检测
     * 
     * @param userId       用户ID
     * @param behaviorData 用户行为数据
     * @return 检测结果，失败返回null
     */
    public static UserLevelResult detectUserLevel(String userId, BehaviorData behaviorData) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(URL_USER_LEVEL);

            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("Accept", "application/json");

            // 构建请求JSON
            JSONObject requestJson = new JSONObject();
            requestJson.put("user", userId);
            requestJson.put("behavior_data", behaviorData.toJSON());

            String json = requestJson.toString();
            System.out.println("发送用户级别检测请求: " + json.substring(0, Math.min(200, json.length())) + "...");

            StringEntity entity = new StringEntity(json, "UTF-8");
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                System.out.println("用户级别检测状态码: " + statusCode);

                if (statusCode >= 400) {
                    System.err.println("服务器错误: " + responseBody);
                    return null;
                }

                JSONObject responseJson = new JSONObject(responseBody);
                return UserLevelResult.fromJSON(responseJson);
            }
        } catch (Exception e) {
            System.err.println("用户级别检测失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 综合检测（用户级别 + 事件级别）
     * 
     * @param userId       用户ID
     * @param behaviorData 用户行为数据
     * @return 综合检测结果，失败返回null
     */
    public static ComprehensiveResult detectComprehensive(String userId, BehaviorData behaviorData) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(URL_COMPREHENSIVE);

            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("Accept", "application/json");

            // 构建请求JSON
            JSONObject requestJson = new JSONObject();
            requestJson.put("user", userId);
            requestJson.put("behavior_data", behaviorData.toJSON());

            String json = requestJson.toString();
            System.out.println("发送综合检测请求...");

            StringEntity entity = new StringEntity(json, "UTF-8");
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                System.out.println("综合检测状态码: " + statusCode);

                if (statusCode >= 400) {
                    System.err.println("服务器错误: " + responseBody);
                    return null;
                }

                JSONObject responseJson = new JSONObject(responseBody);
                return ComprehensiveResult.fromJSON(responseJson);
            }
        } catch (Exception e) {
            System.err.println("综合检测失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 单个事件实时检测
     * 
     * @param userId         用户ID
     * @param eventType      事件类型 (logon, device, file, http, email)
     * @param event          事件数据
     * @param historicalData 历史数据（可选，用于建立基线）
     * @return 事件检测结果，失败返回null
     */
    public static EventResult detectSingleEvent(String userId, String eventType,
            JSONObject event, BehaviorData historicalData) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(URL_SINGLE_EVENT);

            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("Accept", "application/json");

            // 构建请求JSON
            JSONObject requestJson = new JSONObject();
            requestJson.put("user", userId);
            requestJson.put("event_type", eventType);
            requestJson.put("event", event);
            if (historicalData != null) {
                requestJson.put("historical_data", historicalData.toJSON());
            }

            String json = requestJson.toString();
            System.out.println("发送单事件检测请求: " + json);

            StringEntity entity = new StringEntity(json, "UTF-8");
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                System.out.println("单事件检测状态码: " + statusCode);

                if (statusCode >= 400) {
                    System.err.println("服务器错误: " + responseBody);
                    return null;
                }

                JSONObject responseJson = new JSONObject(responseBody);
                return EventResult.fromJSON(responseJson);
            }
        } catch (Exception e) {
            System.err.println("单事件检测失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ===== 数据类 =====

    /**
     * 行为数据类
     */
    public static class BehaviorData {
        private List<LogonRecord> logonRecords = new ArrayList<>();
        private List<DeviceRecord> deviceRecords = new ArrayList<>();
        private List<FileRecord> fileRecords = new ArrayList<>();
        private List<HttpRecord> httpRecords = new ArrayList<>();
        private List<EmailRecord> emailRecords = new ArrayList<>();

        public void addLogonRecord(String date, String pc) {
            logonRecords.add(new LogonRecord(date, pc));
        }

        public void addDeviceRecord(String date, String pc, String activity) {
            deviceRecords.add(new DeviceRecord(date, pc, activity));
        }

        public void addFileRecord(String date, String pc, String filename) {
            fileRecords.add(new FileRecord(date, pc, filename));
        }

        public void addHttpRecord(String date, String pc, String url) {
            httpRecords.add(new HttpRecord(date, pc, url));
        }

        public void addEmailRecord(String date, String to, int size) {
            emailRecords.add(new EmailRecord(date, to, size));
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();

            JSONArray logonArray = new JSONArray();
            for (LogonRecord r : logonRecords) {
                logonArray.put(r.toJSON());
            }
            json.put("logon_records", logonArray);

            JSONArray deviceArray = new JSONArray();
            for (DeviceRecord r : deviceRecords) {
                deviceArray.put(r.toJSON());
            }
            json.put("device_records", deviceArray);

            JSONArray fileArray = new JSONArray();
            for (FileRecord r : fileRecords) {
                fileArray.put(r.toJSON());
            }
            json.put("file_records", fileArray);

            JSONArray httpArray = new JSONArray();
            for (HttpRecord r : httpRecords) {
                httpArray.put(r.toJSON());
            }
            json.put("http_records", httpArray);

            JSONArray emailArray = new JSONArray();
            for (EmailRecord r : emailRecords) {
                emailArray.put(r.toJSON());
            }
            json.put("email_records", emailArray);

            return json;
        }

        static class LogonRecord {
            String date;
            String pc;

            LogonRecord(String date, String pc) {
                this.date = date;
                this.pc = pc;
            }

            JSONObject toJSON() {
                JSONObject json = new JSONObject();
                json.put("date", date);
                json.put("pc", pc);
                return json;
            }
        }

        static class DeviceRecord {
            String date;
            String pc;
            String activity;

            DeviceRecord(String date, String pc, String activity) {
                this.date = date;
                this.pc = pc;
                this.activity = activity;
            }

            JSONObject toJSON() {
                JSONObject json = new JSONObject();
                json.put("date", date);
                json.put("pc", pc);
                json.put("activity", activity);
                return json;
            }
        }

        static class FileRecord {
            String date;
            String pc;
            String filename;

            FileRecord(String date, String pc, String filename) {
                this.date = date;
                this.pc = pc;
                this.filename = filename;
            }

            JSONObject toJSON() {
                JSONObject json = new JSONObject();
                json.put("date", date);
                json.put("pc", pc);
                json.put("filename", filename);
                return json;
            }
        }

        static class HttpRecord {
            String date;
            String pc;
            String url;

            HttpRecord(String date, String pc, String url) {
                this.date = date;
                this.pc = pc;
                this.url = url;
            }

            JSONObject toJSON() {
                JSONObject json = new JSONObject();
                json.put("date", date);
                json.put("pc", pc);
                json.put("url", url);
                return json;
            }
        }

        static class EmailRecord {
            String date;
            String to;
            int size;

            EmailRecord(String date, String to, int size) {
                this.date = date;
                this.to = to;
                this.size = size;
            }

            JSONObject toJSON() {
                JSONObject json = new JSONObject();
                json.put("date", date);
                if (to != null) {
                    json.put("to", to);
                }
                json.put("size", size);
                return json;
            }
        }
    }

    /**
     * 用户级别检测结果
     */
    public static class UserLevelResult {
        private String user;
        private double anomalyScore;
        private String riskLevel;
        private String riskPercentile;
        private boolean isAnomaly;
        private int featuresCount;

        public static UserLevelResult fromJSON(JSONObject json) {
            UserLevelResult result = new UserLevelResult();
            result.user = json.optString("user");
            result.anomalyScore = json.optDouble("anomaly_score");
            result.riskLevel = json.optString("risk_level");
            result.riskPercentile = json.optString("risk_percentile");
            result.isAnomaly = json.optBoolean("is_anomaly");
            result.featuresCount = json.optInt("features_count");
            return result;
        }

        // Getters
        public String getUser() {
            return user;
        }

        public double getAnomalyScore() {
            return anomalyScore;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public String getRiskPercentile() {
            return riskPercentile;
        }

        public boolean isAnomaly() {
            return isAnomaly;
        }

        public int getFeaturesCount() {
            return featuresCount;
        }

        @Override
        public String toString() {
            return String.format("UserLevelResult{user='%s', score=%.4f, risk='%s', percentile='%s', anomaly=%s}",
                    user, anomalyScore, riskLevel, riskPercentile, isAnomaly);
        }
    }

    /**
     * 综合检测结果
     */
    public static class ComprehensiveResult {
        private String user;
        private UserLevelResult userLevel;
        private JSONObject eventLevel;
        private JSONObject summary;
        private String timestamp;

        public static ComprehensiveResult fromJSON(JSONObject json) {
            ComprehensiveResult result = new ComprehensiveResult();
            result.user = json.optString("user");

            if (json.has("user_level")) {
                result.userLevel = UserLevelResult.fromJSON(json.getJSONObject("user_level"));
            }

            if (json.has("event_level")) {
                result.eventLevel = json.getJSONObject("event_level");
            }

            if (json.has("summary")) {
                result.summary = json.getJSONObject("summary");
            }

            result.timestamp = json.optString("timestamp");
            return result;
        }

        // Getters
        public String getUser() {
            return user;
        }

        public UserLevelResult getUserLevel() {
            return userLevel;
        }

        public JSONObject getEventLevel() {
            return eventLevel;
        }

        public JSONObject getSummary() {
            return summary;
        }

        public String getTimestamp() {
            return timestamp;
        }

        /**
         * 格式化输出报告
         */
        public String formatReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("=".repeat(70)).append("\n");
            sb.append(String.format("用户异常检测报告 - %s\n", user));
            sb.append("=".repeat(70)).append("\n\n");

            // 用户级别
            if (userLevel != null) {
                sb.append("【用户整体评估】\n");
                sb.append(String.format("  异常分数: %.4f\n", userLevel.getAnomalyScore()));
                sb.append(String.format("  风险等级: %s\n", userLevel.getRiskLevel()));
                sb.append(String.format("  风险分位: %s\n", userLevel.getRiskPercentile()));
                sb.append(String.format("  是否异常: %s\n\n", userLevel.isAnomaly() ? "是" : "否"));
            }

            // 事件摘要
            if (summary != null) {
                sb.append("【行为事件分析】\n");
                sb.append(String.format("  总事件数: %d\n", summary.optInt("total_events_analyzed")));
                sb.append(String.format("  异常事件数: %d\n", summary.optInt("anomaly_events_count")));
                sb.append(String.format("  异常率: %.2f%%\n\n", summary.optDouble("anomaly_rate") * 100));

                // 最异常的事件
                if (summary.has("top_anomaly_events")) {
                    JSONArray topEvents = summary.getJSONArray("top_anomaly_events");
                    if (topEvents.length() > 0) {
                        sb.append("【最可疑的行为事件（前5个）】\n");
                        for (int i = 0; i < Math.min(5, topEvents.length()); i++) {
                            JSONObject event = topEvents.getJSONObject(i);
                            sb.append(String.format("  %d. [%s] 分数: %.2f | 风险: %s\n",
                                    i + 1, event.optString("type"), event.optDouble("score"), event.optString("risk")));
                        }
                    }
                }
            }

            sb.append("=".repeat(70)).append("\n");
            return sb.toString();
        }

        @Override
        public String toString() {
            return String.format("ComprehensiveResult{user='%s', userLevel=%s, eventCount=%d, anomalyCount=%d}",
                    user, userLevel,
                    summary != null ? summary.optInt("total_events_analyzed") : 0,
                    summary != null ? summary.optInt("anomaly_events_count") : 0);
        }
    }

    /**
     * 单事件检测结果
     */
    public static class EventResult {
        private String user;
        private String eventType;
        private JSONObject event;
        private double anomalyScore;
        private String riskLevel;
        private JSONArray anomalies;

        public static EventResult fromJSON(JSONObject json) {
            EventResult result = new EventResult();
            result.user = json.optString("user");
            result.eventType = json.optString("event_type");
            result.event = json.optJSONObject("event");
            result.anomalyScore = json.optDouble("anomaly_score");
            result.riskLevel = json.optString("risk_level");
            result.anomalies = json.optJSONArray("anomalies");
            return result;
        }

        // Getters
        public String getUser() {
            return user;
        }

        public String getEventType() {
            return eventType;
        }

        public JSONObject getEvent() {
            return event;
        }

        public double getAnomalyScore() {
            return anomalyScore;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public JSONArray getAnomalies() {
            return anomalies;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("EventResult{user='%s', type='%s', score=%.2f, risk='%s'",
                    user, eventType, anomalyScore, riskLevel));

            if (anomalies != null && anomalies.length() > 0) {
                sb.append(", anomalies=[");
                for (int i = 0; i < anomalies.length(); i++) {
                    if (i > 0)
                        sb.append(", ");
                    sb.append(anomalies.getJSONObject(i).optString("type"));
                }
                sb.append("]");
            }
            sb.append("}");
            return sb.toString();
        }
    }

    // ===== 主函数示例 =====

    public static void main(String[] args) {
        System.out.println("=".repeat(70));
        System.out.println("UEBA异常检测API客户端测试");
        System.out.println("=".repeat(70));
        System.out.println("服务器地址: " + BASE_URL);
        System.out.println();

        // 1. 健康检查
        System.out.println("=== 1. 健康检查 ===");
        boolean isHealthy = healthCheck();
        System.out.println("服务状态: " + (isHealthy ? "正常" : "异常"));

        if (!isHealthy) {
            System.err.println("服务不可用，终止测试");
            return;
        }

        // 2. 获取模型信息
        System.out.println("\n=== 2. 模型信息 ===");
        JSONObject modelInfo = getModelInfo();
        if (modelInfo != null) {
            System.out.println("模型类型: " + modelInfo.optString("model_type"));
            System.out.println("特征维度: " + modelInfo.optInt("feature_count"));
            JSONObject perf = modelInfo.optJSONObject("expected_performance");
            if (perf != null) {
                System.out.println("预期性能:");
                System.out.println("  - 准确率: " + String.format("%.2f%%", perf.optDouble("accuracy") * 100));
                System.out.println("  - 召回率: " + String.format("%.2f%%", perf.optDouble("recall") * 100));
                System.out.println("  - F1分数: " + String.format("%.4f", perf.optDouble("f1_score")));
            }
        }

        // 3. 准备测试数据
        System.out.println("\n=== 3. 准备测试数据 ===");
        BehaviorData behaviorData = new BehaviorData();

        // 添加正常登录记录
        behaviorData.addLogonRecord("2010-01-04 08:30:00", "PC-1234");
        behaviorData.addLogonRecord("2010-01-04 09:15:00", "PC-1234");
        behaviorData.addLogonRecord("2010-01-05 08:45:00", "PC-1234");
        behaviorData.addLogonRecord("2010-01-05 17:30:00", "PC-1234");

        // 添加异常登录：深夜 + 陌生设备
        behaviorData.addLogonRecord("2010-01-06 23:45:00", "PC-9999");
        behaviorData.addLogonRecord("2010-01-07 02:30:00", "PC-9999");

        // 添加文件操作
        behaviorData.addFileRecord("2010-01-05 10:00:00", "PC-1234", "report.pdf");
        behaviorData.addFileRecord("2010-01-06 23:50:00", "PC-9999", "confidential.zip"); // 异常

        // 添加邮件
        behaviorData.addEmailRecord("2010-01-05 14:00:00", "colleague@company.com", 50000);
        behaviorData.addEmailRecord("2010-01-07 02:45:00", "external@gmail.com", 50000000); // 异常

        System.out.println("测试数据准备完成");

        // 4. 用户级别检测
        System.out.println("\n=== 4. 用户级别检测 ===");
        UserLevelResult userResult = detectUserLevel("TEST_USER_001", behaviorData);
        if (userResult != null) {
            System.out.println(userResult);
            if (userResult.isAnomaly()) {
                System.out.println("⚠️ 警告：用户行为异常！");
            }
        }

        // 5. 综合检测
        System.out.println("\n=== 5. 综合检测（用户+事件） ===");
        ComprehensiveResult compResult = detectComprehensive("TEST_USER_002", behaviorData);
        if (compResult != null) {
            System.out.println(compResult.formatReport());

            if (compResult.getUserLevel() != null && compResult.getUserLevel().isAnomaly()) {
                System.out.println("⚠️ 检测到异常用户！");
                System.out.println("建议采取措施:");
                System.out.println("  1. 加强监控");
                System.out.println("  2. 人工审查");
                System.out.println("  3. 限制敏感操作");
            }
        }

        // 6. 单事件实时检测
        System.out.println("\n=== 6. 单事件实时检测 ===");
        JSONObject suspiciousLogon = new JSONObject();
        suspiciousLogon.put("date", "2010-01-08 03:00:00"); // 凌晨3点
        suspiciousLogon.put("pc", "PC-UNKNOWN"); // 陌生设备

        EventResult eventResult = detectSingleEvent("TEST_USER_003", "logon", suspiciousLogon, behaviorData);
        if (eventResult != null) {
            System.out.println(eventResult);
            if (eventResult.getAnomalyScore() > 0.5) {
                System.out.println("⚠️ 警告：检测到可疑登录！");
                System.out.println("建议：阻止登录或要求二次验证");
            }
        }

        System.out.println("\n" + "=".repeat(70));
        System.out.println("测试完成");
        System.out.println("=".repeat(70));
    }
}
