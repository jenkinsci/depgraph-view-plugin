package hudson.plugins.depgraph_view.model.display;
import java.util.HashMap;
public class StatusColorConverter {
  static final HashMap<String, String> dict = new HashMap<String,String>(){{
    put("red","red");
    put("notbuilt","gray");
  }};
  public static String getColorFromStatus(String status){
    return dict.get(status).toString();
  }
}
