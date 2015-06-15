package com.torrenttunes.client.tools;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.javalite.activejdbc.DB;
import org.javalite.activejdbc.DBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Request;
import spark.Response;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Tools {

	static final Logger log = LoggerFactory.getLogger(Tools.class);

	public static final Gson GSON = new Gson();
	public static final Gson GSON2 = new GsonBuilder().setPrettyPrinting().create();

	public static final ObjectMapper MAPPER = new ObjectMapper();

	public static final NumberFormat NUMBER_FORMAT = new DecimalFormat("#0.00"); 

	public static final String USER_AGENT = "torrenttunes-client/1.0.0 (https://github.com/tchoulihan/torrenttunes-client)";


	public static void allowOnlyLocalHeaders(Request req, Response res) {


		log.debug("req ip = " + req.ip());


		//		res.header("Access-Control-Allow-Origin", "http://mozilla.com");
		//		res.header("Access-Control-Allow-Origin", "null");
		//		res.header("Access-Control-Allow-Origin", "*");
		//		res.header("Access-Control-Allow-Credentials", "true");


		if (!isLocalIP(req.ip())) {
			throw new NoSuchElementException("Not a local ip, can't access");
		}
	}

	public static Boolean isLocalIP(String ip) {
		Boolean isLocalIP = (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1"));
		return isLocalIP;
	}

	public static void allowAllHeaders(Request req, Response res) {
		String origin = req.headers("Origin");
		res.header("Access-Control-Allow-Credentials", "true");
		res.header("Access-Control-Allow-Origin", origin);


	}



	public static void logRequestInfo(Request req) {
		String origin = req.headers("Origin");
		String origin2 = req.headers("origin");
		String host = req.headers("Host");


		log.debug("request host: " + host);
		log.debug("request origin: " + origin);
		log.debug("request origin2: " + origin2);


		//		System.out.println("origin = " + origin);
		//		if (DataSources.ALLOW_ACCESS_ADDRESSES.contains(req.headers("Origin"))) {
		//			res.header("Access-Control-Allow-Origin", origin);
		//		}
		for (String header : req.headers()) {
			log.debug("request header | " + header + " : " + req.headers(header));
		}
		log.debug("request ip = " + req.ip());
		log.debug("request pathInfo = " + req.pathInfo());
		log.debug("request host = " + req.host());
		log.debug("request url = " + req.url());
	}

	public static final Map<String, String> createMapFromAjaxPost(String reqBody) {
		log.debug(reqBody);
		Map<String, String> postMap = new HashMap<String, String>();
		String[] split = reqBody.split("&");
		for (int i = 0; i < split.length; i++) {
			String[] keyValue = split[i].split("=");
			try {
				if (keyValue.length > 1) {
					postMap.put(URLDecoder.decode(keyValue[0], "UTF-8"),URLDecoder.decode(keyValue[1], "UTF-8"));
				}
			} catch (UnsupportedEncodingException |ArrayIndexOutOfBoundsException e) {
				e.printStackTrace();
				throw new NoSuchElementException(e.getMessage());
			}
		}

		log.debug(GSON2.toJson(postMap));

		return postMap;

	}


	public static FilenameFilter MUSIC_FILE_FILTER = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.endsWith(".mp3");
		}
	};


	public static String sha2FileChecksum(File file) {
		HashCode hc = null;
		try {
			hc = Files.hash(file, Hashing.sha256());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return hc.toString();
	}

	public static void setupDirectories() {
		if (!new File(DataSources.HOME_DIR()).exists()) {
			log.info("Setting up ~/." + DataSources.APP_NAME + " dirs");
			new File(DataSources.HOME_DIR()).mkdirs();
			new File(DataSources.TORRENTS_DIR()).mkdirs();
			new File(DataSources.DEFAULT_MUSIC_STORAGE_PATH()).mkdirs();


		} else {
			log.info("Home directory already exists");
		}
	}

	public static void copyResourcesToHomeDir(Boolean copyAnyway) {


		String zipFile = null;

		if (copyAnyway || !new File(DataSources.SOURCE_CODE_HOME()).exists()) {


			log.info("Copying resources to  ~/." + DataSources.APP_NAME + " dirs");

			try {
				if (new File(DataSources.SHADED_JAR_FILE).exists()) {
					java.nio.file.Files.copy(Paths.get(DataSources.SHADED_JAR_FILE), Paths.get(DataSources.ZIP_FILE()), 
							StandardCopyOption.REPLACE_EXISTING);
					zipFile = DataSources.SHADED_JAR_FILE;

				} else if (new File(DataSources.SHADED_JAR_FILE_2).exists()) {
					java.nio.file.Files.copy(Paths.get(DataSources.SHADED_JAR_FILE_2), Paths.get(DataSources.ZIP_FILE()),
							StandardCopyOption.REPLACE_EXISTING);
					zipFile = DataSources.SHADED_JAR_FILE_2;
				} else {
					log.info("you need to build the project first");
				}
			} catch (IOException e) {

				e.printStackTrace();
			}


			// unzip and rename it to a jar, if it doesn't already exist
			// TODO gotta figure this one out
//			if (!new File(DataSources.JAR_FILE()).exists()) {
				Tools.unzip(new File(zipFile), new File(DataSources.SOURCE_CODE_HOME()));
				new File(DataSources.ZIP_FILE()).renameTo(new File(DataSources.JAR_FILE()));
//			}

			Tools.installShortcuts();
			//		new Tools().copyJarResourcesRecursively("src", configHome);
		} else {
			log.info("The source directory already exists");
		}
	}

	public static void unzip(File zipfile, File directory) {
		try {
			ZipFile zfile = new ZipFile(zipfile);
			Enumeration<? extends ZipEntry> entries = zfile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				File file = new File(directory, entry.getName());
				if (entry.isDirectory()) {
					file.mkdirs();
				} else {
					file.getParentFile().mkdirs();
					InputStream in = zfile.getInputStream(entry);
					try {
						copy(in, file);
					} finally {
						in.close();
					}
				}
			}

			zfile.close();


		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void copy(InputStream in, File file) throws IOException {
		OutputStream out = new FileOutputStream(file);
		try {
			copy(in, out);
		} finally {
			out.close();
		}
	}

	private static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		while (true) {
			int readCount = in.read(buffer);
			if (readCount < 0) {
				break;
			}
			out.write(buffer, 0, readCount);
		}
	}

	private static void copy(File file, OutputStream out) throws IOException {
		InputStream in = new FileInputStream(file);
		try {
			copy(in, out);
		} finally {
			in.close();
		}
	}

	public static void runSQLFile(Connection c,File sqlFile) {

		try {
			Statement stmt = null;
			stmt = c.createStatement();
			String sql;

			sql = Files.toString(sqlFile, Charset.defaultCharset());

			stmt.executeUpdate(sql);
			stmt.close();
		} catch (IOException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static final void dbInit() {
		try {
			new DB("default").open("org.sqlite.JDBC", "jdbc:sqlite:" + DataSources.DB_FILE(), "root", "p@ssw0rd");
		} catch (DBException e) {
			e.printStackTrace();
			dbClose();
			dbInit();
		}

	}

	public static final void dbClose() {
		new DB("default").close();
	}

	public static String constructTrackTorrentFilename(File file, String mbid) {
		return mbid.toLowerCase() + "_" + sha2FileChecksum(file);
	}



	public static void uploadFileToTracker(File torrentFile) {


		try {
			HttpClient httpclient = HttpClientBuilder.create().build(); 

			HttpPost httppost = new HttpPost(DataSources.TORRENT_UPLOAD_URL);

			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.addPart("torrent", new FileBody(torrentFile));
			builder.addPart("test", new StringBody("derp", ContentType.DEFAULT_TEXT));

			HttpEntity entity = builder.build();

			httppost.setEntity(entity);

			HttpResponse response = httpclient.execute(httppost);
			log.info(response.toString());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	public static String uploadTorrentInfoToTracker(String jsonInfo) {


		String postURL = DataSources.TORRENT_INFO_UPLOAD_URL;

		String message = "";
		try {

			CloseableHttpClient httpClient = HttpClients.createDefault();


			HttpPost httpPost = new HttpPost(postURL);
			httpPost.setEntity(new StringEntity(jsonInfo));

			//			httpPost.setEntity(new StringEntity("L"));

			ResponseHandler<String> handler = new BasicResponseHandler();


			CloseableHttpResponse response = httpClient.execute(httpPost);

			message = handler.handleResponse(response);

			httpClient.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new NoSuchElementException("Couldn't save the torrent info");
		} 

		message = "Rqlite write status : " + message;
		log.info(message);
		return message;
	}





	public static String readFile(String path) {
		String s = null;

		byte[] encoded;
		try {
			encoded = java.nio.file.Files.readAllBytes(Paths.get(path));

			s = new String(encoded, Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return s;
	}

	public static void uninstall() {

		try {
			FileUtils.deleteDirectory(new File(DataSources.HOME_DIR()));


			Tools.uninstallShortcuts();

			log.info("Torrenttunes-client uninstalled successfully.");
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static JsonNode jsonToNode(String json) {

		try {
			JsonNode root = MAPPER.readTree(json);
			return root;
		} catch (Exception e) {
			log.error("json: " + json);
			e.printStackTrace();
		}
		return null;
	}

	public static String nodeToJson(ObjectNode a) {
		try {
			return Tools.MAPPER.writeValueAsString(a);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static String nodeToJsonPretty(JsonNode a) {
		try {
			return Tools.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(a);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	public static final String httpGetString(String url) {
		String res = "";
		try {
			URL externalURL = new URL(url);

			URLConnection yc = externalURL.openConnection();
			yc.setRequestProperty("User-Agent", USER_AGENT);

			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							yc.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) 
				res+="\n" + inputLine;
			in.close();

			return res;
		} catch(IOException e) {}
		return res;
	}

	public static final byte[] httpGetBytes(String urlString) throws IOException {
		URL url = new URL(urlString);

		URLConnection uc = url.openConnection();
		int len = uc.getContentLength();
		InputStream is = new BufferedInputStream(uc.getInputStream());
		try {
			byte[] data = new byte[len];
			int offset = 0;
			while (offset < len) {
				int read = is.read(data, offset, data.length - offset);
				if (read < 0) {
					break;
				}
				offset += read;
			}
			if (offset < len) {
				throw new IOException(
						String.format("Read %d bytes; expected %d", offset, len));
			}
			return data;
		} finally {
			is.close();
		}
	}


	public static final void httpSaveFile(String urlString, String savePath) throws IOException {
		URL url = new URL(urlString);

		URLConnection uc = url.openConnection();

		InputStream input = uc.getInputStream();
		byte[] buffer = new byte[4096];
		int n = - 1;

		OutputStream output = new FileOutputStream(savePath);
		while ( (n = input.read(buffer)) != -1) {

			output.write(buffer, 0, n);

		}
		output.close();

	}

	public static String httpSimplePost(String seederInfoUpload) {
		String res = "";
		try {
			URL externalURL = new URL(seederInfoUpload);

			URLConnection yc = externalURL.openConnection();

			yc.setRequestProperty("User-Agent", USER_AGENT);

			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							yc.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) 
				res+="\n" + inputLine;
			in.close();

			return res;
		} catch(IOException e) {}
		return res;
	}

	public static void addExternalWebServiceVarToTools() {

		log.info("tools.js = " + DataSources.TOOLS_JS());
		try {
			List<String> lines = java.nio.file.Files.readAllLines(Paths.get(DataSources.TOOLS_JS()));

			String interalServiceLine = "var localSparkService = '" + 
					DataSources.WEB_SERVICE_URL + "';";

			String externalServiceLine = "var externalSparkService ='" + 
					DataSources.TRACKER_URL + "';";

			lines.set(0, interalServiceLine);
			lines.set(1, externalServiceLine);

			java.nio.file.Files.write(Paths.get(DataSources.TOOLS_JS()), lines);
			Files.touch(new File(DataSources.TOOLS_JS()));


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void pollAndOpenStartPage() {
		// TODO poll some of the url's every .5 seconds, and load the page when they come back with a result
		int i = 500;
		int cTime = 0;
		while (cTime < 30000) {
			try {
				try {
					String webServiceStartedURL = DataSources.WEB_SERVICE_STARTED_URL();

					HttpURLConnection connection = null;
					URL url = new URL(webServiceStartedURL);
					connection = (HttpURLConnection) url.openConnection();
					connection.setConnectTimeout(5000);//specify the timeout and catch the IOexception
					connection.connect();
					Thread.sleep(2*i);
					Tools.openWebpage(DataSources.MAIN_PAGE_URL());
					cTime = 30000;
				} catch (IOException e) {
					log.info("Could not connect to local webservice, retrying in 500ms up to 30 seconds");
					cTime += i;

					Thread.sleep(i);

				}
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	public static void openWebpage(String urlString) {
		try {
			URL url = new URL(urlString);
			openWebpage(url.toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void openWebpage(URI uri) {
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.browse(uri);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static Long folderSize(File directory) {
		long length = 0;
		for (File file : directory.listFiles()) {
			if (file.isFile())
				length += file.length();
			else
				length += folderSize(file);
		}
		return length;
	}

	public static void installShortcuts() {

		String osName = System.getProperty("os.name").toLowerCase();

		if (osName.contains("linux")) {
			installLinuxShortcuts();
		} else if (osName.contains("windows")) {
			installWindowsShortcuts();
		} else if (osName.contains("mac")) {
			installMacShortcuts();
		}
	}

	public static void uninstallShortcuts() {

		String osName = System.getProperty("os.name").toLowerCase();

		if (osName.contains("linux")) {
			uninstallLinuxShortcuts();
		} else if (osName.contains("windows")) {
			uninstallWindowsShortcuts();
		} else if (osName.contains("mac")) {
			uninstallMacShortcuts();
		}
	}


	public static void uninstallMacShortcuts() {
		// TODO Auto-generated method stub

	}

	public static void uninstallWindowsShortcuts() {
		new File(DataSources.WINDOWS_SHORTCUT_LINK()).delete();
	}

	public static void uninstallLinuxShortcuts() {
		new File(DataSources.LINUX_DESKTOP_FILE()).delete();

	}

	public static void installMacShortcuts() {
		log.info("Installing mac shortcuts...");

		try {
			String s = "do shell script \"java -jar " + DataSources.JAR_FILE() + "\"";
			java.nio.file.Files.write(Paths.get(DataSources.MAC_INSTALL_APPLESCRIPT()), s.getBytes());

			File appDir = new File(DataSources.MAC_APP_LOCATION());
			if (appDir.exists()) {
				FileUtils.deleteDirectory(appDir);
			}
			// Run the shortcut install script
			String cmd = "osacompile -o " + DataSources.MAC_APP_LOCATION() + " " + 
					DataSources.MAC_INSTALL_APPLESCRIPT();
			
			log.info("osacompile cmd : " + cmd);
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			
			// Have to change the stupid Icons and app name
			//TODO https://gist.github.com/fabiofl/5873100
			
			// Replace the icon:
			java.nio.file.Files.copy(Paths.get(DataSources.ICON_MAC_LOCATION()), 
					Paths.get(DataSources.MAC_ICON_APPLET()),
					StandardCopyOption.REPLACE_EXISTING);
			
			// Touch the app folder: 
			new File(DataSources.MAC_APP_LOCATION()).setLastModified(System.currentTimeMillis());

		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void installWindowsShortcuts() {

		log.info("Installing windows shortcuts...");
		try {

			// create and write the .vbs file
			String s = "Set oWS = WScript.CreateObject(\"WScript.Shell\")\n"+
					"sLinkFile = \"" + DataSources.WINDOWS_SHORTCUT_LINK() + "\"\n"+
					"Set oLink = oWS.CreateShortcut(sLinkFile)\n"+
					"oLink.TargetPath = \"" + System.getProperty("java.home") + "/bin/javaw.exe \"\n" +
					"oLink.Arguments = \"-jar " +  DataSources.JAR_FILE() + "\"\n"+
					"oLink.Description = \"Torrent Tunes\" \n"+
					"\' oLink.HotKey = \"ALT+CTRL+F\"\n"+
					"oLink.IconLocation = \"" + DataSources.ICON_LOCATION() + "\"\n"+
					"\' oLink.WindowStyle = \"1\" \n"+
					"\' oLink.WorkingDirectory = \"C:\\Program Files\\MyApp\"\n"+
					"oLink.Save";

			java.nio.file.Files.write(Paths.get(DataSources.WINDOWS_INSTALL_VBS()), s.getBytes());

			// Run the shortcut install script
			String cmd = "cscript " + DataSources.WINDOWS_INSTALL_VBS();
			Runtime.getRuntime().exec(cmd);



		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	public static void installLinuxShortcuts() {
		log.info("Installing linux shortcuts...");
		try {
			String s = "[Desktop Entry]\n"+
					"Type=Application\n"+
					"Encoding=UTF-8\n"+
					"Name=Torrent Tunes\n"+
					"Comment=A sample application\n"+
					"Exec=java -jar " + DataSources.JAR_FILE() + "\n"+
					"Path=" + DataSources.HOME_DIR() + "\n" + 
					"Icon=" + DataSources.ICON_LOCATION() + "\n"+
					"Terminal=false\n"+
					"Categories=Audio;Music;Player;AudioVideo\n"+
					"GenericName=Music Player";

			log.info(s);

			File desktopFile = new File(DataSources.LINUX_DESKTOP_FILE());
			if (desktopFile.exists()) {
				desktopFile.delete();
			}
			java.nio.file.Files.write(Paths.get(DataSources.LINUX_DESKTOP_FILE()), s.getBytes());

			// Run the shortcut install script
			//			String cmd = "desktop-file-install " + DataSources.LINUX_DESKTOP_FILE();
			//			Runtime.getRuntime().exec(cmd);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void restartApplication() throws URISyntaxException, IOException {
		final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		final File currentJar = new File(Tools.class.getProtectionDomain().getCodeSource().getLocation().toURI());

		/* is it a jar file? */
		if(!currentJar.getName().endsWith(".jar"))
			return;

		/* Build command: java -jar application.jar */
		final ArrayList<String> command = new ArrayList<String>();
		command.add(javaBin);
		command.add("-jar");
		command.add(currentJar.getPath());

		final ProcessBuilder builder = new ProcessBuilder(command);
		builder.start();
		System.exit(0);
	}

}

