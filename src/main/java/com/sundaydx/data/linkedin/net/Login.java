package com.sundaydx.data.linkedin.net;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Author: SundayDX
 * Date: 2017/3/5
 * <p>
 * 很潦草的写了一下登录的示范
 * Linkedin 的登录流程需要获取对应的csrf的参数，同时需要注意header的完整程度
 * 登录成功了，返回的是302到nhome路径上，但是也会到verity的页面，进行二步验证
 * 200状态则是登录失败，这个很反直觉
 */
public class Login {

    private Map<String, String> cookieMap = new HashMap<>(64);
    private String cookie = "";
    private Document doc = null;

    public void login(String user, String passwd) throws Exception {
        RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD_STRICT).build();
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

        try {
            HttpGet get = new HttpGet("https://www.linkedin.com/");

            CloseableHttpResponse response = httpClient.execute(get);
            setCookie(response);
            String responseHtml = EntityUtils.toString(response.getEntity());
            this.doc = Jsoup.parse(responseHtml);

            String loginCsrfParam = getComponent("#loginCsrfParam-login", "value");
            String sourceAlias = getComponent("#sourceAlias-login", "value");
            System.out.println("csrfValue:" + loginCsrfParam);
            response.close();


            List<NameValuePair> valuePairs = new LinkedList<>();
            valuePairs.add(new BasicNameValuePair("session_key", user));
            valuePairs.add(new BasicNameValuePair("session_password", passwd));
            valuePairs.add(new BasicNameValuePair("isJsEnabled", "false"));
            valuePairs.add(new BasicNameValuePair("loginCsrfParam", loginCsrfParam));
            valuePairs.add(new BasicNameValuePair("sourceAlias", sourceAlias));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(valuePairs, Consts.UTF_8);

            HttpPost post = new HttpPost("https://www.linkedin.com/uas/login-submit");
            post.setHeader("Cookie", this.cookie);
            post.setHeader("origin", "https://www.linkedin.com");
            post.setHeader("pragma", "no-cache");
            post.setHeader("referer", "https://www.linkedin.com/");
            post.setHeader("upgrade-insecure-requests", "1");
            post.setHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
            post.setEntity(entity);
            response = httpClient.execute(post);
            setCookie(response);
            Header[] headers = response.getHeaders("location");

            if (headers.length != 1) {
                throw new Exception("登录失败");
            }

            get = new HttpGet("http://www.linkedin.com/nhome/");
            get.setHeader("Cookie", this.cookie);
            response = httpClient.execute(get);
            responseHtml = EntityUtils.toString(response.getEntity());
            response.close();
            System.out.println(responseHtml);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getComponent(String selectQuery, String attr) throws IOException {
        if (this.doc != null) {
            Element queryDom = this.doc.select(selectQuery).first();
            return queryDom.attr(attr);
        }

        throw new IOException("尚未解析网页");
    }

    private void setCookie(HttpResponse httpResponse) {
        System.out.println("----setCookieStore");
        Header headers[] = httpResponse.getHeaders("Set-Cookie");
        if (headers == null || headers.length == 0) {
            System.out.println("----there are no cookies");
            return;
        }
        String cookie = "";
        for (int i = 0; i < headers.length; i++) {
            cookie += headers[i].getValue();
            if (i != headers.length - 1) {
                cookie += ";";
            }
        }

        String cookies[] = cookie.split(";");
        for (String c : cookies) {
            c = c.trim();
            if (this.cookieMap.containsKey(c.split("=")[0])) {
                this.cookieMap.remove(c.split("=")[0]);
            }
            this.cookieMap.put(c.split("=")[0], c.split("=").length == 1 ? "" : (c.split("=").length == 2 ? c.split("=")[1] : c.split("=", 2)[1]));
        }
        System.out.println("----setCookieStore success");
        String cookiesTmp = "";
        for (String key : this.cookieMap.keySet()) {
            cookiesTmp += key + "=" + this.cookieMap.get(key) + ";";
        }

        this.cookie = cookiesTmp.substring(0, cookiesTmp.length() - 2);
    }

}
