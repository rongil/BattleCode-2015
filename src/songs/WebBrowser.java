package songs;

import java.net.URI;
import java.awt.Desktop;
import java.lang.Thread;
import java.io.IOException;
import java.net.URISyntaxException;

public class WebBrowser {	
	public void OpenPage(String url){
		if(Desktop.isDesktopSupported()){
			Desktop desktop = Desktop.getDesktop();
			try{
				desktop.browse(new URI(url));
			}catch(IOException | URISyntaxException e){
				e.printStackTrace();
			}
		}else{
			Runtime runtime = Runtime.getRuntime();
			try{
				runtime.exec("xdg-open " + url);
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	
	public void LoopPage(String url, long millisduration, int nanosduration, boolean update, String message){
		while(true){
			if(update){
				System.out.println(message);
			}
			
			OpenPage(url);
			
			try{
				Thread.sleep(millisduration, nanosduration);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}
}