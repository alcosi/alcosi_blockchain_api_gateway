package com.alcosi.nft.apigateway.config.path;

import com.alcosi.nft.apigateway.config.path.dto.PathAuthority;
import com.alcosi.nft.apigateway.service.predicate.PredicateMatcherType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties("filter.config.path")
public class SecurityRoutesProperties {
    private SecurityRouteConfig security = new SecurityRouteConfig();
    private SecurityRouteConfig validation = new SecurityRouteConfig();

    public SecurityRouteConfig getSecurity() {
        return security;
    }

    public void setSecurity(SecurityRouteConfig security) {
        this.security = security;
    }

    public SecurityRouteConfig getValidation() {
        return validation;
    }

    public void setValidation(SecurityRouteConfig validation) {
        this.validation = validation;
    }

    public static class Type {
        private PathConfigurationComponent.Method method = PathConfigurationComponent.Method.ETH_JWT;
        private PredicateMatcherType match = PredicateMatcherType.MATCH_IF_CONTAINS_IN_LIST;
        private PathConfigurationComponent.PredicateType predicate = PathConfigurationComponent.PredicateType.MVC;

        public PredicateMatcherType getMatch() {
            return match;
        }

        public void setMatch(PredicateMatcherType match) {
            this.match = match;
        }

        public PathConfigurationComponent.PredicateType getPredicate() {
            return predicate;
        }

        public void setPredicate(PathConfigurationComponent.PredicateType predicate) {
            this.predicate = predicate;
        }

        public PathConfigurationComponent.Method getMethod() {
            return method;
        }

        public void setMethod(PathConfigurationComponent.Method method) {
            this.method = method;
        }

    }

    public class SecurityRouteConfig {
        private Map<String,String> path=Map.of();

        public Map<String, String> getPath() {
            return path;
        }

        public void setPath(Map<String, String> path) {
            this.path = path;
        }

        private SecurityRoutesProperties.Type type = new SecurityRoutesProperties.Type();
        private String baseAuthorities = "[{\"list\":[\"ALL\"],\"checkMode\":\"ALL\"}]";
        private PathAuthority.AuthoritiesCheck baseAuthoritiesCheckType = PathAuthority.AuthoritiesCheck.ANY;
        public SecurityRoutesProperties.Type getType() {
            return type;
        }

        public void setType(SecurityRoutesProperties.Type type) {
            this.type = type;
        }

        public String getBaseAuthorities() {
            return baseAuthorities;
        }//PathAuthority

        public void setBaseAuthorities(String baseAuthorities) {
            this.baseAuthorities = baseAuthorities;
        }

        public PathAuthority.AuthoritiesCheck getBaseAuthoritiesCheckType() {
            return baseAuthoritiesCheckType;
        }

        public void setBaseAuthoritiesCheckType(PathAuthority.AuthoritiesCheck baseAuthoritiesCheckType) {
            this.baseAuthoritiesCheckType = baseAuthoritiesCheckType;
        }
    }
}