package wall;

import java.util.*;

/**
 * @Author: Wang keLong
 * @DateTime: 1:43 2020/10/29
 */
public class Wall {
    private List<String> urls = new ArrayList<>(); //禁止访问的url
    private List<String> hosts = new ArrayList<>(); //禁止访问服务器的主机
    private Map<String, String> redirectHosts = new HashMap<>(); //重定向主机
    private Map<String, String> redirectUrls = new HashMap<>(); //重定向的url

    public Wall() {
        //this.urls.add("http://jwts.hit.edu.cn/");
        //this.hosts.add("192.168.0.1");
        //this.hosts.add("127.0.0.1");
        redirectHosts.put("jwts.hit.edu.cn", "today.hit.edu.cn");
        redirectUrls.put("http://jwts.hit.edu.cn/", "http://today.hit.edu.cn/");
    }

    public boolean isForbiddenUrl(String url) {
        if (urls.contains(url)) return true;
        else return false;
    }

    public boolean isForbiddenHost(String host) {
        if (hosts.contains(host)) return true;
        else return false;
    }

    public String[] isFishing(String host, String url) {
        if (redirectHosts.containsKey(host) && redirectUrls.containsKey(url)) {
            String[] strings = new String[2];
            strings[0] = redirectHosts.get(host);
            strings[1] = redirectUrls.get(url);
            return strings;
        } else {
            return null;
        }
    }

    /**
     * 构造对被禁止的主机的响应报文
     *
     * @param stateCode 响应状态码
     */
    public String getResponse(int stateCode) {
        StringBuffer sb = new StringBuffer();

        return sb.toString();
    }

}
