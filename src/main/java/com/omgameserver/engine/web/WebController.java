package com.omgameserver.engine.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping(value = "/omgameserver/v1")
class WebController {

    private final WebService webService;

    WebController(WebService webService) {
        this.webService = webService;
    }

    @PostMapping(path = "/authorizations", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AuthorizationResponse createAuthorization() throws NoSuchAlgorithmException, InterruptedException {
        WebService.Authorization authorization = webService.createAuthorization();
        return new AuthorizationResponse(authorization.getKeyUid(), authorization.getSecretKey().getEncoded());
    }

    class AuthorizationResponse {
        private final long keyUid;
        private final byte[] secretKey;

        AuthorizationResponse(long keyUid, byte[] secretKey) {
            this.keyUid = keyUid;
            this.secretKey = secretKey;
        }

        public long getKeyUid() {
            return keyUid;
        }

        public byte[] getSecretKey() {
            return secretKey;
        }
    }
}
