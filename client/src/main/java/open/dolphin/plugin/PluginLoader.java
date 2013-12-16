package open.dolphin.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 *
 * @author Kazushi Minagawa.
 * @author modified by masuda, Masuda Naika
 */
public final class PluginLoader<S> implements Iterable<S> {
    
    private static final String PREFIX = "META-INF/plugins/";
    
    // ロードするプラグインのインターフェイス
    private final Class<S> plugin;
    
    // 生成順のプラグインクラス
    private final List<Class> classList;
    
    /** Creates a new instance of PluginLoader */
    private PluginLoader(Class<S> plugin) {
        this.plugin = plugin;
        classList = new LinkedList<>();
        reload();
    }
    
    public static <S> PluginLoader<S> load(Class<S> plugin) {
        return new PluginLoader<>(plugin);
    }
    
    private void reload() {
        
        classList.clear();
        String fullName = PREFIX + plugin.getName();

        ClassLoader loader = getClass().getClassLoader();
        URL url = loader.getResource(fullName);

        try (InputStream in = url.openStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                int pos = line.indexOf("#");
                if (pos != -1) {
                    line = line.substring(0, pos);
                }
                String clsName = line.trim();
                
                try {
                    classList.add(Class.forName(clsName));
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace(System.err);
                }
            }

        } catch (IOException ex) {
            fail(plugin, "Error reading plugin configuration files", ex);
        }
    }
    
    @Override
    public Iterator<S> iterator() {
        
	return new Iterator<S>() {

            Iterator<Class> itr = classList.iterator();

            @Override
	    public boolean hasNext() {
                return itr.hasNext();
	    }

            @Override
            public S next() {
                try {
                    return (S) itr.next().newInstance();
                } catch (InstantiationException | IllegalAccessException ex) {
                    fail(plugin, "Plugin could not be instantiated", ex);
                }
                return null;
            }

            @Override
	    public void remove() {
		throw new UnsupportedOperationException();
	    }

	};
    }
    
    private void fail(Class plugin, String msg, Throwable cause) throws PluginConfigurationError {
	throw new PluginConfigurationError(plugin.getName() + ": " + msg, cause);
    }
}