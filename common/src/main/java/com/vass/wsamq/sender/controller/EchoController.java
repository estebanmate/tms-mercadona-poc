package com.vass.wsamq.sender.controller;

import lombok.AllArgsConstructor;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vass.wsamq.common.CustomHeaders;
import com.vass.wsamq.sender.service.EchoMessageHandler;

/**
 * @author s.nechkin
 */
@RestController
@RequestMapping(value = "/echo")
@AllArgsConstructor
public class EchoController {

    private final EchoMessageHandler echoMessageHandler;

    @PostMapping(consumes = "text/plain; charset=UTF-8")
    public void echo(@RequestHeader(CustomHeaders.HEADER_USERNAME) String username,
                     @RequestHeader(CustomHeaders.HEADER_SCOKET_SESSION_ID) String socketSession,
                     @RequestBody String message) {
        echoMessageHandler.handle(username, socketSession, message);
    }
}
