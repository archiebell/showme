package invalid.showme.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JSONStuff
{
    public static List<String> JSONArrayToStringArray(String json) throws JSONException {
        JSONArray jObj = new JSONArray(json);
        List<String> ret = new ArrayList<>();
        for(int i=0; i<jObj.length(); i++)
            ret.add(jObj.getString(i));
        return ret;
    }

    public static String ExtractKeyFromJSONDictionary(String json, String key) throws JSONException {
        JSONObject jObj = new JSONObject(json);
        Object o = jObj.get(key);
        if(o == null) {
            return null;
        } else {
            return o.toString();
        }
    }

    public static Map<String, String> JSONObjectToMap(String json) throws JSONException {
        Map<String, String> ret = new Hashtable<>();

        JSONObject jObj = new JSONObject(json);
        Iterator<String> keysItr = jObj.keys();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            String value = jObj.get(key).toString();
            ret.put(key, value);
        }

        return ret;
    }

    public static String MapToJSON(Map<String, Object> hs) {
        JSONObject o = new JSONObject(hs);
        return o.toString();
    }
}
