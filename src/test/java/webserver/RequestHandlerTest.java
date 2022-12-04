package webserver;

import org.junit.jupiter.api.Test;
import util.HttpRequestUtils;

import java.util.Map;

class RequestHandlerTest {

    @Test
    public void param_split_테스트(){
        String url = "\\user\\create?userId=aa&password=bb&name=cc&email=dd%40naver.com";

        String[] split = url.split("\\?");
        System.out.println(split[0]);
        System.out.println(split[1]);

    }

    @Test
    public void HttpRequestUtils_parseQueryString_테스트(){
        Map<String, String> stringMap = HttpRequestUtils.parseQueryString("/index.html");

        for(String key : stringMap.keySet()){
            System.out.println(key + " : " + stringMap.get(key));
        }
        System.out.println("============");

        stringMap = HttpRequestUtils.parseQueryString("aaa=3&bb=wjdfbu");
        for(String key : stringMap.keySet()){
            System.out.println(key + " : " + stringMap.get(key));
        }
        System.out.println("============");

        stringMap = HttpRequestUtils.parseQueryString("");
        for(String key : stringMap.keySet()){
            System.out.println(key + " : " + stringMap.get(key));
        }
        System.out.println("============");

    }


}
