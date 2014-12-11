package songs;

class TrollSong{
	private static String url = "https://www.youtube.com/watch?v=o1eHKf-dMwo";
	
	public static void main(String[] args){
		WebBrowser webbrowser = new WebBrowser();
		webbrowser.LoopPage(url, 15000, 0, false, "");
	}
}
