package fr.xodevil_fire.SilverHTTP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Date;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MimetypesFileTypeMap;
import javax.annotation.Resource;

import javax.xml.ws.Endpoint;
import javax.xml.ws.Provider;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Core extends JavaPlugin {
	
	public void onDisable() {
		saveConfig();
	}
	
	public void onEnable() {
		File index = new File(getDataFolder() + "/www/", "index.html");
		if(!index.exists()){
	        index.getParentFile().mkdirs();
	        copy(getResource("index.html"), index);
	    }
		File blankGif = new File(getDataFolder() + "/www/icons/", "blank.gif");
		if(!blankGif.exists()){
	        blankGif.getParentFile().mkdirs();
	        copy(getResource("blank.gif"), blankGif);
	    }
		File backGif = new File(getDataFolder() + "/www/icons/", "back.gif");
		if(!backGif.exists()){
	        backGif.getParentFile().mkdirs();
	        copy(getResource("back.gif"), backGif);
	    }
		File folderGif = new File(getDataFolder() + "/www/icons/", "folder.gif");
		if(!folderGif.exists()){
	        folderGif.getParentFile().mkdirs();
	        copy(getResource("folder.gif"), folderGif);
	    }
		File unknownGif = new File(getDataFolder() + "/www/icons/", "unknown.gif");
		if(!unknownGif.exists()){
			unknownGif.getParentFile().mkdirs();
	        copy(getResource("unknown.gif"), unknownGif);
	    }
		loadConfig();
		String port = this.getConfig().getString("Port");
		String host = (new InetSocketAddress(Integer.parseInt(port))).getAddress().getHostAddress();
		Endpoint endpoint = Endpoint.create(HTTPBinding.HTTP_BINDING, new HtmlFileProvider(this));
		getLogger().info("Demarrage du serveur a l'adresse http://" + host + ":" + port + "/");
		endpoint.publish("http://" + host + ":" + port + "/");
	}
	
	public void loadConfig() {
		getConfig().addDefault("Port", "8920");
		getConfig().options().copyDefaults(true);
		saveConfig();
	}
	
	private void copy(InputStream in, File file) {
	    try {
	        OutputStream out = new FileOutputStream(file);
	        byte[] buf = new byte[1024];
	        int len;
	        while ((len = in.read(buf)) > 0) {
	            out.write(buf,0,len);
	        }
	        out.close();
	        in.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	@WebServiceProvider
	@ServiceMode(Mode.MESSAGE)
	class HtmlFileProvider implements Provider<DataSource> {
		
		private Core instance;
		public HtmlFileProvider(Core instance) {
			this.instance = instance;
		}
		
		@Resource
		protected WebServiceContext wsContext;
		
		public String parseSHTML(String content, String node, Object result) {
			if (content.contains(node)) {
				return content.replace(content.substring(content.indexOf(node), content.indexOf("</shtml>") + 8).toString(), node + result + "</shtml>");
			}
			return "";
		}
		
		public String getPlayers() {
			StringBuffer players = new StringBuffer();
			for (Player player : Bukkit.getServer().getOnlinePlayers()) {
				players.append(player.getName() + ", ");
			}
			if (players.length() > 2) {
				return players.substring(0, players.length() - 2).toString();
			}
			return "";
		}
		
		public String getPlugins() {
			StringBuffer plugins = new StringBuffer();
			for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
				plugins.append(plugin.getName() + ", ");
			}
			if (plugins.length() > 2) {
				return plugins.substring(0, plugins.length() - 2).toString();
			}
			return "";
		}
		
		public boolean isExtension(String mime, String extension){
			Integer ext = (Integer) mime.lastIndexOf(".");
			if (mime.substring(ext + 1, mime.length()).equals(extension)) {
				return true;
			} else {
				return false;
			}
		}
		
		@Override
		public DataSource invoke(DataSource request) {
			String currentDir = "/";
			String iconsDir = "/icons/";
			MessageContext msgCtx = wsContext.getMessageContext();
			//String method = (String)msgCtx .get(MessageContext.HTTP_REQUEST_METHOD);
			String pathInfo = (String)msgCtx.get(MessageContext.PATH_INFO);
			File file = new File(instance.getDataFolder() + "/www/" + pathInfo);
			currentDir += pathInfo;
			if (file.isFile()) {
				if (this.isExtension(file.getName(), "php")) {
					File tempDir = null;
					FileWriter fw = null;
					try {
						tempDir = File.createTempFile("file", ".html");
						fw = new FileWriter(tempDir.getAbsoluteFile(), false);
						BufferedWriter bw = new BufferedWriter(fw);
						Runtime r = Runtime.getRuntime(); //creating an object Runtime by calling the getRuntime Method 
						String cgiContent = "";
						//Process p = r.exec("C:\\php\\php.exe " + file); //win32 process initialised to null
						Process p = r.exec("php " + file.getAbsolutePath());
						/*we redirect the program STDOUT  to a bufferedReader */
						
						BufferedReader brcgi=new BufferedReader(new InputStreamReader(p.getInputStream()));
						
						while((cgiContent=brcgi.readLine())!=null){
							
							if(cgiContent.startsWith("Status")||
							   cgiContent.startsWith("Content")||
							   cgiContent.startsWith("X-Powered-By"))
							{
							
							bw.write("");
							bw.flush();
							
							}else
							{
							//we send the data redirected from the program STDOUT to the client	
							bw.write((cgiContent+"\r\n"));
							bw.flush();
							}
						}
						bw.close();
						fw.close();
						p.destroy();//we destroy the process to free memory
						FileDataSource fds = new FileDataSource(tempDir);
						MimetypesFileTypeMap ftm = new MimetypesFileTypeMap();
						ftm.addMimeTypes("text/html html htm");
						fds.setFileTypeMap(ftm);
						return fds;
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (file.getName().endsWith("shtml")) {
					StringBuffer strFile = new StringBuffer();
					try {
						FileInputStream fis = new FileInputStream(file);
						DataInputStream dis = new DataInputStream(fis);
						BufferedReader br = new BufferedReader(new InputStreamReader(dis));
						//String strLine;
						char[] txt = new char[(int)file.length()];
						String content = "";
						if (br.read(txt) >= 0) {
							content = String.valueOf(txt);
					    }
						this.parseSHTML(content, "<shtml action=getServerName()>", Bukkit.getServer().getName());
						this.parseSHTML(content, "<shtml action=getMaxPlayers()>", Bukkit.getServer().getMaxPlayers());
						this.parseSHTML(content, "<shtml action=getServerIp()>", Bukkit.getServer().getIp());
						this.parseSHTML(content, "<shtml action=getServerPort()>", Bukkit.getServer().getPort());
						//strFile.append(this.parseSHTMLArgs(content, content.substring(content.indexOf("<shtml action=setDefaultGameMode("), content.indexOf(")>" + 2)), content.substring(content.lastIndexOf("<shtml action=setDefaultGameMode("), content.lastIndexOf(")>"));
						this.parseSHTML(content, "<shtml action=getDefaultGameMode()>", Bukkit.getServer().getDefaultGameMode());
						this.parseSHTML(content, "<shtml action=getPlugins()>", this.getPlugins());
						this.parseSHTML(content, "<shtml action=getPlayers()>", this.getPlayers());
						this.parseSHTML(content, "<shtml action=getVersion()>", Bukkit.getServer().getVersion());
						strFile.append(content);
						/*while ((strLine = br.readLine()) != null) {
							strFile.append("\n" + strLine.replace("<shtml action=getPlayers()>", this.getPlayers()));
						}*/
						br.close();
						dis.close();
						fis.close();
						FileWriter fw = new FileWriter(file);
		                BufferedWriter bw = new BufferedWriter(fw);
		                bw.write(strFile.toString());
		                bw.close();
		                fw.close();
						FileDataSource fds = new FileDataSource(instance.getDataFolder() + "/www/" + pathInfo);
						MimetypesFileTypeMap ftm = new MimetypesFileTypeMap();
						ftm.addMimeTypes("text/html shtml");
						fds.setFileTypeMap(ftm);
						return fds;
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				FileDataSource fds = new FileDataSource(instance.getDataFolder() + "/www/" + pathInfo);
				MimetypesFileTypeMap ftm = new MimetypesFileTypeMap();
				ftm.addMimeTypes("text/html html htm");
				ftm.addMimeTypes("text/css css");
				ftm.addMimeTypes("text/csv csv");
				ftm.addMimeTypes("text/plain txt");
				ftm.addMimeTypes("text/javascript js");
				ftm.addMimeTypes("text/xml xml");
				ftm.addMimeTypes("application/xhtml+xml xhtml");
				fds.setFileTypeMap(ftm);
				return fds;
			} else if (currentDir.equals("/null")) {
				FileDataSource fds = new FileDataSource(instance.getDataFolder() + "/www/index.html");
				MimetypesFileTypeMap ftm = new MimetypesFileTypeMap();
				ftm.addMimeTypes("text/html html htm");
				fds.setFileTypeMap(ftm);
				return fds;
			} else if (file.isDirectory()) {
				File tempDir = null;
				FileWriter fw = null;
				Integer nbFound = 0;
				File fileFound = null;
				try {
					tempDir = File.createTempFile("dir", ".html");
					fw = new FileWriter(tempDir.getAbsoluteFile(), false);
					BufferedWriter bw = new BufferedWriter(fw);
					bw.write("<html><head><title>Index of " + currentDir + "</title></head>");
					bw.newLine();
					bw.write("<body><h1>Index of " + currentDir + "</h1><pre><img src=\"" + iconsDir + "blank.gif\" alt=\"     \"> <table style=\"width:50%;\"><tr><th><a href=\"?N=D\">Name</a></th><th><a href=\"?M=A\">Last modified</a></th><th><a href=\"?S=A\">Size</a></th><th><a href=\"?D=A\">Description</a></th></tr><hr>");
					if (!file.getParentFile().getName().equals("www")) {
						bw.newLine();
						bw.write("<tr><td><img src=\"" + iconsDir + "back.gif\" alt=\"[DIR]\"> <a href=\"../" + file.getParentFile().getName() + "\">Parent Directory</a></td><td>-</td><td>-</td><td style=\"text-align:center;\">Folder</td></tr>");
					} else {
						bw.newLine();
						bw.write("<tr><td><img src=\"" + iconsDir + "back.gif\" alt=\"[DIR]\"> <a href=\"../\">Parent Directory</a></td><td>-</td><td>-</td><td style=\"text-align:center;\">Folder</td></tr>");
					}
					bw.newLine();
					for (File f : file.listFiles()) {
						String path;
						if (!currentDir.endsWith("/")) {
							path = f.getParentFile().getName() + "/" + f.getName();
						} else { path = f.getName(); }
						if (f.isDirectory()) {
							bw.write("<tr><td><img src=\"" + iconsDir + "folder.gif\" alt=\"[DIR]\"> <a href=\"" + path + "\">" + f.getName() + "/</a></td><td style=\"text-align:center;\">" + (new Date(file.lastModified())).toString() + "</td><td style=\"text-align:center;\">-</td><td style=\"text-align:center;\">Folder</td></tr>");
							bw.newLine(); 
						} else if (f.isFile()) {
							bw.write("<tr><td><img src=\"" + iconsDir + "unknown.gif\" alt=\"[UKW]\"> <a href=\"" + path + "\">" + f.getName() + "</a></td><td style=\"text-align:center;\">" + (new Date(file.lastModified())).toString() + "</td><td style=\"text-align:center;\">" + (f.length() / 1024) + "k</td><td style=\"text-align:center;\">File</td></tr>");
							bw.newLine();
							if (f.getName().equalsIgnoreCase("index.html")) {
								nbFound++;
								fileFound = f;
							}
						}
					}
					bw.write("</table></pre><hr><address>SilverHTTP Server at port " + instance.getConfig().getString("Port") + "</address></body></html>");
					bw.close();
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				FileDataSource fds = null;
				if (nbFound == 0) {
					fds = new FileDataSource(tempDir);
				} else if (nbFound == 1) {
					fds = new FileDataSource(fileFound);
				}
				MimetypesFileTypeMap ftm = new MimetypesFileTypeMap();
				ftm.addMimeTypes("text/html html htm");
				fds.setFileTypeMap(ftm);
				return fds;
			} else {
				File temp404 = null;
				FileWriter fw = null;
				try {
					temp404 = File.createTempFile("404", ".html");
					fw = new FileWriter(temp404.getAbsoluteFile(), false);
					BufferedWriter bw = new BufferedWriter(fw);
					bw.write("<html><head><title>404 Not Found</title></head>");
					bw.newLine();
					bw.write("<body><h1>Not Found</h1><p>The requested URL " + currentDir + " was not found on this server.</p><hr><address>SilverHTTP Server at port " + instance.getConfig().getString("Port") + "</address></body></html>");
					bw.close();
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				FileDataSource fds = new FileDataSource(temp404);
				MimetypesFileTypeMap ftm = new MimetypesFileTypeMap();
				ftm.addMimeTypes("text/html html htm");
				fds.setFileTypeMap(ftm);
				return fds;
			}
		}

	}

}
