package invalid.showme.tests;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import invalid.showme.util.JSONStuff;

public class JSONStuffTest extends TestCaseWithUtils
{
    public void testJsonArray() throws UnsupportedEncodingException, JSONException {
        String[] expected1 = {"abcde", "fghej"};
        List<String> actual1 = JSONStuff.JSONArrayToStringArray("[\"abcde\",\"fghej\"]");
        assertTrue(CompareArrays(expected1, actual1));

        String[] expected2 = {"abcde", "fgh'ej"};
        List<String> actual2 = JSONStuff.JSONArrayToStringArray("[\"abcde\", \"fgh'ej\"]");
        assertTrue(CompareArrays(expected2, actual2));

        String[] expected3 = {"ab\"cde", "fgh\nej"};
        List<String> actual3 = JSONStuff.JSONArrayToStringArray("[\"ab\\\"cde\", \"fgh\nej\"]");
        assertTrue(CompareArrays(expected3, actual3));
    }

    public void testJsonBuild() throws UnsupportedEncodingException, JSONException {
        Map<String, Object> data1 = new HashMap<>();
        data1.put("key1", 5);
        data1.put("key2", "abcde fghej");
        String expected1 = "{\"key2\":\"abcde fghej\",\"key1\":5}";
        String result1 = JSONStuff.MapToJSON(data1);
        assertEquals(expected1, result1);
    }
}
