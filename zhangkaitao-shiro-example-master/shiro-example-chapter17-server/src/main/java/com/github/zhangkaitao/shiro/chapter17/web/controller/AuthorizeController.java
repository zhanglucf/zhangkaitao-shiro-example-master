package com.github.zhangkaitao.shiro.chapter17.web.controller;

import com.github.zhangkaitao.shiro.chapter17.Constants;
import com.github.zhangkaitao.shiro.chapter17.service.ClientService;
import com.github.zhangkaitao.shiro.chapter17.service.OAuthService;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * <p>User: Zhang Kaitao
 * <p>Date: 14-2-16
 * <p>Version: 1.0
 */
@Controller
public class AuthorizeController {

    @Autowired
    private OAuthService oAuthService;
    @Autowired
    private ClientService clientService;

    @RequestMapping("/authorize")
    public Object authorize(
            Model model,
            HttpServletRequest request)
            throws URISyntaxException, OAuthSystemException {

        try {
            //??????OAuth ????????????
            OAuthAuthzRequest oauthRequest = new OAuthAuthzRequest(request);

            //????????????????????????id????????????
            if (!oAuthService.checkClientId(oauthRequest.getClientId())) {
                OAuthResponse response =
                        OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                                .setError(OAuthError.TokenResponse.INVALID_CLIENT)
                                .setErrorDescription(Constants.INVALID_CLIENT_DESCRIPTION)
                                .buildJSONMessage();
                return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
            }


            Subject subject = SecurityUtils.getSubject();
            //????????????????????????????????????????????????
            if(!subject.isAuthenticated()) {
                if(!login(subject, request)) {//????????????????????????????????????
                    model.addAttribute("client", clientService.findByClientId(oauthRequest.getClientId()));
                    return "oauth2login";
                }
            }

            String username = (String)subject.getPrincipal();
            //???????????????
            String authorizationCode = null;
            //responseType???????????????CODE???????????????TOKEN
            String responseType = oauthRequest.getParam(OAuth.OAUTH_RESPONSE_TYPE);
            if (responseType.equals(ResponseType.CODE.toString())) {
                OAuthIssuerImpl oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
                authorizationCode = oauthIssuerImpl.authorizationCode();
                oAuthService.addAuthCode(authorizationCode, username);
            }

            //??????OAuth????????????
            OAuthASResponse.OAuthAuthorizationResponseBuilder builder =
                    OAuthASResponse.authorizationResponse(request, HttpServletResponse.SC_FOUND);
            //???????????????
            builder.setCode(authorizationCode);
            //?????????????????????????????????
            String redirectURI = oauthRequest.getParam(OAuth.OAUTH_REDIRECT_URI);

            //????????????
            final OAuthResponse response = builder.location(redirectURI).buildQueryMessage();

            //??????OAuthResponse??????ResponseEntity??????
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(new URI(response.getLocationUri()));
            return new ResponseEntity(headers, HttpStatus.valueOf(response.getResponseStatus()));
        } catch (OAuthProblemException e) {

            //????????????
            String redirectUri = e.getRedirectUri();
            if (OAuthUtils.isEmpty(redirectUri)) {
                //???????????????????????????redirectUri????????????
                return new ResponseEntity("OAuth callback url needs to be provided by client!!!", HttpStatus.NOT_FOUND);
            }

            //?????????????????????????error=???
            final OAuthResponse response =
                    OAuthASResponse.errorResponse(HttpServletResponse.SC_FOUND)
                            .error(e).location(redirectUri).buildQueryMessage();
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(new URI(response.getLocationUri()));
            return new ResponseEntity(headers, HttpStatus.valueOf(response.getResponseStatus()));
        }
    }

    private boolean login(Subject subject, HttpServletRequest request) {
        if("get".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return false;
        }

        UsernamePasswordToken token = new UsernamePasswordToken(username, password);

        try {
            subject.login(token);
            return true;
        } catch (Exception e) {
            request.setAttribute("error", "????????????:" + e.getClass().getName());
            return false;
        }
    }
}