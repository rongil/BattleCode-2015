package songs;

class ThemeSong {
	private static String url = "https://www.youtube.com/watch?v=M94ii6MVilw";
	
	public static void main(String[] args){
		WebBrowser webbrowser = new WebBrowser();
		webbrowser.OpenPage(url);
	}
}
