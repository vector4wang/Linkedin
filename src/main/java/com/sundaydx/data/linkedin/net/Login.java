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
import org.jsoup.select.Elements;

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
    private String formUrl = null;
    private Map<String, String> inputForm = new HashMap<>();
    private String username = null;
    private String passwd = null;

    public Login(String username, String passwd) {
        this.username = username;
        this.passwd = passwd;
    }

    public void login() throws Exception {
        RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD_STRICT).build();
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

        try {
            HttpGet get = new HttpGet("https://www.linkedin.com/");

            CloseableHttpResponse response = httpClient.execute(get);
            setCookie(response);
            getForm(EntityUtils.toString(response.getEntity()));
            response.close();

            List<NameValuePair> valuePairs = new LinkedList<>();
            for (String name : this.inputForm.keySet()) {
                valuePairs.add(new BasicNameValuePair(name, this.inputForm.get(name)));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(valuePairs, Consts.UTF_8);

            HttpPost post = new HttpPost("https://www.linkedin.com/uas/login-submit");
            post.setHeader("Cookie", getCookie());
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
            get.setHeader("Cookie", getCookie());
            httpClient.execute(get);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getForm(String html) {
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();
        this.formUrl = form.attr("action");

        Elements input = form.select("input");
        for (Element ele : input) {
            String name = ele.attr("name");
            String value = ele.attr("value");

            if (name != null && value != null) {
                this.inputForm.put(name, value);
            }
        }
        this.inputForm.put("session_key", this.username);
        this.inputForm.put("session_password", this.passwd);
    }

    private String getCookie() {
        String cookiesTmp = "";
        for (String key : this.cookieMap.keySet()) {
            cookiesTmp += key + "=" + this.cookieMap.get(key) + ";";
        }

        return cookiesTmp.substring(0, cookiesTmp.length() - 2);
    }

    private void setCookie(HttpResponse httpResponse) {
        Header headers[] = httpResponse.getHeaders("Set-Cookie");
        if (headers == null || headers.length == 0) {
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
            this.cookieMap.put(c.split("=")[0], c.split("=").length == 1 ? "" :
                    (c.split("=").length == 2 ? c.split("=")[1] : c.split("=", 2)[1]));
        }
    }

}
