package org.skrxiaoyou.reo.reologin;

import java.io.Serializable;

public class UserData implements Serializable {
    UserData(String _encrypted_password, long _last_login_timestamp) {
        super();
        encrypted_password = _encrypted_password;
        last_login_timestamp=_last_login_timestamp;
    }
    String encrypted_password;
    long last_login_timestamp;
}