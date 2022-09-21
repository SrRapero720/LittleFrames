package team.creative.littleframes.client.vlc;

import team.creative.littleframes.LittleFrames;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;

public class VLCDiscovery {
    
    private static boolean loaded = false;
    private static boolean successful = false;
    private static NativeDiscovery discovery;
    public static MediaPlayerFactory factory;
    
    public static synchronized boolean load() {
        if (loaded)
            return successful;
        discovery = new NativeDiscovery(new LinuxNativeDiscoveryStrategyFixed(), new MacOsNativeDiscoveryStrategyFixed(), new WindowsNativeDiscoveryStrategyFixed());
        
        successful = discovery.discover();
        loaded = true;
        if (successful) {
            LittleFrames.LOGGER.info("Loaded VLC in '{}'", discovery.discoveredPath());
            factory = new MediaPlayerFactory("--quiet");
            Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        } else
            LittleFrames.LOGGER.info("Failed to load VLC");
        return successful;
    }
    
    public static VLCLoader createLoader(RenderCallback renderCallback, BufferFormatCallback bufferCallback) {
        return new VLCLoader(renderCallback, bufferCallback);
    }
    
    public static class ShutdownHook extends Thread {
        
        @Override
        public void run() {
            factory.release();
        }
    }
    
}
