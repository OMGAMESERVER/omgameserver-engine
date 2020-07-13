package com.omgameserver.engine.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@RestController
@RequestMapping(value = "/omgameserver/v1")
class WebController {

    private final WebService webService;

    WebController(WebService webService) {
        this.webService = webService;
    }

    @PostMapping(path = "/access", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AccessResponse createAccess() throws InterruptedException {
        WebService.Access access = webService.createAccess();
        return new AccessResponse(access.getAccessKey());
    }

    class AccessResponse {

        private final long accessKey;

        AccessResponse(long accessKey) {
            this.accessKey = accessKey;
        }

        public long getAccessKey() {
            return accessKey;
        }
    }
}
