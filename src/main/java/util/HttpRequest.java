package util;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@Getter
@Setter
public class HttpRequest {

    private String url;
    private Map<String, String> header = new HashMap<>();
    private Map<String, String> params = new HashMap<>();

    @Override
    public String toString() {
        String value = "HttpRequest{"
                + "url='" + url + "' ";

        for(String key : header.keySet()){
            value += "" + key + "='" +header.get(key) +"' ";
        }

        return value += "}";
    }
}
