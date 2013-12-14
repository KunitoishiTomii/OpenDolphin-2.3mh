package open.dolphin.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Kazushi Minagawa. Digital Globe, inc.
 * @author modified by masuda, Masuda Naika
 */
public class PluginLister<S> {
    
    private static final String PREFIX = "META-INF/plugins/";
    
    // ロードするプラグインのインターフェイス
    private final Class<S> plugin;
    
    /** Creates a new instance of PluginLister */
    private PluginLister(Class<S> plugin) {
        this.plugin = plugin;
    }
    
    public static <S> PluginLister<S> list(Class<S> plugin) {
        return new PluginLister<>(plugin);
    }

    public Map<String,String> getProviders() {
        
        String fullName = PREFIX + plugin.getName();
        Map<String,String> providers = new LinkedHashMap<>();
        
        ClassLoader loader = getClass().getClassLoader();
        URL url = loader.getResource(fullName);

        try (InputStream in = url.openStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {

            String line;
            while ((line = r.readLine()) != null) {
                String[] tokens = line.trim().split(",");
                //String menu = tokens[0].trim();
                String cmd = tokens[1].trim();
                String value = tokens[2].trim();
                providers.put(cmd, value);
            }
        } catch (IOException ex) {
             fail(plugin, "Error reading plugin configuration files", ex);
        }
        
        return providers;
    }
    
    private void fail(Class plugin, String msg, Throwable cause) throws PluginConfigurationError {
	throw new PluginConfigurationError(plugin.getName() + ": " + msg, cause);
    }
}
