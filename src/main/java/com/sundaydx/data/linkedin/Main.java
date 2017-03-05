package com.sundaydx.data.linkedin;

import com.sundaydx.data.linkedin.net.Login;

/**
 * Author: SundayDX
 * Date: 2017/3/5
 */
public class Main {
    public static void main(String[] args) {
        Login login = new Login();
        try {
            login.login("账号", "密码");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
