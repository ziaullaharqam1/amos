package com.amos.ams.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private final Jwt jwt = new Jwt();
    private final Kafka kafka = new Kafka();
    private final Cors cors = new Cors();
    private final Grok grok = new Grok();

    public Jwt getJwt() { return jwt; }
    public Kafka getKafka() { return kafka; }
    public Cors getCors() { return cors; }
    public Grok getGrok() { return grok; }

    public static class Jwt {
        private String secret;
        private long expirationMs;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpirationMs() { return expirationMs; }
        public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
    }

    public static class Kafka {
        private Topics topics = new Topics();
        public Topics getTopics() { return topics; }
        public void setTopics(Topics topics) { this.topics = topics; }
        public static class Topics {
            private String notifications;
            private String audit;
            public String getNotifications() { return notifications; }
            public void setNotifications(String notifications) { this.notifications = notifications; }
            public String getAudit() { return audit; }
            public void setAudit(String audit) { this.audit = audit; }
        }
    }

    public static class Cors {
        private String allowedOrigins;
        public String getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class Grok {
        /** free = local no-credit model; xai = Grok API (needs console credits). */
        private String provider = "free";
        private String apiKey = "";
        private String model = "local-free";
        private String baseUrl = "https://api.x.ai/v1";
        private int maxSteps = 8;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getMaxSteps() { return maxSteps; }
        public void setMaxSteps(int maxSteps) { this.maxSteps = maxSteps; }

        public boolean isFree() {
            String p = provider == null ? "free" : provider.trim();
            return p.isEmpty() || "free".equalsIgnoreCase(p) || "local".equalsIgnoreCase(p);
        }

        public boolean isConfigured() {
            return isFree() || (apiKey != null && !apiKey.isBlank());
        }

        public String displayModel() {
            return isFree() ? "local-free" : (model == null || model.isBlank() ? "grok-4.6" : model);
        }
    }
}
